# E-Invoicing Platform

Event-driven platform that receives e-invoices, persists them, publishes domain events to Kafka, and validates them in a separate service.

Built as a **multi-module Maven** project with a **hexagonal / DDD-style** layout so domain logic stays independent from the web, database, and messaging.

## Modules

| Module | Role |
|--------|------|
| **invoice-service** | REST API, PostgreSQL, publish `InvoiceReceivedEvent` |
| **validation-invoice** | Consume `invoice.received`, apply rules, publish `invoice.validated` / `invoice.rejected` |

## Features

- Receive invoice via REST (`POST /api/invoices`)
- Get invoice by id (`GET /api/invoices/{id}`)
- Persist invoices in PostgreSQL (Flyway migrations)
- Publish `InvoiceReceivedEvent` to Kafka
- Validate invoices asynchronously (separate consumer service)
- Publish validation results to Kafka
- Unit tests (domain, application, listener)
- CI with GitHub Actions

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.x / 4.x |
| Architecture | Hexagonal (domain / application / adapters), multi-module |
| Database | PostgreSQL + Flyway |
| Messaging | Apache Kafka |
| Local infra | Docker Compose |
| Tests | JUnit 5, Mockito, AssertJ |
| CI | GitHub Actions |

## Architecture

```text
invoice-service
  adapter/in/web          → REST API
  application             → use cases + ports
  domain                  → Invoice aggregate, events
  adapter/out/persistence → JPA
  adapter/out/messaging   → Kafka publisher

validation-invoice
  adapter/in/messaging    → Kafka listener (invoice.received)
  application             → validation use case
  domain                  → rules + result events
  adapter/out/messaging   → Kafka publisher (validated / rejected)
```

**Flow**

```text
Client
  → POST /api/invoices
  → invoice-service saves Invoice
  → publish InvoiceReceivedEvent
  → topic: invoice.received
  → validation-invoice consumes event
  → rules (amount, currency, invoice number, …)
  → topic: invoice.validated  OR  invoice.rejected
```

## Run locally

### 1. Start infrastructure

```bash
docker compose up -d
```

| Service | URL / port |
|---------|------------|
| PostgreSQL | `localhost:5432` (or `5433` if remapped) |
| Kafka | `localhost:9092` |
| Kafka UI | http://localhost:8090 (if enabled) |

### 2. Configuration (examples)

**invoice-service** — datasource + Kafka producer (see module `application.properties`).

**validation-invoice** — Kafka consumer/producer, e.g. port `8081`:

```properties
server.port=8081
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=validation-invoice
```

### 3. Start both applications

From the **repo root** (parent POM):

```bash
# Terminal 1
mvn spring-boot:run -pl invoice-service

# Terminal 2
mvn spring-boot:run -pl validation-invoice
```

| Service | Base URL |
|---------|----------|
| invoice-service | http://localhost:8080 |
| validation-invoice | http://localhost:8081 (if web enabled) |

## Example API calls

### Create invoice

```http
POST http://localhost:8080/api/invoices
Content-Type: application/json
```

```json
{
  "invoiceNumber": "INV-2026-001",
  "sellerVatNumber": "BE0123456789",
  "buyerVatNumber": "BE9876543210",
  "lines": [
    {
      "description": "Consulting",
      "quantity": 10,
      "unitPrice": 150.00,
      "currency": "EUR"
    }
  ]
}
```

### Get invoice

```http
GET http://localhost:8080/api/invoices/{id}
```

After a successful create, check Kafka topics (console consumer or UI):

- `invoice.received` — event from invoice-service
- `invoice.validated` or `invoice.rejected` — result from validation-invoice

## Tests

From repo root (all modules):

```bash
mvn test
```

Examples:

```bash
mvn test -pl invoice-service -Dtest=InvoiceTest
mvn test -pl invoice-service -Dtest=ReceiveInvoiceServiceTest,GetInvoiceServiceTest
mvn test -pl validation-invoice
```

**Test focus**

- **invoice-service:** domain rules, receive/get use cases (mocked ports)
- **validation-invoice:** validation rules, publish validated/rejected, JSON parse in listener

Note: full `@SpringBootTest` context tests need a running database; the suite relies mainly on **unit tests** so CI stays green without Postgres.

## CI

On every push/PR to `main`, GitHub Actions:

1. Checks out the code
2. Sets up Java 21
3. Runs `mvn clean test` on the multi-module project

**Green CI** = build and automated tests passed on GitHub’s runners.

## Project status

- [x] Invoice receive + persistence + Kafka event
- [x] GET by id
- [x] Validation service (consume `invoice.received`, publish result)
- [x] Domain & application unit tests (both modules)
- [x] Multi-module Maven structure
- [x] CI pipeline
- [ ] JWT / API authentication
- [ ] Approval workflow
- [ ] DLQ for invalid Kafka messages
- [ ] Docker image of the apps
- [ ] Integration tests with Testcontainers

## Why this project

Demonstrates a realistic backend style used in European systems:

- Clear boundaries (DDD / hexagonal)
- Event-driven integration via Kafka
- Multi-module microservices in one repository
- Production-like local setup with Docker
- Automated tests and CI
