package com.einvoicing.invoice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class InvoiceReceivedEvent {

    private final UUID eventId;
    private final UUID invoiceId;
    private final String invoiceNumber;
    private final BigDecimal totalAmount;
    private final String currency;
    private final Instant createdAt;

    public InvoiceReceivedEvent(UUID invoiceId, String invoiceNumber, BigDecimal totalAmount, String currency) {
        this.eventId = UUID.randomUUID();
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
