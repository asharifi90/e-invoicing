package com.einvoicing.payment.adapter.out.messaging;

import com.einvoicing.payment.application.port.out.PaymentResultPublisher;
import com.einvoicing.payment.domain.event.PaymentFailedEvent;
import com.einvoicing.payment.domain.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParseException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class KafkaPaymentResultPublisher implements PaymentResultPublisher {

    public static final Logger log = LoggerFactory.getLogger(KafkaPaymentResultPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaPaymentResultPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void paymentSucceeded(PaymentSucceededEvent event) {
        send("payment.succeeded", event.getInvoiceNumber(), objectMapper.writeValueAsString(event));
        log.info("payment succeeded event sent to kafka successfully, invoice number: {}", event.getInvoiceNumber());
    }

    @Override
    public void paymentFailed(PaymentFailedEvent event) {
        send("payment.failed", event.getInvoiceNumber(), objectMapper.writeValueAsString(event));
        log.info("payment failed event sent to kafka successfully, invoice number: {}", event.getInvoiceNumber());
    }

    private void send(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
        } catch (JsonParseException e) {
            log.error("Error while trying to publish payment result event with invoice number : {} with error message : {} "
                    , key, e.getMessage());
            throw new RuntimeException("Failed to serialize event to JSON", e);
        }
    }
}
