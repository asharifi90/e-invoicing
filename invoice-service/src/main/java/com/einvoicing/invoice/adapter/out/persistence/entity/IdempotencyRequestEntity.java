package com.einvoicing.invoice.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_request")
public class IdempotencyRequestEntity {

    @Id
    @Column(name = "key", nullable = false, length = 255)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRequestEntity() {
    }

    public static IdempotencyRequestEntity create(String key, String requestHash, UUID invoiceId) {
        IdempotencyRequestEntity entity = new IdempotencyRequestEntity();
        entity.key = key;
        entity.requestHash = requestHash;
        entity.invoiceId = invoiceId;
        entity.createdAt = Instant.now();
        return entity;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }
}
