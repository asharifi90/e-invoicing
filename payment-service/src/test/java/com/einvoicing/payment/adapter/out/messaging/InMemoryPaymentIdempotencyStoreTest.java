package com.einvoicing.payment.adapter.out.messaging;

import com.einvoicing.payment.application.port.out.PaymentIdempotencyStore;
import com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryPaymentIdempotencyStoreTest {


    private final PaymentIdempotencyStore paymentIdempotencyStore = new InMemoryPaymentIdempotencyStore();


    @Test
    void tryBegin_true_forFirstPaymentAttempt(){

        boolean tryBegin = paymentIdempotencyStore.tryBegin(UUID.randomUUID());

        Assertions.assertTrue(tryBegin);
    }

    @Test
    void tryBegin_false_forProcessingPaymentAttempt(){
        UUID invoiceId = UUID.randomUUID();
        paymentIdempotencyStore.tryBegin(invoiceId);
        boolean processing = paymentIdempotencyStore.tryBegin(invoiceId);

        Assertions.assertFalse(processing);
    }

    @Test
    void tryBegin_false_forSucceededPaymentAttempt(){
        UUID invoiceId = UUID.randomUUID();
        paymentIdempotencyStore.tryBegin(invoiceId);
        paymentIdempotencyStore.markSucceeded(invoiceId, "test");
        String paymentReference = paymentIdempotencyStore.findPaymentReference(invoiceId).get();

        Assertions.assertFalse(paymentIdempotencyStore.tryBegin(invoiceId));
        assertThat(paymentReference).isEqualTo("test");
    }

    @Test
    void tryBegin_true_afterFailedPaymentAttempt_allowsRetry(){

        UUID invoiceId = UUID.randomUUID();
        paymentIdempotencyStore.tryBegin(invoiceId);
        paymentIdempotencyStore.markFailed(invoiceId, "test");

        Assertions.assertTrue(paymentIdempotencyStore.tryBegin(invoiceId));
    }

    @Test
    void status_succeeded_after_markSucceededPaymentAttempt(){
        UUID invoiceId = UUID.randomUUID();
        paymentIdempotencyStore.tryBegin(invoiceId);
        paymentIdempotencyStore.markSucceeded(invoiceId, "test");
        Optional<PaymentAttemptStatus> status = paymentIdempotencyStore.findStatus(invoiceId);

        Assertions.assertTrue(status.get().equals(PaymentAttemptStatus.SUCCEEDED));
    }

    @Test
    void status_failed_after_markFailedPaymentAttempt(){
        UUID invoiceId = UUID.randomUUID();
        paymentIdempotencyStore.tryBegin(invoiceId);
        paymentIdempotencyStore.markFailed(invoiceId, "test");
        Optional<PaymentAttemptStatus> status = paymentIdempotencyStore.findStatus(invoiceId);

        Assertions.assertTrue(status.get().equals(PaymentAttemptStatus.FAILED));
    }
}
