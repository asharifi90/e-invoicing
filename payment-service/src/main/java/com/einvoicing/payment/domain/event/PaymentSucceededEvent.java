package com.einvoicing.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentSucceededEvent {

    private UUID invoiceId;
    private UUID eventId;
    private String invoiceNumber;
    private BigDecimal amount;
    private String currency;
    private Instant paidAt;
    private String providerReference;

    public static PaymentSucceededEvent of(UUID invoiceId, String invoiceNumber, BigDecimal amount, String currency,
                                           String providerReference) {
        PaymentSucceededEvent event = new PaymentSucceededEvent();
        event.invoiceId = invoiceId;
        event.eventId = UUID.randomUUID();
        event.invoiceNumber = invoiceNumber;
        event.amount = amount;
        event.currency = currency;
        event.providerReference = providerReference;
        event.paidAt = Instant.now();
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

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
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

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }
}
