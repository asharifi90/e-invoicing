# Failure modes & design decisions

This document describes how the e-invoicing platform can fail, what we implemented in response, and **what we deliberately did not build yet**. It is written for reviewers and for future design discussions: every mitigation has a cost.

The flow:

```text
invoice intake → validation → approval → payment
```

---

## 1. Dual-write (business data vs event)

| | |
|--|--|
| **Failure** | Invoice is saved to PostgreSQL but `InvoiceReceivedEvent` never reaches Kafka (process crash, broker down, publisher error). Downstream never validates the invoice. |
| **Symptom** | Row in `invoices`, no message on `invoice.received` (or delayed until recovery). |
| **Mitigation** | **Transactional outbox** on `invoice-service`: the event is persisted in the same DB transaction as the invoice; a publisher polls and sends to Kafka, then marks the row published. |
| **Trade-off** | Extra table + polling latency vs strong consistency of "saved ⇒ eventually publishable". At-least-once publish still possible → consumers must tolerate duplicates. |
| **Cost** | Storage, publisher load, operational ownership of the outbox table. |
| **Not done yet** | Shared outbox library across all services; relay metrics/alerts on "stuck unpublished" age. |

---

## 2. At-least-once delivery / duplicate payment

| | |
|--|--|
| **Failure** | Kafka redelivers `invoice.approved`; payment runs twice → double charge against a real PSP. |
| **Symptom** | Two provider calls for the same `invoiceId`. |
| **Mitigation** | **Idempotency store** keyed by `invoiceId`: `tryBegin` gates the charge; `SUCCEEDED` blocks a second charge; `FAILED` may allow retry (policy). |
| **Trade-off** | In-memory map = simple, fast to reason about, **lost on restart**, **not safe multi-instance**. Production needs a durable unique key + conditional status updates. |
| **Cost** | Demo: almost none. Prod: schema, migrations, careful state machine. |
| **Not done yet** | DB-backed payment intent; PSP-side idempotency keys; reconciliation for ambiguous timeouts. |

---

## 3. Provider unavailable or slow

| | |
|--|--|
| **Failure** | Simulated (or real) PSP throws or hangs; naive code either crashes the consumer loop or blocks threads. |
| **Symptom** | Errors in payment logs; lag on `invoice.approved`; possible thread starvation if unbounded waits. |
| **Mitigation** | **Resilience4j** on the charge path: retry, circuit breaker, fallback returning a failed `PaymentProviderResult`. Provider technical failures can become `payment.failed` without poisoning the whole consumer indefinitely. |
| **Trade-off** | Retries add load on a sick dependency; circuit breaker sheds load but increases short-term failure rate visible to the business. Fallback must not be confused with a successful pay. |
| **Cost** | Tuning (`maxAttempts`, windows, wait duration); need for metrics on open circuits. |
| **Not done yet** | Explicit time limiter on all paths; bulkhead isolation; adaptive retry budgets. |

---

## 4. Poison messages

| | |
|--|--|
| **Failure** | Non-JSON or permanently invalid payload on `invoice.approved`. |
| **Symptom** | Listener exceptions; without a DLQ, the partition can stall on retries. |
| **Mitigation** | **DefaultErrorHandler + Dead Letter Publishing** → `invoice.approved.DLT` after limited retries. Main consumer continues. |
| **Trade-off** | Business work in the DLT is **invisible** until someone watches the topic. DLQ without alerting = silent loss. |
| **Cost** | Extra topic; runbooks; possible PII in bad payloads. |
| **Not done yet** | Alert on DLT depth; replay tooling; same pattern on all consumers. |

---

## 5. Async gap between services (no shared transaction)

| | |
|--|--|
| **Failure** | Validation succeeds, approval never runs (or payment never runs): process stuck in an intermediate business state. |
| **Symptom** | Events on early topics, silence downstream; support asks "where is my invoice?" |
| **Mitigation (partial)** | Clear topic chain and logs; correlation id on HTTP entry (invoice API). |
| **Trade-off** | Choreography (events) scales ownership per service but makes **global progress** harder to see than an orchestrated saga with an explicit process state. |
| **Cost** | Full process manager / saga compensations = more moving parts. |
| **Not done yet** | End-to-end process state store; compensations (e.g. react to `payment.failed`); SLO dashboards per stage. |

---

## 6. Observability gaps

| | |
|--|--|
| **Failure** | We cannot answer quickly: "did we publish?", "did we double-charge?", "is the circuit open?" |
| **Mitigation (partial)** | Structured logs; outbox-related metrics (invoice-service); correlation id filter on API; payment counters (success / fail / duplicate skipped). |
| **Trade-off** | More metrics/labels = cardinality and noise; too few = flying blind. |
| **Not done yet** | Alert rules; trace propagation into Kafka headers; dashboards. |

---

## Decision principles (how this project is steered)

When choosing a mitigation, prefer in order:

1. **Correctness under failure** for money-like operations (idempotency before clever routing).
2. **Explicit trade-offs** written down (this file) instead of hidden assumptions.
3. **Cheapest control that reduces the biggest risk** (outbox on intake; idempotency on pay).
4. **Honesty about demo vs production** (in-memory store is a teaching boundary, not a production claim).

Strong engineering practice is not "use every pattern"; it is **owning the cost** of each pattern (latency, ops load, complexity) and knowing what still hurts in production.

---

## Map: component → main risks

| Component | Main risks addressed |
|-----------|----------------------|
| Invoice outbox | Dual-write intake |
| Validation / approval consumers | (partial) poison payloads — extend DLQ pattern |
| Payment Resilience4j | Dependency failure / latency |
| Payment idempotency | Duplicate charge |
| Payment DLQ | Poison `invoice.approved` |

---

## Follow-ups (backlog, ordered by leverage)

1. Durable payment idempotency (DB + status transitions).
2. Alerting on payment metrics and DLT depth.
3. DLQ on all consumers.
4. Reaction to `payment.failed` (business state / compensation story).
5. Optional: process-level view (choreography vs orchestration when complexity grows).
