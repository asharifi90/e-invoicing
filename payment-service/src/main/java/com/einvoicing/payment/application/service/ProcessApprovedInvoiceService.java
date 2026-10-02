package com.einvoicing.payment.application.service;

import com.einvoicing.payment.application.port.in.ProcessApprovedInvoiceUseCase;
import com.einvoicing.payment.application.port.out.PaymentIdempotencyStore;
import com.einvoicing.payment.application.port.out.PaymentResultPublisher;
import com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus;
import com.einvoicing.payment.application.port.out.dto.PaymentProviderResult;
import com.einvoicing.payment.domain.event.InvoiceApprovedEvent;
import com.einvoicing.payment.domain.event.PaymentFailedEvent;
import com.einvoicing.payment.domain.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProcessApprovedInvoiceService implements ProcessApprovedInvoiceUseCase {

    public static final Logger log = LoggerFactory.getLogger(ProcessApprovedInvoiceService.class);


    private final PaymentResultPublisher paymentResultPublisher;
    private final ResilientPaymentGateway resilientPaymentGateway;
    private final PaymentIdempotencyStore paymentIdempotencyStore;

    public ProcessApprovedInvoiceService(PaymentResultPublisher paymentResultPublisher,
                                         ResilientPaymentGateway resilientPaymentGateway,
                                         PaymentIdempotencyStore paymentIdempotencyStore) {
        this.paymentResultPublisher = paymentResultPublisher;
        this.resilientPaymentGateway = resilientPaymentGateway;
        this.paymentIdempotencyStore = paymentIdempotencyStore;
    }

    @Override
    public void handle(InvoiceApprovedEvent event) {

        UUID invoiceId = event.getInvoiceId();
        if (!paymentIdempotencyStore.tryBegin(invoiceId)) {
            PaymentAttemptStatus paymentAttemptStatus = paymentIdempotencyStore.findStatus(invoiceId).orElse(null);
            log.info("Duplicate payment skipped for invoice {} and status {}", invoiceId, paymentAttemptStatus);
            if (paymentAttemptStatus == PaymentAttemptStatus.SUCCEEDED) {
                paymentResultPublisher.publishSucceeded(
                        PaymentSucceededEvent.of(invoiceId, event.getInvoiceNumber(), event.getTotalAmount(),
                                getCurrency(event),
                                paymentIdempotencyStore.findPaymentReference(invoiceId).orElse("IDEMPOTENT-REPLAY"))
                );
            }
            return;
        }

        log.info("Processing approved invoice with invoice number: {}", event.getInvoiceNumber());

        try {
            PaymentProviderResult result = resilientPaymentGateway.charge(event.getInvoiceId(),
                    event.getInvoiceNumber(),
                    event.getTotalAmount(),
                    getCurrency(event));

            if (result.isSuccess()) {
                paymentIdempotencyStore.markSucceeded(invoiceId, result.getProviderReference());
                paymentResultPublisher.publishSucceeded(
                        PaymentSucceededEvent.of(event.getInvoiceId(),
                                event.getInvoiceNumber(),
                                event.getTotalAmount(),
                                getCurrency(event),
                                result.getProviderReference())
                );
            } else {
                paymentIdempotencyStore.markFailed(invoiceId, result.getFailureReason());
                paymentResultPublisher.publishFailed(
                        PaymentFailedEvent.of(event.getInvoiceId(),
                                event.getInvoiceNumber(),
                                result.getFailureReason(),
                                event.getTotalAmount(),
                                getCurrency(event))
                );
            }
        }catch (Exception ex) {
            log.error("Payment charge failed for invoice {}", invoiceId, ex);
            paymentIdempotencyStore.markFailed(invoiceId, ex.getMessage());
            paymentResultPublisher.publishFailed(
                    PaymentFailedEvent.of(
                            invoiceId,
                            event.getInvoiceNumber(),
                            ex.getMessage() != null ? ex.getMessage() : "payment failed",
                            event.getTotalAmount(),
                            getCurrency(event)
                    )
            );
        }
    }

    private static String getCurrency(InvoiceApprovedEvent event) {
        return event.getCurrency() != null ? event.getCurrency() : "EUR";
    }
}
