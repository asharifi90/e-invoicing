package com.einvoicing.approval.application.service;

import com.einvoicing.approval.application.port.in.ApproveInvoiceUseCase;
import com.einvoicing.approval.application.port.out.ApprovalResultPublisher;
import com.einvoicing.approval.application.port.out.PendingApprovalStore;
import com.einvoicing.approval.domain.event.InvoiceApprovalRequiredEvent;
import com.einvoicing.approval.domain.event.InvoiceApprovedEvent;
import com.einvoicing.approval.domain.event.InvoiceValidatedEvent;
import com.einvoicing.approval.domain.model.PendingApproval;
import com.einvoicing.approval.domain.service.ApprovalPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ApproveInvoiceService implements ApproveInvoiceUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApproveInvoiceService.class);

    private final ApprovalPolicy policy;
    private final ApprovalResultPublisher publisher;
    private final PendingApprovalStore pendingApprovalStore;

    public ApproveInvoiceService(ApprovalPolicy policy,
                                 ApprovalResultPublisher publisher,
                                 PendingApprovalStore pendingApprovalStore) {
        this.policy = policy;
        this.publisher = publisher;
        this.pendingApprovalStore = pendingApprovalStore;
    }

    @Override
    public void handleValidated(InvoiceValidatedEvent event) {
        if (policy.canAutoApprove(event.getTotalAmount())) {
            publisher.publishApproved(
                    InvoiceApprovedEvent.auto(event.getInvoiceId(), event.getInvoiceNumber()));
            log.info("Auto approved InvoiceValidatedEvent and published with InvoiceNumber: {}", event.getInvoiceNumber());
        } else {
            pendingApprovalStore.save(new PendingApproval(event.getInvoiceId(), event.getInvoiceNumber(),
                    event.getTotalAmount(), Instant.now()));
            publisher.publishApprovalRequired(
                    InvoiceApprovalRequiredEvent.of(
                            event.getInvoiceId(),
                            event.getInvoiceNumber(),
                            event.getTotalAmount()));
            log.info("InvoiceValidatedEvent require approval, published with InvoiceNumber: {}", event.getInvoiceNumber());
        }
    }
}

