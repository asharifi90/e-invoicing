package com.einvoicing.validation.domain.event;

import java.time.Instant;
import java.util.UUID;

public class InvoiceValidatedEvent {

    private UUID eventId;
    private UUID invoiceId;
    private String invoiceNumber;
    private Instant validatedAt;

    public static InvoiceValidatedEvent of(UUID invoiceId, String invoiceNumber) {
        InvoiceValidatedEvent event = new InvoiceValidatedEvent();
        event.eventId = invoiceId;
        event.invoiceNumber = invoiceNumber;
        event.validatedAt = Instant.now();
        event.invoiceId = UUID.randomUUID();
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
}
