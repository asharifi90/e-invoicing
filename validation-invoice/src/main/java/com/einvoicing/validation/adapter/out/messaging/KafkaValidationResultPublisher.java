package com.einvoicing.validation.adapter.out.messaging;

import com.einvoicing.validation.application.port.out.ValidationResultPublisher;
import com.einvoicing.validation.domain.event.InvoiceRejectedEvent;
import com.einvoicing.validation.domain.event.InvoiceValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class KafkaValidationResultPublisher implements ValidationResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaValidationResultPublisher.class);

    public static final String TOPIC_REJECTED = "invoice.rejected";
    public static final String TOPIC_VALIDATED = "invoice.validated";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;


    public KafkaValidationResultPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                          ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }


    @Override
    public void publishValidatedEvent(InvoiceValidatedEvent event) {
        kafkaTemplate.send(TOPIC_VALIDATED, event.getInvoiceId().toString(), objectMapper.writeValueAsString(event));
        log.info("validated event published with invoiceNumber: {}", event.getInvoiceNumber());
    }

    @Override
    public void publishRejectedEvent(InvoiceRejectedEvent event) {
        kafkaTemplate.send(TOPIC_REJECTED, event.getInvoiceId().toString(), objectMapper.writeValueAsString(event));
        log.info("rejected event published with invoiceNumber: {}", event.getInvoiceNumber());
    }
}
