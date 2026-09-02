package com.einvoicing.approval.adapter.out.memory;

import com.einvoicing.approval.application.port.out.PendingApprovalStore;
import com.einvoicing.approval.domain.model.PendingApproval;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPendingApprovalStore implements PendingApprovalStore {

    private final ConcurrentHashMap<UUID, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();

    @Override
    public void save(PendingApproval approval) {
        pendingApprovals.put(approval.invoiceId(), approval);
    }

    @Override
    public Optional<PendingApproval> findByInvoiceId(UUID invoiceId) {
        return Optional.ofNullable(pendingApprovals.get(invoiceId));
    }

    @Override
    public void delete(UUID invoiceId) {
        pendingApprovals.remove(invoiceId);
    }
}
