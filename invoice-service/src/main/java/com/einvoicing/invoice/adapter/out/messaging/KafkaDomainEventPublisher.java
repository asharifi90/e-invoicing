package com.einvoicing.invoice.adapter.out.messaging;

import com.einvoicing.invoice.application.port.out.DomainEventPublisher;
import com.einvoicing.invoice.domain.event.InvoiceReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static com.einvoicing.invoice.config.kafka.KafkaTopicConfig.INVOICE_RECEIVED_TOPIC;

@Component
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaDomainEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(InvoiceReceivedEvent event) {

        String key = event.getEventId().toString();

        log.info("publishing invoice received event for invoiceId: {}", key);
        String json = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(INVOICE_RECEIVED_TOPIC, key, json)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("error while publishing invoice received event for invoiceId: {}", key);
                    } else {
                        log.debug("successfully published invoice received event for invoiceId: {}", key);
                    }

                });
    }
}
