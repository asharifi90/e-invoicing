# E-Invoicing Platform – Invoice Service

Event-driven service that receives e-invoices, stores them, and publishes domain events to Kafka.

Built with a **hexagonal / DDD-style** layout so the domain stays independent from the web, database, and messaging.

## Features

- Receive invoice via REST (`POST /api/invoices`)
- Get invoice by id (`GET /api/invoices/{id}`)
- Persist invoices in PostgreSQL (Flyway migrations)
- Publish `InvoiceReceivedEvent` to Kafka
- Clear domain model (aggregate, value objects, domain events)
- Unit tests for domain and application layers

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Architecture | Hexagonal (domain / application / adapters) |
| Database | PostgreSQL + Flyway |
| Messaging | Apache Kafka |
| Local infra | Docker Compose |
| Tests | JUnit 5, Mockito, AssertJ |
| CI | GitHub Actions |

## Architecture

```text
adapter/in/web          → REST API
application             → use cases + ports
domain                  → Invoice aggregate, events, rules
adapter/out/persistence → JPA
adapter/out/messaging   → Kafka publisher
```

**Flow**

```text
Client → POST /api/invoices
      → ReceiveInvoice use case
      → save Invoice
      → publish InvoiceReceivedEvent → Kafka topic invoice.received
```

## Run locally

### 1. Start infrastructure

```bash
docker compose up -d
```

| Service | URL / port |
|---------|------------|
| PostgreSQL | `localhost:5432` (or `5433` if you remapped) |
| Kafka | `localhost:9092` |
| Kafka UI | http://localhost:8090 (if enabled) |

### 2. Configure application

`application.properties` (example):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/einvoice
spring.datasource.username=einvoice
spring.datasource.password=einvoice
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
spring.kafka.bootstrap-servers=localhost:9092
```

### 3. Start the app

```bash
mvn spring-boot:run
```

API base: `http://localhost:8080`

## Example API calls

### Create invoice

```http
POST /api/invoices
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
GET /api/invoices/{id}
```

## Tests

```bash
# all tests
mvn test

# domain only
mvn test -Dtest=InvoiceTest

# use cases only
mvn test -Dtest=ReceiveInvoiceServiceTest,GetInvoiceServiceTest
```

**Current test focus**

- Domain: invoice creation, totals, status rules
- Application: receive (save + publish), get (found / not found) with mocked ports

## CI

On every push/PR to `main`, GitHub Actions:

1. Checks out the code
2. Sets up Java 21
3. Runs `mvn clean test`

## Project status

- [x] Invoice receive + persistence + Kafka event
- [x] GET by id
- [x] Domain & application unit tests
- [x] CI pipeline
- [ ] Validation service (consume `invoice.received`)
- [ ] Approval workflow
- [ ] Docker image for the app itself
- [ ] Integration tests with Testcontainers

## Why this project

Demonstrates a realistic backend style used in European/fintech-style systems:

- Clear boundaries (DDD / hexagonal)
- Event-driven integration via Kafka
- Production-like local setup with Docker
- Automated tests and CI
```
