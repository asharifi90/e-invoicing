package com.einvoicing.approval.application.port.in;

import com.einvoicing.approval.domain.event.InvoiceValidatedEvent;

public interface ApproveInvoiceUseCase {
    void handleValidated(InvoiceValidatedEvent event);
}
