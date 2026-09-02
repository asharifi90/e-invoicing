package com.einvoicing.approval.application.service;

import com.einvoicing.approval.application.port.in.ManualApprovalUseCase;
import com.einvoicing.approval.application.port.out.ApprovalResultPublisher;
import com.einvoicing.approval.application.port.out.PendingApprovalStore;
import com.einvoicing.approval.domain.event.InvoiceApprovedEvent;
import com.einvoicing.approval.domain.model.PendingApproval;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ManualApprovalService implements ManualApprovalUseCase {

    private final PendingApprovalStore store;
    private final ApprovalResultPublisher publisher;

    public ManualApprovalService(PendingApprovalStore store,
                                 ApprovalResultPublisher publisher) {
        this.store = store;
        this.publisher = publisher;
    }

    @Override
    public void approveManual(UUID invoiceId, String approvedBy) {

        PendingApproval pendingApproval = store.findByInvoiceId(invoiceId).
                orElseThrow(() -> new IllegalArgumentException("No pending approval found for invoice " + invoiceId));
        publisher.publishApproved(
                InvoiceApprovedEvent.manual(pendingApproval.invoiceId(), pendingApproval.invoiceNumber(), approvedBy)
        );
        store.delete(invoiceId);
    }
}
