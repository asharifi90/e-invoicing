package com.einvoicing.invoice.application.port.out;

import com.einvoicing.invoice.domain.event.InvoiceReceivedEvent;

/**
 * @deprecated Prefer OutboxWriter for InvoiceReceived – direct Kafka publish is dual-write unsafe.
 */
@Deprecated
public interface DomainEventPublisher {
    void publish(InvoiceReceivedEvent event);
}
