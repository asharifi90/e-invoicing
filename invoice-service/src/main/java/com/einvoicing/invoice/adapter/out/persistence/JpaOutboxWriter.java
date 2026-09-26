package com.einvoicing.invoice.adapter.out.persistence;

import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.invoice.adapter.out.persistence.repository.OutboxInvoiceRepository;
import com.einvoicing.invoice.application.port.out.OutboxWriter;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class JpaOutboxWriter implements OutboxWriter {

    private final OutboxInvoiceRepository outboxInvoiceRepository;
    private final ObjectMapper objectMapper;

    public JpaOutboxWriter(OutboxInvoiceRepository outboxInvoiceRepository,
                           ObjectMapper objectMapper) {
        this.outboxInvoiceRepository = outboxInvoiceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(String aggregateType, String aggregateId, String eventType, Object event) {

        try {
            String receivedEvent = objectMapper.writeValueAsString(event);
            OutboxEventEntity outboxEventEntity = OutboxEventEntity.create(
                    aggregateType, aggregateId, eventType, receivedEvent
            );
            outboxInvoiceRepository.save(outboxEventEntity);
        } catch (JsonParseException ex) {
            throw new IllegalArgumentException("cannot serialize outbox json", ex);
        }
    }
}
