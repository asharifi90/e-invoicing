package com.einvoicing.validation.application.port.in;

import com.einvoicing.validation.domain.event.InvoiceReceivedEvent;

public interface ValidateInvoiceUseCase {

    void validateReceivedEvent(InvoiceReceivedEvent event);
}
