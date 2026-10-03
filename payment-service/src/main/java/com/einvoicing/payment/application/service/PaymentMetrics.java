package com.einvoicing.payment.application.service;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final Counter success;
    private final Counter failed;
    private final Counter duplicateSkipped;

    public PaymentMetrics(MeterRegistry meterRegistry) {

        this.success = Counter.builder("payment.charge.success")
                .description("successful payment charges")
                .register(meterRegistry);
        this.failed = Counter.builder("payment.charge.failed")
                .description("failed payment charges")
                .register(meterRegistry);
        this.duplicateSkipped = Counter.builder("payment.charge.duplicate_skipped")
                .description("duplicate payment charges")
                .register(meterRegistry);
    }

    public void  success() {
        success.increment();
    }
    public void failed() {
        failed.increment();
    }
    public void duplicateSkipped() {
        duplicateSkipped.increment();
    }
}
