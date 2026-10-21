package com.einvoicing.approval.adapter.in.messaging;

import com.einvoicing.approval.application.port.in.ApproveInvoiceUseCase;
import com.einvoicing.approval.application.port.out.ProcessedEventStore;
import com.einvoicing.approval.domain.event.InvoiceValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ValidatedInvoiceListener {

    private static final Logger log = LoggerFactory.getLogger(ValidatedInvoiceListener.class);

    private final ObjectMapper objectMapper;
    private final ApproveInvoiceUseCase approveInvoiceUseCase;
    private final ProcessedEventStore processedEventStore;

    public ValidatedInvoiceListener(ObjectMapper objectMapper,
                                    ApproveInvoiceUseCase approveInvoiceUseCase,
                                    ProcessedEventStore processedEventStore) {
        this.objectMapper = objectMapper;
        this.approveInvoiceUseCase = approveInvoiceUseCase;
        this.processedEventStore = processedEventStore;
    }

    @KafkaListener(
            topics = "invoice.validated",
            groupId = "approval-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(String message){
        InvoiceValidatedEvent event;
        try {
            event = objectMapper.readValue(message, InvoiceValidatedEvent.class);
        }catch (JacksonException e) {
            log.error("invalid message received", e);
            throw new IllegalArgumentException("invalid message received", e);
        }

        log.info("received InvoiceValidatedEvent with invoice id : {}", event.getInvoiceId());
        String eventKey = "invoice.validated:" + event.getInvoiceId();
        if (!processedEventStore.tryClaim(eventKey)) {
            log.info("Skipping duplicate InvoiceValidatedEvent with invoice id: {}", event.getInvoiceId());
            return;
        }
        try {
            approveInvoiceUseCase.handleValidated(event);
        } catch (RuntimeException | Error e) {
            processedEventStore.release(eventKey);
            throw e;
        }
    }

}
