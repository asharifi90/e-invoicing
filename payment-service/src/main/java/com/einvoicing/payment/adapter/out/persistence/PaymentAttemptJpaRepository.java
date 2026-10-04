package com.einvoicing.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptEntity, UUID> {

    /**
     * Atomic: only FAILED → PROCESSING wins the right to charge again.
     * @return number of rows updated (0 or 1)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE payment_attempt
       SET status = 'PROCESSING',
           provider_reference = NULL,
           failure_reason = NULL,
           updated_at = NOW()
     WHERE invoice_id = :invoiceId
       AND status = 'FAILED'
    """, nativeQuery = true)
    int reopenIfFailed(@Param("invoiceId") UUID invoiceId);
}
