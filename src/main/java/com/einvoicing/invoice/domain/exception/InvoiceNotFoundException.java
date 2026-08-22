package com.einvoicing.invoice.domain.exception;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(String invoiceNumber) {
        super("Invoice number " + invoiceNumber + " not found");
    }
}
