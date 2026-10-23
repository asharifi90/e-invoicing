package com.einvoicing.payment.adapter.out.messaging;

import com.einvoicing.payment.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.payment.adapter.out.persistence.repository.OutboxEventJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicLong unpublishedCount = new AtomicLong();

    public OutboxPublisherJob(OutboxEventJpaRepository repository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
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
            try {
                kafkaTemplate.send(event.getTopic(), event.getPayload()).get(10, TimeUnit.SECONDS);
                event.markPublished();
                repository.save(event);
            } catch (Exception e) {
                log.warn("Outbox publish failed id={}: {}", event.getId(), e.getMessage(), e);
            }
        }
    }
}
