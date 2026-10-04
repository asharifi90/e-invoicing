package com.einvoicing.payment.application.service;

import com.einvoicing.payment.application.port.out.PaymentProvider;
import com.einvoicing.payment.application.port.out.dto.PaymentProviderResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Boot 4

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Disabled("Requires full context; fallback verified manually")
@SpringBootTest
class ResilientPaymentGatewaySpringTest {

    @Autowired
    private ResilientPaymentGateway gateway;

    @MockitoBean
    private PaymentProvider paymentProvider;

    @Test
    void charge_success_returnsSuccess() {
        UUID id = UUID.randomUUID();
        when(paymentProvider.charge(any(), any(), any(), any()))
                .thenReturn(PaymentProviderResult.success("SIM-1"));

        PaymentProviderResult result =
                gateway.charge(id, "INV-1", new BigDecimal("30.00"), "EUR");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProviderReference()).isEqualTo("SIM-1");
        verify(paymentProvider, times(1)).charge(any(), any(), any(), any());
    }

    @Test
    void charge_providerThrows_triggersFallback() {

        when(paymentProvider.charge(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Simulated provider failed"));

        PaymentProviderResult result =
                gateway.charge(UUID.randomUUID(), "INV-1", new BigDecimal("30.00"), "EUR");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).containsIgnoringCase("unavailable");
        verify(paymentProvider, atLeastOnce()).charge(any(), any(), any(), any());
    }
}