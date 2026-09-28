package com.einvoicing.payment.application.service;

import com.einvoicing.payment.application.port.out.PaymentProvider;
import com.einvoicing.payment.application.port.out.dto.PaymentProviderResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ResilientPaymentGateway {

    private final PaymentProvider paymentProvider;

    public ResilientPaymentGateway(PaymentProvider paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    @CircuitBreaker(name = "paymentProvider", fallbackMethod = "chargeFallback")
    @Retry(name = "paymentProvider")
    public PaymentProviderResult charge(UUID invoiceId, String invoiceNumber, BigDecimal amount, String currency) {
        return paymentProvider.charge(invoiceId, invoiceNumber, amount, currency);
    }

    public  PaymentProviderResult chargeFallback(UUID invoiceId, BigDecimal amount, String currency, Throwable ex) {
        return PaymentProviderResult.failure("payment provider unavailable" + ex.getMessage());
    }
}
