package com.einvoicing.invoice.application.port.out;

import com.einvoicing.invoice.event.InvoiceReceivedEvent;

public interface DomainEventPublisher {

    void publish(InvoiceReceivedEvent event);
}
