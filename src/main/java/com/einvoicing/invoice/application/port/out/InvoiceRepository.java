package com.einvoicing.invoice.application.port.out;

import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;

import java.util.Optional;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);
    Optional<Invoice> findById(InvoiceId id);
    boolean existsByInvoiceNumber(String invoiceNumber);
}
