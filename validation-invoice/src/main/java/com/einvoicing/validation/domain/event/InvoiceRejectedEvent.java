package com.einvoicing.validation.domain.event;

import java.time.Instant;
import java.util.UUID;

public class InvoiceRejectedEvent {

    private String invoiceNumber;
    private UUID invoiceId;
    private UUID eventId;
    private String reason;
    private Instant rejectedAt;

    public static InvoiceRejectedEvent from(String invoiceNumber, UUID invoiceId, String reason) {
        InvoiceRejectedEvent event = new InvoiceRejectedEvent();
        event.invoiceNumber = invoiceNumber;
        event.invoiceId = invoiceId;
        event.rejectedAt = Instant.now();
        event.reason = reason;
        event.eventId = UUID.randomUUID();
        return event;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
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

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }
}
