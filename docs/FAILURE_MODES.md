# Failure modes & production resilience catalogue

This document lists **how the e-invoicing platform can fail in production**, what we already mitigate, what we deliberately defer, and how we intend to **test** and **observe** each class of failure.

It is written for reviewers, interviewers, and future design work: every mitigation has a cost.

---

## End-to-end flow

```text
Client
  → POST /api/auth/login                         (JWT)
  → POST /api/invoices                           (invoice-service)
  → DB + transactional outbox
  → Kafka: invoice.received
  → validation-invoice → invoice.validated | invoice.rejected
  → approval-service   → invoice.approved | invoice.approval-required
  → payment-service    → payment.succeeded | payment.failed
  → invoice-service    → status PAID | PAYMENT_FAILED
```

**Correct payment reactions (do not invert):**

| Event | Invoice status |
|-------|----------------|
| `payment.succeeded` | `PAID` |
| `payment.failed` | `PAYMENT_FAILED` |

---

## Legend

| Column | Meaning |
|--------|---------|
| **ID** | Stable reference for tests and tickets |
| **Already** | Mitigation present in the current codebase |
| **Target** | Production-grade direction |
| **Test** | Unit / integration focus |

---

## 1. Intake & API (`invoice-service`)

| ID | Scenario | Symptom | Business impact | Already | Target solution | Test |
|----|----------|---------|-----------------|---------|-----------------|------|
| **P1** | PostgreSQL unavailable on `POST /api/invoices` | 5xx / connection errors | No intake | Fail with error | Health checks, fail fast, client retry with backoff; optional maintenance mode | IT: DB down → 5xx, no partial row |
| **P2** | Concurrent create with same `invoiceNumber` | Unique constraint / race | Duplicate invoice risk | Exists-by-number check (app-level) | DB unique constraint + clear 409 conflict | IT: parallel POST same number → one success |
| **P3** | Missing / invalid / expired JWT | 401 / 403 | Unauthorized intake | Spring Security + JWT | Keep; rate-limit login; short-lived tokens | Unit/IT: security filter cases |
| **P4** | Invalid or oversized payload | 400 / resource pressure | Bad data or light DoS | Bean validation (partial) | Strict DTOs + max request size | Unit: validation; IT: oversize body |
| **P5** | Client retries after timeout while server already committed | Duplicate intake attempts | Duplicate business records | Partial (invoice number uniqueness) | HTTP `Idempotency-Key` on POST | IT: same key twice → one invoice |
| **P6** | Process crash after DB commit, before HTTP response | Client retries | Same as P5 | Partial | Idempotency-Key + safe retries | IT with controlled crash is hard; document + key design |

---

## 2. Dual-write: business data vs events (outbox)

| ID | Scenario | Symptom | Business impact | Already | Target solution | Test |
|----|----------|---------|-----------------|---------|-----------------|------|
| **O1** | Invoice saved, Kafka publish never happens (crash / broker down) | Row in `invoices`, nothing on `invoice.received` | Invoice stuck, never validated | **Transactional outbox** on invoice intake | Keep outbox; alert on unpublished age | Unit: enqueue in same TX; IT: Kafka down → row unpublished, then publish after recovery |
| **O2** | Outbox publisher fails repeatedly on one row | One invoice never moves | Silent stuck item | Poll/publish path exists | Poison outbox handling + metric `outbox_stuck_age` | Unit: publisher error leaves row retryable |
| **O3** | At-least-once publish of `invoice.received` | Duplicate messages | Double validation work | At-least-once accepted | Consumer idempotency by `invoiceId` / `eventId` | IT: duplicate message → one business outcome |
| **O4** | Outbox backlog grows under load | Rising lag | Delayed validation/approval/payment | — | Publisher scaling, indexes, lag alerts | Metric assertion / load note in runbook |

---

## 3. Validation & approval consumers

| ID | Scenario | Symptom | Business impact | Already | Target solution | Test |
|----|----------|---------|-----------------|---------|-----------------|------|
| **V1** | Poison / invalid JSON on `invoice.received` | Consumer errors, possible partition lag | Validation stalled | Listener try/catch (varies) | **DLQ** (e.g. `invoice.received.DLT`) after N attempts | Unit: bad payload → error handler; IT: message lands on DLT |
| **V2** | Poison message on `invoice.validated` | Approval stalled | Same | — | **DLQ** `invoice.validated.DLT` | Same pattern as V1 |
| **V3** | Consumer crash mid-handler, offset not committed | Redelivery | Duplicate side effects | — | Idempotent handlers | IT: redelivery safe |
| **V4** | Publish `validated` then crash before offset commit | Duplicate downstream events | Duplicate approval attempts | — | Idempotent approval + optional outbox on outbound events | Unit + IT duplicate |
| **V5** | Wrong auto-approval threshold config | Too many manual / too many auto | Ops cost or risk | Policy in code/config | Externalized config + tests for boundary amounts | Unit: `ApprovalPolicy` boundaries |
| **V6** | Manual approval without strong authorization | Unauthorized approve | Fraud risk | JWT on API (scope depends on config) | Role `APPROVER` + audit log | Security IT |

---

## 4. Payment & provider (money path)

| ID | Scenario | Symptom | Business impact | Already | Target solution | Test |
|----|----------|---------|-----------------|---------|-----------------|------|
| **$1** | Kafka redelivers `invoice.approved` | Second charge attempt | **Double charge** | **DB idempotency** (`payment_attempt`) | Keep durable store; unique key; no in-memory in prod | Unit: `tryBegin` states; IT: two messages → one charge |
| **$2** | Provider throws / returns failure | Failed payment event | Invoice not paid | Resilience4j + `payment.failed` | Keep; alert on failure rate | Unit: fallback/failure path |
| **$3** | Provider slow / timeout | Thread blocked, lag, or fallback | Delay or false failure | TimeLimiter / retry / CB (as configured) | Explicit timeouts + **reconciliation** for ambiguous outcomes | Unit: slow mode; IT optional |
| **$4** | Timeout but provider **actually charged** | Local FAILED, money taken | Accounting mismatch | Not fully solved | PSP idempotency key + reconciliation job | Document + stub job later |
| **$5** | Charge OK, crash before `payment.succeeded` publish | Invoice stays APPROVED | Wrong status | In-process publish | **Outbox for payment results** | IT: crash simulation / outbox row |
| **$6** | Circuit open for long period | All charges fail fast | Payment backlog | CB + fallback | Alert on open state; scheduled retry of FAILED | Unit: CB open → fallback |
| **$7** | DLQ fill on `invoice.approved.DLT` | Messages not processed | Manual ops debt | **DLQ configured** for payment listener | Alert on DLT depth + replay runbook | IT: poison → DLT |
| **$8** | In-memory idempotency used in multi-instance / restart | Lost state | Double charge risk | JPA store exists | **Prod profile: JPA only** | Config review + test profile |

---

## 5. Saga / invoice lifecycle

| ID | Scenario | Symptom | Business impact | Already | Target solution | Test |
|----|----------|---------|-----------------|---------|-----------------|------|
| **S1** | `payment.succeeded` consumed | Status → PAID | Happy path | **PaymentSucceededListener** | Keep; idempotent mark paid | Unit: `MarkPaymentSucceededService` |
| **S2** | `payment.failed` consumed | Status → PAYMENT_FAILED | Visible failure | **PaymentFailedListener** | Keep; idempotent mark failed | Unit: `MarkPaymentFailedService` |
| **S3** | Stale `payment.failed` after already PAID | Status overwritten or error | Serious inconsistency | Domain rules (verify) | **Ignore failed if already PAID**; never downgrade | Unit: domain transition guard |
| **S4** | Stale `payment.succeeded` after PAYMENT_FAILED | Unexpected PAID | Policy choice | Verify domain | Explicit policy (allow repair vs reject) | Unit: transition matrix |
| **S5** | `payment.*` never consumed | Stuck APPROVED | Ops blind spot | — | Alert “APPROVED longer than N minutes” | Metric + doc |
| **S6** | Illegal domain transition | Exception / corrupt state | Inconsistency | Domain methods | Strict aggregate invariants | Unit: full status matrix |

This choreography **is** a saga style (choreography, not orchestrator): each service reacts to events; compensation is limited (status to `PAYMENT_FAILED` rather than full reverse approval chain).

---

## 6. Platform & infrastructure

| ID | Scenario | Symptom | Business impact | Already | Target solution | Test |
|----|----------|---------|-----------------|---------|-----------------|------|
| **I1** | Disk full / DB out of connections | Write failures | Intake down | — | Monitoring + pool limits + runbook | Ops checklist |
| **I2** | Kafka unavailable sustained | Outbox backlog, consumer idle | Pipeline freeze | Outbox protects publish side | Multi-AZ broker (ops); alerts | IT: broker down behaviour |
| **I3** | OOM / CPU exhaustion | Restarts, rebalancing | Instability | — | Container limits + JVM metrics | Load note |
| **I4** | Wrong config / missing secret at boot | App crash loop | Deploy outage | Local properties | Fail fast; no silent defaults for secrets | Boot IT with missing env |
| **I5** | Clock skew | JWT / timeouts flaky | Intermittent auth | — | NTP (ops) | Doc only |

---

## 7. Security

| ID | Scenario | Impact | Already | Target | Test |
|----|----------|--------|---------|--------|------|
| **X1** | Secrets committed to Git | Credential leak | Placeholders in Bicep/docs | Secret scanning; rotation runbook | Repo policy |
| **X2** | Open approve/pay endpoints | Fraud | JWT on intake | Method security / roles | Security tests |
| **X3** | Log injection / sensitive data in logs | Leak / noise | Partial | Redact PAN-like fields; structured logs | Review |

---

## Already implemented (summary)

| Area | Mechanism |
|------|-----------|
| Dual-write intake | Transactional outbox (`outbox_events`) |
| Payment duplicates | `payment_attempt` + `tryBegin` / mark succeeded|failed |
| Provider instability | Resilience4j (retry, circuit breaker, fallback) |
| Poison `invoice.approved` | DLQ / DLT |
| Trace HTTP | `X-Correlation-Id` |
| Payment → invoice state | `payment.succeeded` → PAID, `payment.failed` → PAYMENT_FAILED |
| Observability start | Actuator, outbox metrics, payment metrics (as present) |
| Cloud target | Azure Bicep App Service skeleton (not live deploy by default) |

---

## Priority backlog (resilience beyond tests)

Ordered by production risk vs effort:

1. **DLQ** on validation and approval listeners (same pattern as payment).
2. **Consumer idempotency** for `invoice.received` / `invoice.validated` (and payment result listeners).
3. **Domain guards** for stale payment events (especially no PAID → PAYMENT_FAILED downgrade).
4. **Outbox** for `payment.succeeded` / `payment.failed` publish path.
5. **HTTP Idempotency-Key** on `POST /api/invoices`.
6. **Prod profile** forces JPA idempotency store (no in-memory).
7. **Reconciliation stub** for APPROVED-too-long and ambiguous provider timeouts.
8. Full **monitoring** pack (see below).

---

## Testing strategy

### Unit (fast, no brokers)

- Domain: status transition matrix (S3, S4, S6).
- Application: receive + outbox enqueue; mark paid/failed; approval policy; payment `ProcessApprovedInvoice` idempotency paths; Resilience4j fallback behaviour.
- Messaging adapters: parse failures trigger error path (mock).

### Integration (Testcontainers: Postgres + Kafka)

| Scenario IDs | Assertion sketch |
|--------------|------------------|
| O1 | POST invoice → outbox row → message on topic when Kafka up |
| O1 recovery | Kafka stopped during publish → unpublished row → after restart, published |
| $1 | Two `invoice.approved` → one provider charge |
| $7 / V1 | Poison payload → DLT topic |
| S1 / S2 | `payment.succeeded` / `failed` → invoice status |
| P2 / P5 | Duplicate number / idempotency key behaviour |

### What we do not claim yet

- Full chaos suite in CI on every PR (cost/time).
- Real PSP contract tests.
- Multi-region failover drills.

---

## Monitoring direction (phase after resilience gaps)

| Signal | Example |
|--------|---------|
| **RED** | HTTP 5xx rate; consumer lag; provider timeout rate |
| **Business** | Count invoices `APPROVED` older than N minutes; outbox unpublished age; DLT depth |
| **Resilience** | Circuit breaker state; retry counts; payment success/fail counters |
| **Deps** | DB pool usage; Kafka auth errors |
| **Trace** | Correlation id in all logs; later OpenTelemetry traces |

Expose via Spring Actuator + Micrometer; scrape with Prometheus (or cloud equivalent); alert on stuck outbox, DLT growth, and elevated payment failures.

---

## Runbook one-liners (short)

| Symptom | First checks |
|---------|----------------|
| Invoices not validating | Outbox unpublished rows; Kafka up; validation consumer group lag |
| Double pay suspected | `payment_attempt` status by `invoiceId`; provider references |
| Payments all failing | Circuit state; provider mode/config; DLT messages |
| Invoice stuck APPROVED | payment-service logs; `payment.*` topics; listeners on invoice-service |
| Deploy broken | Env vars present; DB reachable; Flyway; no secret in image |

---

## Document control

| | |
|--|--|
| **Status** | Living catalogue — update when mitigations ship |
| **Related code** | `invoice-service` outbox, `payment-service` idempotency & Resilience4j, DLQ config, payment listeners |
| **Related infra** | `docker-compose.yml`, `infra/azure/`, GitHub Actions CI |

When a backlog item is implemented, move it from **Target** to **Already** and add the test name that locks the behaviour.
