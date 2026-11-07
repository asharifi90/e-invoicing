# Observability lab

Local stack: Docker Compose (Postgres, Kafka, Prometheus, Grafana).
Apps run on the host; Prometheus scrapes `/actuator/prometheus`.

## URLs

| Component   | URL |
|------------|-----|
| Prometheus | http://localhost:9090 |
| Grafana    | http://localhost:3000 (admin / admin) |
| invoice-service metrics | http://localhost:8080/actuator/prometheus |
| payment-service metrics | http://localhost:8083/actuator/prometheus |

> Actuator `health` + `prometheus` are `permitAll` so Prometheus can scrape without JWT.

## Metrics (business)

| Metric | Service | Meaning |
|--------|---------|---------|
| `payment_charge_success_total` | payment-service | Successful charges |
| `payment_charge_failed_total` | payment-service | Failed charges |
| `payment_charge_duplicate_skipped_total` | payment-service | Idempotent skip |
| `outbox_published_failed_total` | invoice-service | Outbox publish failures |

## Scenario A — Payment provider FAIL

1. Set payment mode to `FAIL` (property / env used by simulated provider).
2. Run flow until payment (auto-approved invoice).
3. **Expect:** `payment_charge_failed_total` increases in Prometheus / Grafana.
4. **Expect:** invoice status `PAYMENT_FAILED` (via `payment.failed` event).
5. **Mitigation in code:** Resilience4j fallback + DB idempotency + status update.

## Scenario B — Outbox publish failure

1. Stop Kafka: `docker compose stop kafka` (or break bootstrap temporarily).
2. `POST /api/invoices` with JWT (invoice is saved + outbox row).
3. Wait for outbox publisher attempts.
4. **Expect:** `outbox_published_failed_total` increases.
5. Start Kafka again: `docker compose start kafka`.
6. **Expect:** pending outbox rows publish; flow can continue downstream.

## Scenario C — Payment SUCCESS (control)

1. Mode `SUCCESS`.
2. Full flow → **Expect:** `payment_charge_success_total` +1, invoice `PAID`.

## Notes

- Grafana dashboards persist if volume `grafana_data` is set; `docker compose down -v` wipes them.
- invoice-service and payment-service use **separate databases** (`e_invoice`, `payment`) so Flyway histories do not clash.