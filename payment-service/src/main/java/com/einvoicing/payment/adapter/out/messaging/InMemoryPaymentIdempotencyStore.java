package com.einvoicing.payment.adapter.out.messaging;

import com.einvoicing.payment.application.port.out.PaymentIdempotencyStore;
import com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPaymentIdempotencyStore implements PaymentIdempotencyStore {

    private final ConcurrentHashMap<UUID, Entry> store = new ConcurrentHashMap<>();

    private record Entry(PaymentAttemptStatus status, String providerReference, String reason){}

    @Override
    public boolean tryBegin(UUID invoiceId) {
        while (true) {
            Entry current = store.get(invoiceId);

            if (current == null) {
                Entry started = new Entry(PaymentAttemptStatus.PROCESSING, null, null);
                if (store.putIfAbsent(invoiceId, started) == null) {
                    return true;
                }
                continue;
            }

            if (current.status() == PaymentAttemptStatus.SUCCEEDED) {
                return false;
            }

            if (current.status() == PaymentAttemptStatus.PROCESSING) {
                return false;
            }

            if (current.status() == PaymentAttemptStatus.FAILED) {
                Entry restarted = new Entry(PaymentAttemptStatus.PROCESSING, null, null);
                if (store.replace(invoiceId, current, restarted)) {
                    return true;
                }
                continue;
            }

            return false;
        }
    }

    @Override
    public void markSucceeded(UUID invoiceId, String provideReference) {
        store.put(invoiceId, new Entry(PaymentAttemptStatus.SUCCEEDED, provideReference, null));
    }

    @Override
    public void markFailed(UUID invoiceId, String reason) {
        store.put(invoiceId, new Entry(PaymentAttemptStatus.FAILED, reason, null));
    }

    @Override
    public Optional<PaymentAttemptStatus> findStatus(UUID invoiceId) {
        return Optional.ofNullable(store.get(invoiceId)).map(Entry::status);
    }

    @Override
    public Optional<String> findPaymentReference(UUID invoiceId) {
        return Optional.ofNullable(store.get(invoiceId))
                .map(Entry::providerReference)
                .filter(r -> !r.isBlank());
    }
}
