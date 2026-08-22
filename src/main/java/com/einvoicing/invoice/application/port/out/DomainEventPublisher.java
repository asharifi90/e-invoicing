package com.einvoicing.invoice.application.port.out;

import com.einvoicing.invoice.domain.event.InvoiceReceivedEvent;

public interface DomainEventPublisher {

    void publish(InvoiceReceivedEvent event);
}
