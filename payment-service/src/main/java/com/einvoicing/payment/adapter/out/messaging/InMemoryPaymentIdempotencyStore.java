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
        Entry existing = store.putIfAbsent(invoiceId,
                new Entry(PaymentAttemptStatus.PROCESSING, null, null));
        return existing == null;
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
