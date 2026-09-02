package com.einvoicing.approval.application.service;

import com.einvoicing.approval.application.port.out.ApprovalResultPublisher;
import com.einvoicing.approval.application.port.out.PendingApprovalStore;
import com.einvoicing.approval.domain.event.InvoiceApprovedEvent;
import com.einvoicing.approval.domain.model.PendingApproval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ManualApprovalServiceTest {

    @InjectMocks
    private ManualApprovalService manualApprovalService;

    @Mock
    private PendingApprovalStore pendingApprovalStore;

    @Mock
    private ApprovalResultPublisher approvalResultPublisher;

    @Test
    void approveManual_noPendingApproval() {
        UUID id = UUID.randomUUID();
        when(pendingApprovalStore.findByInvoiceId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manualApprovalService.approveManual(id, "demo-user")).isInstanceOf(
                IllegalArgumentException.class
        );

        verify(approvalResultPublisher, never()).publishApproved(any());
    }

    @Test
    void approvalManual_approved() {
        UUID id = UUID.randomUUID();
        PendingApproval pendingApproval = new PendingApproval(id, "INV-001", new BigDecimal("1000.00"), Instant.now());
        when(pendingApprovalStore.findByInvoiceId(id)).thenReturn(Optional.of(pendingApproval));
        manualApprovalService.approveManual(id, "demo-user");

        verify(approvalResultPublisher).publishApproved(any(InvoiceApprovedEvent.class));
        verify(pendingApprovalStore).delete(id);
    }

}
