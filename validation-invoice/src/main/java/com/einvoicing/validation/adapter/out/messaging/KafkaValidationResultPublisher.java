package com.einvoicing.validation.adapter.out.messaging;

import com.einvoicing.validation.application.port.out.ValidationResultPublisher;
import com.einvoicing.validation.domain.event.InvoiceRejectedEvent;
import com.einvoicing.validation.domain.event.InvoiceValidatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaValidationResultPublisher implements ValidationResultPublisher {

    public static final String TOPIC_REJECTED = "invoice.rejected";
    public static final String TOPIC_VALIDATED = "invoice.validated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaValidationResultPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    @Override
    public void publishValidatedEvent(InvoiceValidatedEvent event) {
        kafkaTemplate.send(TOPIC_VALIDATED, event.getInvoiceId().toString(), event);
    }

    @Override
    public void publishRejectedEvent(InvoiceRejectedEvent event) {
        kafkaTemplate.send(TOPIC_REJECTED, event.getInvoiceId().toString(), event);
    }
}
