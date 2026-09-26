package com.einvoicing.invoice.adapter.out.persistence.repository;

import com.einvoicing.invoice.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;


public interface OutboxInvoiceRepository extends JpaRepository<OutboxEventEntity, UUID> {


    @Query("""
            SELECT e FROM OutboxEventEntity e
            Where e.publishedAt is null
            Order by e.createdAt ASC""")
    List<OutboxEventEntity> findPending(Pageable pageable);
}
