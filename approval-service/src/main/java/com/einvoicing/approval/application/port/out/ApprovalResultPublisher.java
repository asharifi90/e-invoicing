package com.einvoicing.approval.application.port.out;

import com.einvoicing.approval.domain.event.InvoiceApprovalRequiredEvent;
import com.einvoicing.approval.domain.event.InvoiceApprovedEvent;

public interface ApprovalResultPublisher {
    void publishApproved(InvoiceApprovedEvent event);
    void publishApprovalRequired(InvoiceApprovalRequiredEvent event);
}
