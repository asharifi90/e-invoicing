package com.einvoicing.approval.application.port.out;

import com.einvoicing.approval.domain.model.PendingApproval;

import java.util.Optional;
import java.util.UUID;

public interface PendingApprovalStore {

    void save(PendingApproval approval);
    Optional<PendingApproval> findByInvoiceId(UUID id);
    void delete(UUID invoiceId);
}
