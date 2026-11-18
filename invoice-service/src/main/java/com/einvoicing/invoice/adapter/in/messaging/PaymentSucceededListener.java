package com.einvoicing.invoice.adapter.in.messaging;

import com.einvoicing.invoice.application.port.in.MarkPaymentSucceededUseCase;
import com.einvoicing.invoice.domain.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentSucceededListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededListener.class);

    private final MarkPaymentSucceededUseCase markPaymentSucceededUseCase;
    private final ObjectMapper objectMapper;

    public PaymentSucceededListener(
            MarkPaymentSucceededUseCase markPaymentSucceededUseCase,
            ObjectMapper objectMapper) {
        this.markPaymentSucceededUseCase = markPaymentSucceededUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "payment.succeeded",
            groupId = "invoice-service-payment-succeeded",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String message) {
        try {
            String payload = message;
            if (payload != null && payload.startsWith("\"")) {
                payload = objectMapper.readValue(payload, String.class);
            }
            PaymentSucceededEvent event =
                    deserialize(payload, PaymentSucceededEvent.class);

            log.info("Received payment.succeeded for invoice {}", event.getInvoiceId());
            markPaymentSucceededUseCase.markPaid(
                    event.getInvoiceId(),
                    event.getProviderReference());
        } catch (Exception e) {
            log.error("Failed to process payment.succeeded: {}", message, e);
            throw new IllegalArgumentException("Invalid payment.succeeded message", e);
        }
    }

    private <T> T deserialize(String message, Class<T> targetType) throws Exception {
        String payload = message;
        if (payload != null && payload.startsWith("\"")) {
            payload = objectMapper.readValue(payload, String.class);
        }
        return objectMapper.readValue(payload, targetType);
    }
}
