package com.einvoicing.payment.adapter.out.persistence;

import com.einvoicing.payment.application.port.out.PaymentIdempotencyStore;
import com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPaymentIdempotencyStore implements PaymentIdempotencyStore {

    private final PaymentAttemptJpaRepository repository;

    public JpaPaymentIdempotencyStore(PaymentAttemptJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * Wins the right to charge:
     * - no row → INSERT PROCESSING
     * - FAILED → conditional UPDATE to PROCESSING
     * - PROCESSING or SUCCEEDED → false
     */
    @Override
    @Transactional
    public boolean tryBegin(UUID invoiceId) {
        Optional<PaymentAttemptEntity> existing = repository.findById(invoiceId);

        if (existing.isEmpty()) {
            try {
                repository.saveAndFlush(PaymentAttemptEntity.start(invoiceId, null));
                return true;
            } catch (DataIntegrityViolationException e) {
                return false;
            }
        }

        PaymentAttemptStatus status = existing.get().getStatus();
        if (status == PaymentAttemptStatus.SUCCEEDED
                || status == PaymentAttemptStatus.PROCESSING) {
            return false;
        }

        int updated = repository.reopenIfFailed(invoiceId);
        return updated == 1;
    }

    /** Overload if you want to store invoiceNumber at start — optional */
    @Transactional
    public boolean tryBegin(UUID invoiceId, String invoiceNumber) {
        Optional<PaymentAttemptEntity> existing = repository.findById(invoiceId);
        if (existing.isEmpty()) {
            try {
                repository.saveAndFlush(PaymentAttemptEntity.start(invoiceId, invoiceNumber));
                return true;
            } catch (DataIntegrityViolationException e) {
                return false;
            }
        }
        if (existing.get().getStatus() == PaymentAttemptStatus.SUCCEEDED
                || existing.get().getStatus() == PaymentAttemptStatus.PROCESSING) {
            return false;
        }
        return repository.reopenIfFailed(invoiceId) == 1;
    }

    @Override
    @Transactional
    public void markSucceeded(UUID invoiceId, String providerReference) {
        PaymentAttemptEntity entity = repository.findById(invoiceId)
                .orElseThrow(() -> new IllegalStateException("No payment_attempt for " + invoiceId));
        entity.markSucceeded(providerReference);
        repository.save(entity);
    }

    @Override
    @Transactional
    public void markFailed(UUID invoiceId, String reason) {
        PaymentAttemptEntity entity = repository.findById(invoiceId)
                .orElseThrow(() -> new IllegalStateException("No payment_attempt for " + invoiceId));
        entity.markFailed(reason);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentAttemptStatus> findStatus(UUID invoiceId) {
        return repository.findById(invoiceId).map(PaymentAttemptEntity::getStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findProviderReference(UUID invoiceId) {
        return repository.findById(invoiceId)
                .map(PaymentAttemptEntity::getProviderReference)
                .filter(ref -> ref != null && !ref.isBlank());
    }
}