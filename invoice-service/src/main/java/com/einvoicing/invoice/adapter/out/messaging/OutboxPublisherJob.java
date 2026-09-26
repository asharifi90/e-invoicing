package com.einvoicing.invoice.adapter.out.messaging;

import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.invoice.adapter.out.persistence.repository.OutboxInvoiceRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxInvoiceRepository outboxInvoiceRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter publishCounter;
    private final Counter failCounter;

    public OutboxPublisherJob(OutboxInvoiceRepository outboxInvoiceRepository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              MeterRegistry meterRegistry) {
        this.outboxInvoiceRepository = outboxInvoiceRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.publishCounter = Counter.builder("outbox.published")
                .description("Outbox events published successfully to kafka")
                .register( meterRegistry );
        this.failCounter = Counter.builder("outbox.published.failed")
                .description("Outbox events attempts that failed")
                .register( meterRegistry );
    }

    @Scheduled(fixedDelay = 2000)
    public void publishPending(){

        List<OutboxEventEntity> batch = outboxInvoiceRepository.findPending(PageRequest.of(0, 50));

        for (OutboxEventEntity event : batch) {
            try {
                String topic = toTopic(event.getEventType());
                kafkaTemplate.send(topic, event.getAggregatedId(), event.getPayload());
                event.setPublishedAt();
                publishCounter.increment();
                outboxInvoiceRepository.save(event);
            } catch (Exception e) {
                failCounter.increment();
                log.warn("Outbox publish failed id={}: {}", event.getId(), e.getMessage());
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
