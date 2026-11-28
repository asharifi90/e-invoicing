package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class InvoiceMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public InvoiceMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordE2eDuration(Invoice invoice, Instant completedAt, String outcome) {
        if (invoice == null || invoice.getReceivedAt() == null) {
            return;
        }
        Duration duration = Duration.between(invoice.getReceivedAt(), completedAt);
        if (!duration.isNegative()) {
            Timer.builder("invoice.e2e.duration")
                    .description("End-to-end invoice processing duration from creation to terminal state")
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .record(duration);
        }
    }
}
