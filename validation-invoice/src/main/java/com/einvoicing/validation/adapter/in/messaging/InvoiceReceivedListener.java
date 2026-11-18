package com.einvoicing.validation.adapter.in.messaging;

import com.einvoicing.validation.application.port.in.ValidateInvoiceUseCase;
import com.einvoicing.validation.application.port.out.ProcessedEventStore;
import com.einvoicing.validation.domain.event.InvoiceReceivedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    private final ProcessedEventStore processedEventStore;
    private final Tracer tracer;

    public InvoiceReceivedListener(ValidateInvoiceUseCase validateInvoiceUseCase,
                                   ObjectMapper objectMapper,
                                   ProcessedEventStore processedEventStore,
                                   Tracer tracer) {
        this.validateInvoiceUseCase = validateInvoiceUseCase;
        this.objectMapper = objectMapper;
        this.processedEventStore = processedEventStore;
        this.tracer = tracer;
    }

    @KafkaListener(topics = "invoice.received", groupId = "validation-invoice")
    public void onMessage(ConsumerRecord<String, String> record) {
        Span span = tracer.nextSpan().name("validate-invoice").start();
        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            InvoiceReceivedEvent event;
            try {
                event = objectMapper.readValue(record.value(), InvoiceReceivedEvent.class);
            } catch (JacksonException e) {
                log.error("Invalid message received", e);
                span.error(e);
                throw new IllegalArgumentException("Invalid message received", e);
            }
            span.tag("invoiceId", String.valueOf(event.getInvoiceId()));
            log.info("received InvoiceReceivedEvent with invoice id : {}", event.getInvoiceId());

            String eventKey = "invoice.received:" + event.getInvoiceId();
            if (!processedEventStore.tryClaim(eventKey)) {
                log.info("Skipping duplicate InvoiceReceivedEvent with invoice id: {}", event.getInvoiceId());
                return;
            }
            try {
                validateInvoiceUseCase.validateReceivedEvent(event);
            } catch (RuntimeException | Error e) {
                processedEventStore.release(eventKey);
                span.error(e);
                throw e;
            }
        }finally {
            span.end();
        }
    }
}
