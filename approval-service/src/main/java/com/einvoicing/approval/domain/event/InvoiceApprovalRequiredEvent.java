package com.einvoicing.approval.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class InvoiceApprovalRequiredEvent {
    private UUID eventId;
    private UUID invoiceId;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private Instant requestedAt;

    public static InvoiceApprovalRequiredEvent of(UUID invoiceId, String number, BigDecimal amount) {
    InvoiceApprovalRequiredEvent event = new InvoiceApprovalRequiredEvent();
    event.invoiceId = invoiceId;
    event.invoiceNumber = number;
    event.totalAmount = amount;
    event.requestedAt = Instant.now();
    event.eventId = UUID.randomUUID();
    return event;
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

    public Instant getRequestedAt() {
        return requestedAt;
    }
}