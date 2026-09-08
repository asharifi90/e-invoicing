# E-Invoicing Platform

Event-driven platform that receives e-invoices, persists them, publishes domain events to Kafka, and validates them in a separate service.

Built as a **multi-module Maven** project with a **hexagonal / DDD-style** layout so domain logic stays independent from the web, database, and messaging.

## Modules

| Module | Role |
|--------|------|
| **invoice-service** | REST API, PostgreSQL, publish `InvoiceReceivedEvent` |
| **validation-invoice** | Consume `invoice.received`, apply rules, publish `invoice.validated` / `invoice.rejected` |
| **approval-service** | 8082 | Consume `invoice.validated`, auto-approve or pending + manual `POST` approve |

## Features

- Receive invoice via REST (`POST /api/invoices`) with **JWT** authentication
- Get invoice by id (`GET /api/invoices/{id}`)
- Persist invoices in PostgreSQL (Flyway migrations)
- Publish domain events to **Apache Kafka**
- Asynchronous validation service
- Approval: auto under threshold or manual via API
- Unit tests (domain, application, listeners) + **GitHub Actions** CI

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
Client
  → POST /api/auth/login          → JWT
  → POST /api/invoices (+ Bearer)
  → invoice-service saves invoice
  → Kafka: invoice.received
  → validation-invoice
       → invoice.validated  OR  invoice.rejected
  → approval-service (on validated)
       → if amount < threshold  → invoice.approved (AUTO)
       → else → store pending + invoice.approval-required
            → POST /api/approvals/{invoiceId}/approve
            → invoice.approved (MANUAL)
```

```text
invoice-service
  adapter/in/web          → REST + JWT
  application             → use cases + ports
  domain                  → Invoice aggregate, events
  adapter/out/persistence → JPA
  adapter/out/messaging   → Kafka publisher

validation-invoice
  adapter/in/messaging    → Kafka listener
  application / domain    → rules
  adapter/out/messaging   → Kafka publisher

approval-service
  adapter/in/messaging    → Kafka listener
  adapter/in/web          → manual approve API
  application / domain    → policy + pending store (in-memory demo)
  adapter/out/messaging   → Kafka publisher
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

### 2. Start the three applications

From the **repo root** (parent POM):

```bash
# Terminal 1
mvn spring-boot:run -pl invoice-service

# Terminal 2
mvn spring-boot:run -pl validation-invoice

# Terminal 3
mvn spring-boot:run -pl approval-service
```

| Service | Base URL |
|---------|----------|
| invoice-service | http://localhost:8080 |
| validation-invoice | http://localhost:8081 |
| approval-service | http://localhost:8082 |

## Authentication (JWT)

Demo user (in-memory): **`demo` / `demo123`**

### Login

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "username": "demo",
  "password": "demo123"
}
```

Response includes a Bearer token. Use it on protected invoice endpoints:

```http
Authorization: Bearer <jwt-token>
```

Without a valid token → `401 Unauthorized`.

## Example API calls

### Create invoice

```http
POST http://localhost:8080/api/invoices
Authorization: Bearer <jwt-token>
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
Authorization: Bearer <jwt-token>
```

### Manual approval (when amount ≥ auto-approve threshold, e.g. 1000 EUR)

```http
POST http://localhost:8082/api/approvals/{invoiceId}/approve
```

→ publishes `invoice.approved` with mode `MANUAL`.

Pending approvals are kept **in memory** for the demo (restart clears them). Production would use a database.

## Kafka topics

| Topic | Producer | Meaning |
|-------|----------|---------|
| `invoice.received` | invoice-service | Invoice accepted and stored |
| `invoice.validated` | validation-invoice | Passed business rules |
| `invoice.rejected` | validation-invoice | Failed business rules |
| `invoice.approval-required` | approval-service | Needs human approval |
| `invoice.approved` | approval-service | Approved (AUTO or MANUAL) |

## Tests

From repo root:

```bash
mvn test
```

Examples:

```bash
mvn test -pl invoice-service
mvn test -pl validation-invoice
mvn test -pl approval-service
```

Focus: domain rules and use cases with mocked ports (no Postgres/Kafka required in unit tests).  
**Green CI** = `mvn clean test` passed on GitHub Actions.

## CI

On push/PR to `main`:

1. Checkout  
2. Java 21  
3. `mvn clean test`  

## Project status

- [x] Invoice receive + persistence + Kafka event  
- [x] GET by id  
- [x] JWT authentication (demo user)  
- [x] Validation service  
- [x] Approval service (auto + manual API)  
- [x] Unit tests + multi-module structure  
- [x] GitHub Actions CI  
- [ ] DLQ for invalid Kafka messages  
- [ ] Transactional outbox (e.g. Namastack)  
- [ ] Docker images for the apps  
- [ ] Integration tests with Testcontainers  
- [ ] Pending approvals in database  

## Why this project

Demonstrates a realistic European-style backend:

- Clear boundaries (hexagonal / DDD-style)  
- Event-driven microservices with Kafka  
- Security (JWT) on the public API  
- Automated tests and CI  
- A full business flow: intake → validate → approve  

## License / note

Portfolio / learning project. Demo credentials and in-memory stores are not for production use.
