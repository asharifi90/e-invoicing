package com.einvoicing.payment.application.port.out;

import com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIdempotencyStore {

    /**
     * @return true, if we can request a new payment for this invoiceId
     */
    boolean tryBegin(UUID invoiceId);
    void markSucceeded(UUID invoiceId, String provideReference);
    void markFailed(UUID invoiceId, String reason);
    Optional<PaymentAttemptStatus> findStatus(UUID invoiceId);
    Optional<String> findPaymentReference(UUID invoiceId);
}
