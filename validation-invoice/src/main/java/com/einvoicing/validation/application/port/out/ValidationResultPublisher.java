package com.einvoicing.validation.application.port.out;

import com.einvoicing.validation.domain.event.InvoiceRejectedEvent;
import com.einvoicing.validation.domain.event.InvoiceValidatedEvent;

public interface ValidationResultPublisher {

    void publishValidatedEvent(InvoiceValidatedEvent  event);
    void publishRejectedEvent(InvoiceRejectedEvent event);
}
