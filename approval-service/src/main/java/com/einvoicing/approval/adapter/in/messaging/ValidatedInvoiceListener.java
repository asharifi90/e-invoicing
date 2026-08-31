package com.einvoicing.approval.adapter.in.messaging;

import com.einvoicing.approval.application.port.in.ApproveInvoiceUseCase;
import com.einvoicing.approval.domain.event.InvoiceValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ValidatedInvoiceListener {

    private static final Logger log = LoggerFactory.getLogger(ValidatedInvoiceListener.class);

    private final ObjectMapper objectMapper;
    private final ApproveInvoiceUseCase approveInvoiceUseCase;

    public ValidatedInvoiceListener(ObjectMapper objectMapper, ApproveInvoiceUseCase approveInvoiceUseCase) {
        this.objectMapper = objectMapper;
        this.approveInvoiceUseCase = approveInvoiceUseCase;
    }

    @KafkaListener(
            topics = "invoice.validated",
            groupId = "approval-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(String message) throws Exception {
        InvoiceValidatedEvent event =
                objectMapper.readValue(message, InvoiceValidatedEvent.class);
        log.info("received InvoiceValidatedEvent with invoice id : {}", event.getInvoiceId());
        approveInvoiceUseCase.handleValidated(event);
    }

}
