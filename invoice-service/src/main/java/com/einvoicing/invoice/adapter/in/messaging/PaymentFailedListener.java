package com.einvoicing.invoice.adapter.in.messaging;

import org.springframework.stereotype.Component;

import com.einvoicing.invoice.application.port.in.MarkPaymentFailedUseCase;
import com.einvoicing.invoice.domain.event.PaymentFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

@Component
public class PaymentFailedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedListener.class);

    private final MarkPaymentFailedUseCase markPaymentFailedUseCase;
    private final ObjectMapper objectMapper;

    public PaymentFailedListener(
            MarkPaymentFailedUseCase markPaymentFailedUseCase,
            ObjectMapper objectMapper) {
        this.markPaymentFailedUseCase = markPaymentFailedUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "invoice-service-payment-failed",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String message) {
        try {
            PaymentFailedEvent event =
                    objectMapper.readValue(message, PaymentFailedEvent.class);
            log.info("Received payment.failed for invoice {}", event.getInvoiceId());
            markPaymentFailedUseCase.markPaymentFailed(
                    event.getInvoiceId(),
                    event.getReason());
        } catch (Exception e) {
            log.error("Failed to process payment.failed: {}", message, e);
            throw new IllegalArgumentException("Invalid payment.failed message", e);
        }
    }
}
