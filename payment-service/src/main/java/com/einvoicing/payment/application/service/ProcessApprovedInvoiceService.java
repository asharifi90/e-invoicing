package com.einvoicing.payment.application.service;

import com.einvoicing.payment.application.port.in.ProcessApprovedInvoiceUseCase;
import com.einvoicing.payment.application.port.out.PaymentIdempotencyStore;
import com.einvoicing.payment.application.port.out.OutboxWriter;
import com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus;
import com.einvoicing.payment.application.port.out.dto.PaymentProviderResult;
import com.einvoicing.payment.domain.event.InvoiceApprovedEvent;
import com.einvoicing.payment.domain.event.PaymentFailedEvent;
import com.einvoicing.payment.domain.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessApprovedInvoiceService implements ProcessApprovedInvoiceUseCase {

    public static final Logger log = LoggerFactory.getLogger(ProcessApprovedInvoiceService.class);


    private final OutboxWriter outboxWriter;
    private final ResilientPaymentGateway resilientPaymentGateway;
    private final PaymentIdempotencyStore paymentIdempotencyStore;
    private final PaymentMetrics paymentMetrics;

    public ProcessApprovedInvoiceService(OutboxWriter outboxWriter,
                                         ResilientPaymentGateway resilientPaymentGateway,
                                         PaymentIdempotencyStore paymentIdempotencyStore,
                                         PaymentMetrics paymentMetrics) {
        this.outboxWriter = outboxWriter;
        this.resilientPaymentGateway = resilientPaymentGateway;
        this.paymentIdempotencyStore = paymentIdempotencyStore;
        this.paymentMetrics = paymentMetrics;
    }

    @Override
    @Transactional
    public void handle(InvoiceApprovedEvent event) {

        UUID invoiceId = event.getInvoiceId();
        if (!paymentIdempotencyStore.tryBegin(invoiceId, event.getInvoiceNumber())) {
            PaymentAttemptStatus paymentAttemptStatus = paymentIdempotencyStore.findStatus(invoiceId).orElse(null);
            log.info("Duplicate payment skipped for invoice {} and status {}", invoiceId, paymentAttemptStatus);
            paymentMetrics.duplicateSkipped();
            if (paymentAttemptStatus == PaymentAttemptStatus.SUCCEEDED) {
                outboxWriter.enqueue("payment.succeeded", PaymentSucceededEvent.of(
                        invoiceId, event.getInvoiceNumber(), event.getTotalAmount(), getCurrency(event),
                        paymentIdempotencyStore.findProviderReference(invoiceId).orElse("IDEMPOTENT-REPLAY")));
            }
            return;
        }

        log.info("Processing approved invoice with invoice number: {}", event.getInvoiceNumber());

        PaymentProviderResult result;
        try {
            result = resilientPaymentGateway.charge(event.getInvoiceId(),
                    event.getInvoiceNumber(),
                    event.getTotalAmount(),
                    getCurrency(event));
        } catch (Exception ex) {
            log.error("Payment charge failed for invoice {}", invoiceId, ex);
            enqueueFailure(event, ex.getMessage() != null ? ex.getMessage() : "payment failed");
            return;
        }

        if (result.isSuccess()) {
            paymentIdempotencyStore.markSucceeded(invoiceId, result.getProviderReference());
            paymentMetrics.success();
            outboxWriter.enqueue("payment.succeeded", PaymentSucceededEvent.of(
                    invoiceId, event.getInvoiceNumber(), event.getTotalAmount(), getCurrency(event),
                    result.getProviderReference()));
        } else {
            enqueueFailure(event, result.getFailureReason());
        }
    }

    private void enqueueFailure(InvoiceApprovedEvent event, String reason) {
        paymentIdempotencyStore.markFailed(event.getInvoiceId(), reason);
        paymentMetrics.failed();
        outboxWriter.enqueue("payment.failed", PaymentFailedEvent.of(
                event.getInvoiceId(), event.getInvoiceNumber(), reason, event.getTotalAmount(),
                getCurrency(event)));
    }

    private static String getCurrency(InvoiceApprovedEvent event) {
        return event.getCurrency() != null ? event.getCurrency() : "EUR";
    }
}
