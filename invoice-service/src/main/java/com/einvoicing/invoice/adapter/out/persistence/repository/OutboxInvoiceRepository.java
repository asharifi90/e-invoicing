package com.einvoicing.invoice.adapter.out.persistence.repository;

import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.UUID;


public interface OutboxInvoiceRepository extends JpaRepository<OutboxEventEntity, UUID> {

    long countByPublishedAtIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT e FROM OutboxEventEntity e
            WHERE e.publishedAt IS NULL
            ORDER BY e.createdAt ASC""")
    List<OutboxEventEntity> findPending(Pageable pageable);
}
