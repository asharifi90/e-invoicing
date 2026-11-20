package com.einvoicing.invoice.adapter.out.persistence;

import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.invoice.adapter.out.persistence.repository.OutboxInvoiceRepository;
import com.einvoicing.invoice.application.port.out.OutboxWriter;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class JpaOutboxWriter implements OutboxWriter {

    private static final Logger LOG = LoggerFactory.getLogger(JpaOutboxWriter.class);
    private final OutboxInvoiceRepository outboxInvoiceRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public JpaOutboxWriter(OutboxInvoiceRepository outboxInvoiceRepository,
                           ObjectMapper objectMapper,
                           Tracer tracer) {
        this.outboxInvoiceRepository = outboxInvoiceRepository;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @Override
    public void enqueue(String aggregateType, String aggregateId, String eventType, Object event, String traceParent) {
        Span span = tracer.nextSpan().name("outbox.enqueue").start();
        try(Tracer.SpanInScope scope = tracer.withSpan(span)) {
            LOG.info("outbox.enqueue span started");
            span.tag("event.type", eventType);
            if(traceParent != null) {
                span.tag("traceParent.present", "true");
            }
            String receivedEvent = objectMapper.writeValueAsString(event);
            OutboxEventEntity outboxEventEntity = OutboxEventEntity.create(
                    aggregateType, aggregateId, eventType, receivedEvent
            );
            outboxEventEntity.setTraceParent(traceParent);
            outboxInvoiceRepository.save(outboxEventEntity);
        } catch (JsonParseException ex) {
            span.error(ex);
            throw new IllegalArgumentException("cannot serialize outbox json", ex);
        } finally {
            span.end();
        }
    }
}
