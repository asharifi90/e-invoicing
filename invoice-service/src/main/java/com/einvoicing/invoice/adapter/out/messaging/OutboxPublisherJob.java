package com.einvoicing.invoice.adapter.out.messaging;

import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.invoice.adapter.out.persistence.repository.OutboxInvoiceRepository;
import com.einvoicing.invoice.config.TracingKafkaSupport;
import io.micrometer.core.instrument.Counter;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(com.einvoicing.invoice.adapter.out.messaging.OutboxPublisherJob.class);

    private final OutboxInvoiceRepository outboxInvoiceRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter publishCounter;
    private final Counter failCounter;
    private final AtomicLong unpublishedCount = new AtomicLong();
    private final Tracer tracer;
    private final Propagator propagator;
    private final TracingKafkaSupport tracingKafkaSupport;
    private final TransactionTemplate transactionTemplate;

    public OutboxPublisherJob(OutboxInvoiceRepository outboxInvoiceRepository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              MeterRegistry meterRegistry,
                              Tracer tracer, Propagator propagator,
                              TracingKafkaSupport tracingKafkaSupport,
                              TransactionTemplate transactionTemplate) {
        this.outboxInvoiceRepository = outboxInvoiceRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.publishCounter = Counter.builder("outbox.published")
                .description("Outbox events published successfully to kafka")
                .register( meterRegistry );
        this.failCounter = Counter.builder("outbox.published.failed")
                .description("Outbox events attempts that failed")
                .register( meterRegistry );
        this.tracer = tracer;
        this.propagator = propagator;
        this.tracingKafkaSupport = tracingKafkaSupport;
        this.transactionTemplate = transactionTemplate;
        // Gauge name: outbox.unpublished (current unpublished invoice events).
        Gauge.builder("outbox.unpublished", unpublishedCount, AtomicLong::get)
                .description("Unpublished invoice outbox events")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 500)
    public void publishPending() {
        unpublishedCount.set(outboxInvoiceRepository.countByPublishedAtIsNull());

        List<OutboxEventEntity> batch = transactionTemplate.execute(status ->
                outboxInvoiceRepository.claimPending(250)
        );

        if (batch == null || batch.isEmpty()) {
            return;
        }

        record InFlightEvent(OutboxEventEntity entity, Span span, CompletableFuture<?> future) {}
        List<InFlightEvent> inFlight = new ArrayList<>(batch.size());

        for (OutboxEventEntity event : batch) {
            String topic = toTopic(event.getEventType());
            String tp = event.getTraceParent();

            Span span = (tp != null && !tp.isBlank())
                    ? propagator.extract(Map.of("traceparent", tp), Map::get).name("kafka.publish").start()
                    : tracer.nextSpan().name("kafka.publish").start();

            try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
                span.tag("kafka.topic", topic);
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(topic, event.getAggregatedId(), event.getPayload());
                tracingKafkaSupport.putTraceParent(record, tp);

                CompletableFuture<?> future = kafkaTemplate.send(record);
                inFlight.add(new InFlightEvent(event, span, future));
            } catch (Exception e) {
                span.error(e);
                span.end();
                failCounter.increment();
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

        List<UUID> succeeded = new ArrayList<>();
        for (InFlightEvent item : inFlight) {
            try {
                if (item.future().isDone() && !item.future().isCompletedExceptionally()) {
                    succeeded.add(item.entity.getId());
                    publishCounter.increment();
                } else {
                    failCounter.increment();
                    item.span().error(new RuntimeException("Kafka publish failed or timed out"));
                }
            } finally {
                item.span().end();
            }
        }

        if (!succeeded.isEmpty()) {
            transactionTemplate.executeWithoutResult(status ->
                    outboxInvoiceRepository.markPublished(succeeded, Instant.now())
            );
        }
    }

    private String toTopic(String eventType) {
        return switch (eventType) {
            case "InvoiceReceived" -> "invoice.received";
            default -> throw new IllegalArgumentException("invalid event type: " + eventType);
        };
    }
}
