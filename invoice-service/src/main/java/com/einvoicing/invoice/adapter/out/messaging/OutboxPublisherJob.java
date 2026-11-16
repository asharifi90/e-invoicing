package com.einvoicing.invoice.adapter.out.messaging;

import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.invoice.adapter.out.persistence.repository.OutboxInvoiceRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public OutboxPublisherJob(OutboxInvoiceRepository outboxInvoiceRepository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              MeterRegistry meterRegistry,
                              Tracer tracer) {
        this.outboxInvoiceRepository = outboxInvoiceRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.publishCounter = Counter.builder("outbox.published")
                .description("Outbox events published successfully to kafka")
                .register( meterRegistry );
        this.failCounter = Counter.builder("outbox.published.failed")
                .description("Outbox events attempts that failed")
                .register( meterRegistry );
        this.tracer = tracer;
        // Gauge name: outbox.unpublished (current unpublished invoice events).
        Gauge.builder("outbox.unpublished", unpublishedCount, AtomicLong::get)
                .description("Unpublished invoice outbox events")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 2000)
    public void publishPending(){

        unpublishedCount.set(outboxInvoiceRepository.countByPublishedAtIsNull());
        List<OutboxEventEntity> batch = outboxInvoiceRepository.findPending(PageRequest.of(0, 50));

        for (OutboxEventEntity event : batch) {
            Span span = tracer.nextSpan().name("kafka.publish").start();
            try(Tracer.SpanInScope scope = tracer.withSpan(span)) {
                String topic = toTopic(event.getEventType());
                span.tag("kafka.topic", topic);
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getAggregatedId(), event.getPayload());
                kafkaTemplate.send(record);
                event.setPublishedAt();
                publishCounter.increment();
                outboxInvoiceRepository.save(event);
            } catch (Exception e) {
                failCounter.increment();
                log.warn("Outbox publish failed id={}: {}", event.getId(), e.getMessage());
                span.error(e);
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
