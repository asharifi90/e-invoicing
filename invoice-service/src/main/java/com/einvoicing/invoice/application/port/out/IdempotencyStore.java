package com.einvoicing.invoice.application.port.out;

import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;

import java.util.Optional;

public interface IdempotencyStore {

    Optional<IdempotencyRequest> findByKey(String key);

    void save(String key, String requestHash, InvoiceId invoiceId);

    record IdempotencyRequest(String requestHash, InvoiceId invoiceId) {}
}
