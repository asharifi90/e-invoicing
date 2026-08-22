package com.einvoicing.invoice.domain.exception;

import com.einvoicing.invoice.domain.model.aggregate.Invoice;

public class InvoiceAlreadyExistsException extends RuntimeException{

    public  InvoiceAlreadyExistsException(String invoiceNumber) {
        super("Invoice number " + invoiceNumber + " already exists");
    }
}
