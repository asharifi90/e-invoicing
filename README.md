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

## Flow

Client → POST /api/invoices
      → ReceiveInvoice use case
      → save Invoice
      → publish InvoiceReceivedEvent → Kafka topic invoice.received

## Run localy

1. Start infrastructure:
   ```bash
   docker compose up -d
   
| Service | URL |
|---------|-----|
| portPostgreSQL | localhost:5432 |
| Kafka | localhost:9092 |
| Kafka UI | http://localhost:8090 (if enabled) |

2. Configure application
```text
   spring.datasource.url=jdbc:postgresql://localhost:5432/einvoice
   spring.datasource.username=einvoice
   spring.datasource.password=einvoice
   spring.flyway.enabled=true
   spring.jpa.hibernate.ddl-auto=validate
   spring.kafka.bootstrap-servers=localhost:9092

3. Start the app
   ```bash
   mvn spring-boot:run
```text
   API base: http://localhost:8080
