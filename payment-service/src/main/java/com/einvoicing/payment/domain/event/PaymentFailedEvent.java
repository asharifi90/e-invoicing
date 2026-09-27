package com.einvoicing.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentFailedEvent {

    private UUID invoiceId;
    private UUID eventId;
    private String reason;
    private BigDecimal amount;
    private String currency;
    private Instant failedAt;
    private String invoiceNumber;

    public static PaymentFailedEvent of(UUID invoiceId, String invoiceNumber,
                                        String reason, BigDecimal amount, String currency) {
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.invoiceId = invoiceId;
        event.invoiceNumber = invoiceNumber;
        event.reason = reason;
        event.amount = amount;
        event.currency = currency;
        event.failedAt = Instant.now();
        event.eventId = UUID.randomUUID();
        return event;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
}
