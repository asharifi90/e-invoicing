package com.einvoicing.payment.adapter.out.provider;

import com.einvoicing.payment.application.port.out.PaymentProvider;
import com.einvoicing.payment.application.port.out.dto.PaymentProviderResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SimulatedPaymentProvider implements PaymentProvider {


    private final String mode;
    private final long slowMs;

    public SimulatedPaymentProvider(@Value("${app.payment.mode:SUCCESS}") String mode,
                                    @Value("${app.payment.slow-ms:3000}") long slowMs) {
        this.mode = mode;
        this.slowMs = slowMs;
    }

    @Override
    public PaymentProviderResult charge(UUID invoiceId, String invoiceNumber, BigDecimal amount, String currency) {

        return switch (mode.toUpperCase()) {
            case "FAIL" -> throw new IllegalArgumentException("Simulated provider failed");
            case "SLOW" -> {
                try {
                    Thread.sleep(slowMs);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    yield PaymentProviderResult.failure("Interrupted during slow payment");
                }
                yield PaymentProviderResult.success("SIM-" + UUID.randomUUID());
            }
            default -> PaymentProviderResult.success("SIM-" + UUID.randomUUID());
        };
    }
}
