package com.einvoicing.invoice.adapter.out.persistence.repository;

import com.einvoicing.invoice.adapter.out.persistence.entity.IdempotencyRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRequestJpaRepository extends JpaRepository<IdempotencyRequestEntity, String> {
}
