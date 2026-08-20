package com.einvoicing.invoice.domain;

import java.util.UUID;

public record InvoiceId(UUID value) {

    public InvoiceId {
        if (value == null) {
            throw new IllegalArgumentException("invoiceId cannot be null");
        }
    }

    public static InvoiceId generate() {
        return new InvoiceId(UUID.randomUUID());
    }

    public static InvoiceId from(String value){
        return new InvoiceId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
