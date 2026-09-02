package com.einvoicing.approval.application.service;

import com.einvoicing.approval.application.port.out.ApprovalResultPublisher;
import com.einvoicing.approval.application.port.out.PendingApprovalStore;
import com.einvoicing.approval.domain.event.InvoiceApprovalRequiredEvent;
import com.einvoicing.approval.domain.event.InvoiceApprovedEvent;
import com.einvoicing.approval.domain.event.InvoiceValidatedEvent;
import com.einvoicing.approval.domain.service.ApprovalPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ApproveInvoiceServiceTest {

    private final ApprovalPolicy approvalPolicy = new ApprovalPolicy();

    @Mock
    private ApprovalResultPublisher approvalResultPublisher;

    @Mock
    private PendingApprovalStore pendingApprovalStore;
    private ApproveInvoiceService approveInvoiceService;

    @BeforeEach
    void setUp() {
        approveInvoiceService = new ApproveInvoiceService(approvalPolicy, approvalResultPublisher, pendingApprovalStore);
    }

    @Test
    void handleValidated_autoApproved() {

        InvoiceValidatedEvent invoiceValidatedEvent = new InvoiceValidatedEvent();
        invoiceValidatedEvent.setTotalAmount(new BigDecimal("100.00"));
        invoiceValidatedEvent.setInvoiceId(UUID.randomUUID());
        invoiceValidatedEvent.setInvoiceNumber("INV_001");

        approveInvoiceService.handleValidated(invoiceValidatedEvent);

        verify(approvalResultPublisher).publishApproved(any(InvoiceApprovedEvent.class));
        verify(pendingApprovalStore, never()).save(any());
    }

    @Test
    void handleValidated_manualApproved_saved() {
        InvoiceValidatedEvent invoiceValidatedEvent = new InvoiceValidatedEvent();
        invoiceValidatedEvent.setTotalAmount(new BigDecimal("1000.00"));
        invoiceValidatedEvent.setInvoiceId(UUID.randomUUID());
        invoiceValidatedEvent.setInvoiceNumber("INV_001");

        approveInvoiceService.handleValidated(invoiceValidatedEvent);

        verify(approvalResultPublisher).publishApprovalRequired(any(InvoiceApprovalRequiredEvent.class));
        verify(pendingApprovalStore).save(any());

    }
}
