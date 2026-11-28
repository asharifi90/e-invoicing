package com.einvoicing.invoice.domain.event;

import java.time.Instant;
import java.util.UUID;

public class InvoiceRejectedEvent {
    private UUID invoiceId;
    private String invoiceNumber;
    private String reason;
    private Instant rejectedAt;

    public UUID getInvoiceId() { return invoiceId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getReason() { return reason; }
    public Instant getRejectedAt() { return rejectedAt; }
}