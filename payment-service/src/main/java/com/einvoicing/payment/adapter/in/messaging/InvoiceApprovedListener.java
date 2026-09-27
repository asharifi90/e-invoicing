package com.einvoicing.payment.adapter.in.messaging;

import com.einvoicing.payment.application.port.in.ProcessApprovedInvoiceUseCase;
import com.einvoicing.payment.domain.event.InvoiceApprovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InvoiceApprovedListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceApprovedListener.class);

    private final ProcessApprovedInvoiceUseCase processApprovedInvoiceUseCase;
    private final ObjectMapper objectMapper;

    public InvoiceApprovedListener(ProcessApprovedInvoiceUseCase processApprovedInvoiceUseCase, ObjectMapper objectMapper) {
        this.processApprovedInvoiceUseCase = processApprovedInvoiceUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "invoice.approved", groupId = "payment-service", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String message) {

        try {
            InvoiceApprovedEvent invoiceApprovedEvent = objectMapper.readValue(message, InvoiceApprovedEvent.class);
            log.info("Received invoice.approved for : {}", invoiceApprovedEvent.getInvoiceNumber());
            processApprovedInvoiceUseCase.handle(invoiceApprovedEvent);
        } catch (Exception e) {
            log.error("Failed to process invoice.approved for : {} ", message, e);
            throw new IllegalArgumentException("Invalid invoice approved message", e);
        }
    }
}
