package com.einvoicing.invoice.adapter.out.persistence.repository;

import com.einvoicing.invoice.adapter.out.persistence.entity.InvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    boolean existsByInvoiceNumber(String invoiceNumber);
}
