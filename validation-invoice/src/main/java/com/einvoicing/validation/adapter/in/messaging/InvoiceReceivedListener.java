package com.einvoicing.validation.adapter.in.messaging;

import com.einvoicing.validation.application.port.in.ValidateInvoiceUseCase;
import com.einvoicing.validation.application.port.out.ProcessedEventStore;
import com.einvoicing.validation.domain.event.InvoiceReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class InvoiceReceivedListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceReceivedListener.class);

    private final ValidateInvoiceUseCase validateInvoiceUseCase;
    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEventStore;

    public InvoiceReceivedListener(ValidateInvoiceUseCase validateInvoiceUseCase,
                                   ObjectMapper objectMapper,
                                   ProcessedEventStore processedEventStore) {
        this.validateInvoiceUseCase = validateInvoiceUseCase;
        this.objectMapper = objectMapper;
        this.processedEventStore = processedEventStore;
    }

    @KafkaListener(topics = "invoice.received", groupId = "validation-invoice")
    public void onMessage(String message) {
        InvoiceReceivedEvent event;
        try {
            event = objectMapper.readValue(message, InvoiceReceivedEvent.class);
        } catch (JacksonException e) {
            log.error("Invalid message received", e);
            throw new IllegalArgumentException("Invalid message received", e);
        }

        log.info("received InvoiceReceivedEvent with invoice id : {}", event.getInvoiceId());

        String eventKey = "invoice.received:" + event.getInvoiceId();
        if (!processedEventStore.tryClaim(eventKey)) {
            log.info("Skipping duplicate InvoiceReceivedEvent with invoice id: {}", event.getInvoiceId());
            return;
        }
        try {
            validateInvoiceUseCase.validateReceivedEvent(event);
        } catch (RuntimeException | Error e) {
            processedEventStore.release(eventKey);
            throw e;
        }
    }
}
