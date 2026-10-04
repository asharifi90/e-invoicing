package com.einvoicing.payment.application.service;

import com.einvoicing.payment.application.port.out.PaymentIdempotencyStore;
import com.einvoicing.payment.application.port.out.PaymentResultPublisher;
import com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus;
import com.einvoicing.payment.application.port.out.dto.PaymentProviderResult;
import com.einvoicing.payment.domain.event.InvoiceApprovedEvent;
import com.einvoicing.payment.domain.event.PaymentFailedEvent;
import com.einvoicing.payment.domain.event.PaymentSucceededEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessApprovedInvoiceServiceTest {

    @Mock
    private PaymentIdempotencyStore idempotencyStore;

    @Mock
    private ResilientPaymentGateway resilientPaymentGateway;

    @Mock
    private PaymentResultPublisher paymentResultPublisher;

    private ProcessApprovedInvoiceService service;

    @Mock
    private PaymentMetrics paymentMetrics;

    @BeforeEach
    void setUp() {
        service = new ProcessApprovedInvoiceService(
                paymentResultPublisher,
                resilientPaymentGateway,
                idempotencyStore,
                paymentMetrics
        );
    }

    private InvoiceApprovedEvent approvedEvent(UUID invoiceId) {
        InvoiceApprovedEvent event = new InvoiceApprovedEvent();
        event.setInvoiceId(invoiceId);
        event.setInvoiceNumber("INV-1");
        event.setTotalAmount(new BigDecimal("30.00"));
        event.setCurrency("EUR");
        event.setMode("AUTO");
        return event;
    }


    @Test
    void handle_firstAttempt_success_chargesAndPublishesSucceeded() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceApprovedEvent event = approvedEvent(invoiceId);

        when(idempotencyStore.tryBegin(invoiceId, event.getInvoiceNumber())).thenReturn(true);
        when(resilientPaymentGateway.charge(
                eq(invoiceId),
                eq("INV-1"),
                eq(new BigDecimal("30.00")),
                eq("EUR")
        )).thenReturn(PaymentProviderResult.success("SIM-123"));

        service.handle(event);

        verify(resilientPaymentGateway).charge(invoiceId, "INV-1", new BigDecimal("30.00"), "EUR");
        verify(idempotencyStore).markSucceeded(invoiceId, "SIM-123");
        verify(paymentResultPublisher).publishSucceeded(any(PaymentSucceededEvent.class));
        verify(paymentResultPublisher, never()).publishFailed(any());
    }


    @Test
    void handle_firstAttempt_providerFailure_publishesFailed() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceApprovedEvent event = approvedEvent(invoiceId);

        when(idempotencyStore.tryBegin(invoiceId, event.getInvoiceNumber())).thenReturn(true);
        when(resilientPaymentGateway.charge(any(), any(), any(), any()))
                .thenReturn(PaymentProviderResult.failure("provider down"));

        service.handle(event);

        verify(idempotencyStore).markFailed(invoiceId, "provider down");
        verify(paymentResultPublisher).publishFailed(any(PaymentFailedEvent.class));
        verify(paymentResultPublisher, never()).publishSucceeded(any());
    }


    @Test
    void handle_duplicateWhenSucceeded_skipsCharge() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceApprovedEvent event = approvedEvent(invoiceId);

        when(idempotencyStore.tryBegin(invoiceId, event.getInvoiceNumber())).thenReturn(false);
        when(idempotencyStore.findStatus(invoiceId))
                .thenReturn(Optional.of(PaymentAttemptStatus.SUCCEEDED));
        when(idempotencyStore.findProviderReference(invoiceId))
                .thenReturn(Optional.of("SIM-123"));

        service.handle(event);

        verify(resilientPaymentGateway, never()).charge(any(), any(), any(), any());
        verify(idempotencyStore, never()).markSucceeded(any(), any());
        verify(idempotencyStore, never()).markFailed(any(), any());
        verify(paymentResultPublisher).publishSucceeded(any(PaymentSucceededEvent.class));
    }


    @Test
    void handle_tryBeginFalse_whenStatusFailed_skipsCharge() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceApprovedEvent event = approvedEvent(invoiceId);

        when(idempotencyStore.tryBegin(invoiceId, event.getInvoiceNumber())).thenReturn(false);
        when(idempotencyStore.findStatus(invoiceId))
                .thenReturn(Optional.of(PaymentAttemptStatus.FAILED));

        service.handle(event);

        verify(resilientPaymentGateway, never()).charge(any(), any(), any(), any());
        verify(paymentResultPublisher, never()).publishSucceeded(any());
        verify(paymentResultPublisher, never()).publishFailed(any());
    }


    @Test
    void handle_afterFailed_tryBeginTrue_chargesAgain() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceApprovedEvent event = approvedEvent(invoiceId);

        when(idempotencyStore.tryBegin(invoiceId, event.getInvoiceNumber())).thenReturn(true);
        when(resilientPaymentGateway.charge(any(), any(), any(), any()))
                .thenReturn(PaymentProviderResult.success("SIM-RETRY"));

        service.handle(event);

        verify(resilientPaymentGateway).charge(invoiceId, "INV-1", new BigDecimal("30.00"), "EUR");
        verify(idempotencyStore).markSucceeded(invoiceId, "SIM-RETRY");
        verify(paymentResultPublisher).publishSucceeded(any(PaymentSucceededEvent.class));
    }
}