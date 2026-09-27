package com.einvoicing.payment.application.port.out;

import com.einvoicing.payment.application.port.out.dto.PaymentProviderResult;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProvider {
    PaymentProviderResult charge(UUID invoiceId, String invoiceNumber, BigDecimal amount, String currency);
}
