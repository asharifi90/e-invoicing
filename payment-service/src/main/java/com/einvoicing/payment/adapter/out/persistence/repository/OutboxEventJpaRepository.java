package com.einvoicing.payment.adapter.out.persistence.repository;

import com.einvoicing.payment.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("""
            SELECT event FROM OutboxEventEntity event
            WHERE event.publishedAt IS NULL
            ORDER BY event.createdAt ASC
            """)
    List<OutboxEventEntity> findPending(Pageable pageable);
}
