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
    @Query("""
        update PaymentAttemptEntity p
           set p.status = com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus.PROCESSING,
               p.providerReference = null,
               p.failureReason = null,
               p.updatedAt = CURRENT_TIMESTAMP
         where p.invoiceId = :invoiceId
           and p.status = com.einvoicing.payment.application.port.out.dto.PaymentAttemptStatus.FAILED
        """)
    int reopenIfFailed(@Param("invoiceId") UUID invoiceId);
}
