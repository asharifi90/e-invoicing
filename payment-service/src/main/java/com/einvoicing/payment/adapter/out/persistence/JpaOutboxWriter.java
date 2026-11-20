package com.einvoicing.payment.adapter.out.persistence;

import com.einvoicing.payment.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.payment.adapter.out.persistence.repository.OutboxEventJpaRepository;
import com.einvoicing.payment.application.port.out.OutboxWriter;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
public class JpaOutboxWriter implements OutboxWriter {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final Propagator propagator;

    public JpaOutboxWriter(OutboxEventJpaRepository repository,
                           ObjectMapper objectMapper,
                           Tracer tracer,
                           Propagator propagator) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public void enqueue(String topic, Object event) {
        try {
            String traceParent = null;
            if(tracer.currentSpan() != null) {
                Map<String, String> carrier = new HashMap<>();
                propagator.inject(tracer.currentSpan().context(), carrier, Map::put);
                traceParent = carrier.get("traceparent");
            }
            repository.save(OutboxEventEntity.create(topic, objectMapper.writeValueAsString(event), traceParent));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Cannot serialize payment event for outbox", e);
        }
    }
}
