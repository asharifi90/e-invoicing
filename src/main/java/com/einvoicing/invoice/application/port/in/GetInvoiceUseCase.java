package com.einvoicing.invoice.application.port.in;

import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;

public interface GetInvoiceUseCase {
    Invoice getById(InvoiceId id);
}
