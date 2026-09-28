package com.einvoicing.payment.application.service;

import com.einvoicing.payment.application.port.in.ProcessApprovedInvoiceUseCase;
import com.einvoicing.payment.application.port.out.PaymentResultPublisher;
import com.einvoicing.payment.application.port.out.dto.PaymentProviderResult;
import com.einvoicing.payment.domain.event.InvoiceApprovedEvent;
import com.einvoicing.payment.domain.event.PaymentFailedEvent;
import com.einvoicing.payment.domain.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessApprovedInvoiceService implements ProcessApprovedInvoiceUseCase {

    public static final Logger log = LoggerFactory.getLogger(ProcessApprovedInvoiceService.class);


    private final PaymentResultPublisher paymentResultPublisher;
    private final ResilientPaymentGateway resilientPaymentGateway;

    public ProcessApprovedInvoiceService(PaymentResultPublisher paymentResultPublisher,
                                         ResilientPaymentGateway resilientPaymentGateway) {
        this.paymentResultPublisher = paymentResultPublisher;
        this.resilientPaymentGateway = resilientPaymentGateway;
    }

    @Override
    public void handle(InvoiceApprovedEvent event) {

        log.info("Processing approved invoice with invoice number: {}", event.getInvoiceNumber());

        PaymentProviderResult result = resilientPaymentGateway.charge(event.getInvoiceId(),
                event.getInvoiceNumber(),
                event.getTotalAmount(),
                event.getCurrency() != null ? event.getCurrency() : "EUR");

        if (result.isSuccess()){
            paymentResultPublisher.paymentSucceeded(
                    PaymentSucceededEvent.of(event.getInvoiceId(),
                            event.getInvoiceNumber(),
                            event.getTotalAmount(),
                            event.getCurrency()  != null ? event.getCurrency() : "EUR",
                            result.getProviderReference())
            );
        } else {
            paymentResultPublisher.paymentFailed(
                    PaymentFailedEvent.of(event.getInvoiceId(),
                            event.getInvoiceNumber(),
                            result.getFailureReason(),
                            event.getTotalAmount(),
                            event.getCurrency() != null ? event.getCurrency() : "EUR")
            );
        }
    }
}
