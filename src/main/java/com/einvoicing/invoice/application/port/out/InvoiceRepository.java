package com.einvoicing.invoice.application.port.out;

import com.einvoicing.invoice.domain.Invoice;
import com.einvoicing.invoice.domain.InvoiceId;

import java.util.Optional;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);
    Optional<Invoice> findById(InvoiceId id);
    boolean existsByInvoiceNumber(String invoiceNumber);
}
