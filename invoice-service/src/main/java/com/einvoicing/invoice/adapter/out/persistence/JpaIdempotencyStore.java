package com.einvoicing.invoice.adapter.out.persistence;

import com.einvoicing.invoice.adapter.out.persistence.entity.IdempotencyRequestEntity;
import com.einvoicing.invoice.adapter.out.persistence.repository.IdempotencyRequestJpaRepository;
import com.einvoicing.invoice.application.port.out.IdempotencyStore;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaIdempotencyStore implements IdempotencyStore {

    private final IdempotencyRequestJpaRepository repository;

    public JpaIdempotencyStore(IdempotencyRequestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<IdempotencyRequest> findByKey(String key) {
        return repository.findById(key)
                .map(entity -> new IdempotencyRequest(
                        entity.getRequestHash(), new InvoiceId(entity.getInvoiceId())));
    }

    @Override
    public void save(String key, String requestHash, InvoiceId invoiceId) {
        repository.save(IdempotencyRequestEntity.create(key, requestHash, invoiceId.value()));
    }
}
