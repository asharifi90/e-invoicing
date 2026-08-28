package com.einvoicing.validation.adapter.in.messaging;

import com.einvoicing.validation.application.port.in.ValidateInvoiceUseCase;
import com.einvoicing.validation.domain.event.InvoiceReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InvoiceReceivedListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceReceivedListener.class);

    private final ValidateInvoiceUseCase validateInvoiceUseCase;

    public InvoiceReceivedListener(ValidateInvoiceUseCase validateInvoiceUseCase) {
        this.validateInvoiceUseCase = validateInvoiceUseCase;
    }

    @KafkaListener(topics = "invoice.received", groupId = "validation-invoice")
    public void onMessage(InvoiceReceivedEvent event) {
        log.info("received InvoiceReceivedEvent with invoice id : {}", event.getInvoiceId());
        validateInvoiceUseCase.validateReceivedEvent(event);
    }
}
