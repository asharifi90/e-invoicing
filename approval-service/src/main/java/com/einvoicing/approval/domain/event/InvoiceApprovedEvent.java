package com.einvoicing.approval.domain.event;

import com.einvoicing.approval.domain.enums.InvoiceApprovalMode;

import java.time.Instant;
import java.util.UUID;

public class InvoiceApprovedEvent {
    private UUID eventId;
    private UUID invoiceId;
    private String invoiceNumber;
    private Instant approvedAt;
    private InvoiceApprovalMode mode;
    private String approvedBy;

    public static InvoiceApprovedEvent auto(UUID invoiceId, String invoiceNumber) {
        InvoiceApprovedEvent e = new InvoiceApprovedEvent();
        e.eventId = UUID.randomUUID();
        e.invoiceId = invoiceId;
        e.invoiceNumber = invoiceNumber;
        e.approvedAt = Instant.now();
        e.mode = InvoiceApprovalMode.AUTO;
        return e;
    }

    public static InvoiceApprovedEvent manual(UUID invoiceId, String invoiceNumber, String approvedBy) {
        InvoiceApprovedEvent e = new InvoiceApprovedEvent();
        e.eventId = UUID.randomUUID();
        e.invoiceId = invoiceId;
        e.invoiceNumber = invoiceNumber;
        e.approvedAt = Instant.now();
        e.mode = InvoiceApprovalMode.MANUAL;
        e.approvedBy = approvedBy;
        return e;
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

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public InvoiceApprovalMode getMode() {
        return mode;
    }

    public void setMode(InvoiceApprovalMode mode) {
        this.mode = mode;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}
