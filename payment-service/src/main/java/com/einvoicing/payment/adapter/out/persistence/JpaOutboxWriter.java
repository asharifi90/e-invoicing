package com.einvoicing.payment.adapter.out.persistence;

import com.einvoicing.payment.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.payment.adapter.out.persistence.repository.OutboxEventJpaRepository;
import com.einvoicing.payment.application.port.out.OutboxWriter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class JpaOutboxWriter implements OutboxWriter {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaOutboxWriter(OutboxEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(String topic, Object event) {
        try {
            repository.save(OutboxEventEntity.create(topic, objectMapper.writeValueAsString(event)));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Cannot serialize payment event for outbox", e);
        }
    }
}
