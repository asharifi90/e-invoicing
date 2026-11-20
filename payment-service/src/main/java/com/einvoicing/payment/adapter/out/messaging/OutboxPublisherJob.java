package com.einvoicing.payment.adapter.out.messaging;

import com.einvoicing.payment.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.payment.adapter.out.persistence.repository.OutboxEventJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicLong unpublishedCount = new AtomicLong();
    private final Propagator propagator;
    private final Tracer tracer;

    public OutboxPublisherJob(OutboxEventJpaRepository repository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              MeterRegistry meterRegistry, Propagator propagator, Tracer tracer) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.propagator = propagator;
        this.tracer = tracer;
        // Gauge name: outbox.unpublished (current unpublished payment events).
        Gauge.builder("outbox.unpublished", unpublishedCount, AtomicLong::get)
                .description("Unpublished payment outbox events")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 2000)
    public void publishPending() {
        unpublishedCount.set(repository.countByPublishedAtIsNull());
        List<OutboxEventEntity> pending = repository.findPending(PageRequest.of(0, 50));
        for (OutboxEventEntity event : pending) {
            String tp = event.getTraceParent();
            Span span = (tp != null && !tp.isBlank())
                    ? propagator.extract(Map.of("traceparent", tp), Map::get).name("kafka.publish.payment").start()
                    : tracer.nextSpan().name("kafka.publish.payment").start();
            try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(event.getTopic(), event.getPayload());
                if (tp != null && !tp.isBlank()) {
                    record.headers().add("traceparent", tp.getBytes(StandardCharsets.UTF_8));
                }
                kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
                event.markPublished();
                repository.save(event);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Outbox publish failed id={}: {}", event.getId(), e.getMessage(), e);
                span.error(e);
                break;
            } finally {
                span.end();
            }
        }
    }
}
