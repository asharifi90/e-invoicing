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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxInvoiceRepository outboxInvoiceRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter publishCounter;
    private final Counter failCounter;
    private final AtomicLong unpublishedCount = new AtomicLong();
    private final Tracer tracer;
    private final Propagator propagator;
    private final TracingKafkaSupport tracingKafkaSupport;

    public OutboxPublisherJob(OutboxInvoiceRepository outboxInvoiceRepository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              MeterRegistry meterRegistry,
                              Tracer tracer, Propagator propagator, TracingKafkaSupport tracingKafkaSupport) {
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
        // Gauge name: outbox.unpublished (current unpublished invoice events).
        Gauge.builder("outbox.unpublished", unpublishedCount, AtomicLong::get)
                .description("Unpublished invoice outbox events")
                .register(meterRegistry);
    }

    @Transactional
    @Scheduled(fixedDelay = 2000)
    public void publishPending(){

        unpublishedCount.set(outboxInvoiceRepository.countByPublishedAtIsNull());
        List<OutboxEventEntity> batch = outboxInvoiceRepository.findPending(PageRequest.of(0, 50));

        for (OutboxEventEntity event : batch) {
            String topic = toTopic(event.getEventType());
            String tp = event.getTraceParent();
            Span span;
            if (tp != null && !tp.isBlank()) {
                Map<String, String> carrier = new HashMap<>();
                carrier.put("traceparent", tp);
                Span.Builder builder = propagator.extract(carrier, Map::get);
                span = (builder != null)
                        ? builder.name("kafka.publish").start()
                        : tracer.nextSpan().name("kafka.publish").start();
            } else {
                span = tracer.nextSpan().name("kafka.publish").start();
            }
            try(Tracer.SpanInScope scope = tracer.withSpan(span)) {
                span.tag("kafka.topic", topic);
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(topic, event.getAggregatedId(), event.getPayload());
                tracingKafkaSupport.putTraceParent(record, event.getTraceParent());
                kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
                event.setPublishedAt();
                publishCounter.increment();
                outboxInvoiceRepository.save(event);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                failCounter.increment();
                log.warn("Outbox publish failed id={}: {}", event.getId(), e.getMessage());
                span.error(e);
                break;
            }finally {
                span.end();
            }
        }
    }

    private String toTopic(String eventType) {
        return switch (eventType) {
            case "InvoiceReceived" -> "invoice.received";
            default -> throw new IllegalArgumentException("invalid event type: " + eventType);
        };
    }
}
