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
}
