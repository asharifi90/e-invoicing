package com.einvoicing.payment.adapter.out.messaging;

import com.einvoicing.payment.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.payment.adapter.out.persistence.repository.OutboxEventJpaRepository;
import com.einvoicing.payment.config.TracingKafkaSupport;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(com.einvoicing.payment.adapter.out.messaging.OutboxPublisherJob.class);

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicLong unpublishedCount = new AtomicLong();
    private final Propagator propagator;
    private final Tracer tracer;
    private final TracingKafkaSupport tracingKafkaSupport;
    private final TransactionTemplate transactionTemplate;

    public OutboxPublisherJob(OutboxEventJpaRepository repository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              MeterRegistry meterRegistry,
                              Propagator propagator,
                              Tracer tracer,
                              TracingKafkaSupport tracingKafkaSupport,
                              TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.propagator = propagator;
        this.tracer = tracer;
        this.tracingKafkaSupport = tracingKafkaSupport;
        this.transactionTemplate = transactionTemplate;
        // Gauge name: outbox.unpublished (current unpublished payment events).
        Gauge.builder("outbox.unpublished", unpublishedCount, AtomicLong::get)
                .description("Unpublished payment outbox events")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 500)
    public void publishPending() {
        unpublishedCount.set(repository.countByPublishedAtIsNull());

        List<OutboxEventEntity> batch = transactionTemplate.execute(status ->
                repository.findPending(PageRequest.of(0, 250))
        );

        if (batch == null || batch.isEmpty()) {
            return;
        }

        record InFlightEvent(OutboxEventEntity entity, Span span, CompletableFuture<?> future) {
        }
        List<InFlightEvent> inFlight = new ArrayList<>(batch.size());

        for (OutboxEventEntity event : batch) {
            String topic = event.getTopic();
            String tp = event.getTraceParent();

            Span span = (tp != null && !tp.isBlank())
                    ? propagator.extract(Map.of("traceparent", tp), Map::get).name("kafka.publish").start()
                    : tracer.nextSpan().name("kafka.publish").start();

            try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
                span.tag("kafka.topic", topic);
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(topic, event.getId().toString(), event.getPayload());
                tracingKafkaSupport.putTraceParent(record, tp);

                CompletableFuture<?> future = kafkaTemplate.send(record);
                inFlight.add(new InFlightEvent(event, span, future));
            } catch (Exception e) {
                span.error(e);
                span.end();
                log.warn("Failed to enqueue outbox event id={}: {}", event.getId(), e.getMessage());
            }
        }

        try {
            CompletableFuture<Void> allAcks = CompletableFuture.allOf(
                    inFlight.stream().map(InFlightEvent::future).toArray(CompletableFuture[]::new)
            );
            allAcks.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Batch wait interrupted or timed out: {}", e.getMessage());
        }

        List<OutboxEventEntity> succeeded = new ArrayList<>();
        for (InFlightEvent item : inFlight) {
            try {
                if (item.future().isDone() && !item.future().isCompletedExceptionally()) {
                    item.entity().setPublishedAt();
                    succeeded.add(item.entity());
                } else {
                    item.span().error(new RuntimeException("Kafka publish failed or timed out"));
                }
            } finally {
                item.span().end();
            }
        }

        if (!succeeded.isEmpty()) {
            transactionTemplate.executeWithoutResult(status ->
                    repository.saveAll(succeeded)
            );
        }
    }

}

