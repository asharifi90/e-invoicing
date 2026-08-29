package com.einvoicing.validation.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class InvoiceReceivedEvent {

    private  UUID eventId;
    private UUID invoiceId;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private String currency;
    private Instant createdAt;


    public InvoiceReceivedEvent() {
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

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
