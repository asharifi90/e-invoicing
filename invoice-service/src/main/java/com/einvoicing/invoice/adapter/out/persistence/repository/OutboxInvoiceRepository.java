package com.einvoicing.invoice.adapter.out.persistence.repository;

import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public interface OutboxInvoiceRepository extends JpaRepository<OutboxEventEntity, UUID> {

    long countByPublishedAtIsNull();

    @Query(value = """
            WITH claimed AS (
                SELECT id
                FROM outbox_events
                WHERE published_at IS NULL
                  AND (locked_at IS NULL OR locked_at < NOW() - INTERVAL '2 minutes')
                ORDER BY created_at ASC
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_events o
            SET locked_at = NOW()
            FROM claimed
            WHERE o.id = claimed.id
            RETURNING o.*
            """, nativeQuery = true)
    List<OutboxEventEntity> claimPending(@Param("batchSize") int batchSize);

    @Modifying
    @Query("""
        UPDATE OutboxEventEntity e
        SET e.publishedAt = :now
        WHERE e.id IN :ids
        """)
    int markPublished(@Param("ids") List<UUID> ids, @Param("now") Instant now);
}
