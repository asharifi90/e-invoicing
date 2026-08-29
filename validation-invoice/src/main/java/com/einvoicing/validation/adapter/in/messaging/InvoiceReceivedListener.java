package com.einvoicing.validation.adapter.in.messaging;

import com.einvoicing.validation.application.port.in.ValidateInvoiceUseCase;
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

    public InvoiceReceivedListener(ValidateInvoiceUseCase validateInvoiceUseCase,
                                   ObjectMapper objectMapper) {
        this.validateInvoiceUseCase = validateInvoiceUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "invoice.received", groupId = "validation-invoice")
    public void onMessage(String message) {
        try {
            InvoiceReceivedEvent event = objectMapper.readValue(message, InvoiceReceivedEvent.class);
            log.info("received InvoiceReceivedEvent with invoice id : {}", event.getInvoiceId());
            validateInvoiceUseCase.validateReceivedEvent(event);
        }catch (JacksonException e){
            log.error("Invalid message received: {}", message);
            throw new IllegalArgumentException("Invalid message received: " + message);
        }
    }
}
