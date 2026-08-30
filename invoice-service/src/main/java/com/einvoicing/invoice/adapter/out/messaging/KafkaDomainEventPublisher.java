package com.einvoicing.invoice.adapter.out.messaging;

import com.einvoicing.invoice.application.port.out.DomainEventPublisher;
import com.einvoicing.invoice.domain.event.InvoiceReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.einvoicing.invoice.config.kafka.KafkaTopicConfig.INVOICE_RECEIVED_TOPIC;

@Component
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(InvoiceReceivedEvent event) {

        String key = event.getEventId().toString();

        log.info("publishing invoice received event for invoiceId: {}", key);
        kafkaTemplate.send(INVOICE_RECEIVED_TOPIC, key, event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("error while publishing invoice received event for invoiceId: {}", key);
                    } else {
                        log.debug("successfully published invoice received event for invoiceId: {}", key);
                    }

                });
    }
}
