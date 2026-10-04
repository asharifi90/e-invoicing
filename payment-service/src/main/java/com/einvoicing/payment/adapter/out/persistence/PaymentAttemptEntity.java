package com.einvoicing.payment.adapter.out.persistence;

import com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempt")
public class PaymentAttemptEntity {

    @Id
    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @Column(name = "invoice_number", length = 64)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentAttemptStatus status;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentAttemptEntity() {}

    public static PaymentAttemptEntity start(UUID invoiceId, String invoiceNumber) {
        PaymentAttemptEntity e = new PaymentAttemptEntity();
        e.invoiceId = invoiceId;
        e.invoiceNumber = invoiceNumber;
        e.status = PaymentAttemptStatus.PROCESSING;
        e.createdAt = Instant.now();
        e.updatedAt = e.createdAt;
        return e;
    }

    public void markSucceeded(String providerReference) {
        this.status = PaymentAttemptStatus.SUCCEEDED;
        this.providerReference = providerReference;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentAttemptStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void reopenForRetry() {
        this.status = PaymentAttemptStatus.PROCESSING;
        this.providerReference = null;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public UUID getInvoiceId() { return invoiceId; }
    public PaymentAttemptStatus getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
}
