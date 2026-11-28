package com.einvoicing.invoice.adapter.in.messaging;


import com.einvoicing.invoice.application.port.in.MarkInvoiceRejectedUseCase;
import com.einvoicing.invoice.domain.event.InvoiceRejectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InvoiceRejectedListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceRejectedListener.class);

    private final MarkInvoiceRejectedUseCase markInvoiceRejectedUseCase;
    private final KafkaEventDeserializer deserializer;

    public InvoiceRejectedListener(
            MarkInvoiceRejectedUseCase markInvoiceRejectedUseCase,
            KafkaEventDeserializer deserializer) {
        this.markInvoiceRejectedUseCase = markInvoiceRejectedUseCase;
        this.deserializer = deserializer;
    }

    @KafkaListener(
            topics = "invoice.rejected",
            groupId = "invoice-service-invoice-rejected",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String message) {
        try {
            InvoiceRejectedEvent event = deserializer.deserialize(message, InvoiceRejectedEvent.class);
            log.info("Received invoice.rejected for invoice {}", event.getInvoiceId());
            markInvoiceRejectedUseCase.markRejected(event.getInvoiceId(), event.getReason());
        } catch (Exception e) {
            log.error("Failed to process invoice.rejected: {}", message, e);
            throw new IllegalArgumentException("Invalid invoice.rejected message", e);
        }
    }
}
