package com.einvoicing.payment.application.port.in;

import com.einvoicing.payment.domain.event.InvoiceApprovedEvent;

public interface ProcessApprovedInvoiceUseCase {

    void handle(InvoiceApprovedEvent event);
}
