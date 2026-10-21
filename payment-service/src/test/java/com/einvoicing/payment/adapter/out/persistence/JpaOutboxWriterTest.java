package com.einvoicing.payment.adapter.out.persistence;

import com.einvoicing.payment.adapter.out.persistence.entity.OutboxEventEntity;
import com.einvoicing.payment.adapter.out.persistence.repository.OutboxEventJpaRepository;
import com.einvoicing.payment.domain.event.PaymentFailedEvent;
import com.einvoicing.payment.domain.event.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaOutboxWriterTest {

    @Mock
    private OutboxEventJpaRepository repository;

    @Test
    void enqueue_serializesSucceededEventWithInvoiceListenerFields() throws Exception {
        JpaOutboxWriter writer = new JpaOutboxWriter(repository, new ObjectMapper());
        UUID invoiceId = UUID.randomUUID();
        PaymentSucceededEvent event = PaymentSucceededEvent.of(
                invoiceId, "INV-1", new BigDecimal("12.50"), "EUR", "provider-123");

        writer.enqueue("payment.succeeded", event);

        OutboxEventEntity saved = savedOutboxEvent();
        var payload = new ObjectMapper().readTree(saved.getPayload());
        assertThat(saved.getTopic()).isEqualTo("payment.succeeded");
        assertThat(payload.get("invoiceId").asString()).isEqualTo(invoiceId.toString());
        assertThat(payload.get("invoiceNumber").asString()).isEqualTo("INV-1");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(payload.get("currency").asString()).isEqualTo("EUR");
        assertThat(payload.get("providerReference").asString()).isEqualTo("provider-123");
        assertThat(payload.get("eventId")).isNotNull();
        assertThat(payload.get("paidAt")).isNotNull();
    }

    @Test
    void enqueue_serializesFailedEventWithInvoiceListenerFields() throws Exception {
        JpaOutboxWriter writer = new JpaOutboxWriter(repository, new ObjectMapper());
        UUID invoiceId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.of(
                invoiceId, "INV-1", "provider down", new BigDecimal("12.50"), "EUR");

        writer.enqueue("payment.failed", event);

        OutboxEventEntity saved = savedOutboxEvent();
        var payload = new ObjectMapper().readTree(saved.getPayload());
        assertThat(saved.getTopic()).isEqualTo("payment.failed");
        assertThat(payload.get("invoiceId").asString()).isEqualTo(invoiceId.toString());
        assertThat(payload.get("invoiceNumber").asString()).isEqualTo("INV-1");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(payload.get("currency").asString()).isEqualTo("EUR");
        assertThat(payload.get("reason").asString()).isEqualTo("provider down");
        assertThat(payload.get("eventId")).isNotNull();
        assertThat(payload.get("failedAt")).isNotNull();
    }

    private OutboxEventEntity savedOutboxEvent() {
        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
