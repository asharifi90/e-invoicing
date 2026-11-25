package com.einvoicing.invoice.adapter.out.messaging;

import com.einvoicing.invoice.AbstractIntegrationTest;
import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.invoice.adapter.out.persistence.repository.OutboxInvoiceRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxKafkaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxPublisherJob outboxPublisher;

    @Autowired
    private OutboxInvoiceRepository outboxRepository;

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void publishPending_shouldSendEventToKafka_andMarkAsPublished() {
        String invoiceId = UUID.randomUUID().toString();
        String payload = """
                {"invoiceId":"%s",
                "invoiceNumber":"INV-2026-001",
                "totalAmount":100.00,
                "currency":"EUR"}
                """.formatted(invoiceId);

        outboxRepository.saveAndFlush(
                OutboxEventEntity.create("Invoice", invoiceId, "InvoiceReceived", payload)
        );

        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID(),
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );

        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer()) {

            consumer.subscribe(List.of("invoice.received"));

            outboxPublisher.publishPending();

            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, "invoice.received", Duration.ofSeconds(10));

            assertThat(record.value()).contains("INV-2026-001");
            assertThat(outboxRepository.countByPublishedAtIsNull()).isZero();
        }
    }
}
