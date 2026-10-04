package com.einvoicing.validation.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class InvoiceValidatedEvent {

    private UUID eventId;
    private UUID invoiceId;
    private String invoiceNumber;
    private Instant validatedAt;
    private BigDecimal totalAmount;
    private String currency;

    public static InvoiceValidatedEvent of(UUID invoiceId, String invoiceNumber,  BigDecimal totalAmount, String currency) {
        InvoiceValidatedEvent event = new InvoiceValidatedEvent();
        event.eventId = UUID.randomUUID();
        event.invoiceNumber = invoiceNumber;
        event.validatedAt = Instant.now();
        event.invoiceId = invoiceId;
        event.totalAmount = totalAmount;
        event.currency = currency;
        return event;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
