package com.einvoicing.approval.adapter.in.messaging;

import com.einvoicing.approval.application.port.in.ApproveInvoiceUseCase;
import com.einvoicing.approval.adapter.out.memory.InMemoryProcessedEventStore;
import com.einvoicing.approval.application.port.out.ApprovalResultPublisher;
import com.einvoicing.approval.application.port.out.PendingApprovalStore;
import com.einvoicing.approval.application.service.ApproveInvoiceService;
import com.einvoicing.approval.domain.service.ApprovalPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidatedInvoiceListenerTest {

    @Mock
    private ApproveInvoiceUseCase approveInvoiceUseCase;

    @Test
    void onMessage_rethrowsProcessingFailure() {
        RuntimeException failure = new RuntimeException("approval failed");
        doThrow(failure).when(approveInvoiceUseCase).handleValidated(any());
        ValidatedInvoiceListener listener =
                new ValidatedInvoiceListener(
                        new ObjectMapper(), approveInvoiceUseCase, new InMemoryProcessedEventStore());
        String message = """
                {
                  "invoiceId":"87a857ee-4182-4cd1-92a5-6cf43320b559",
                  "invoiceNumber":"INV-2026-001",
                  "totalAmount":1500.00,
                  "currency":"EUR",
                  "createdAt":"2026-08-29T15:59:43.418038Z",
                  "eventId":"f960d20d-34b7-42bf-b836-9a8fb66f2079"
                }
                """;

        assertThatThrownBy(() -> listener.onMessage(message))
                .isSameAs(failure);
    }

    @Test
    void onMessage_rethrowsParseFailure() {
        ValidatedInvoiceListener listener =
                new ValidatedInvoiceListener(
                        new ObjectMapper(), approveInvoiceUseCase, new InMemoryProcessedEventStore());

        assertThatThrownBy(() -> listener.onMessage("not-json"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void onMessage_duplicateInvoice_doesNotApplyPolicyOrPublishAgain() {
        ApprovalPolicy policy = mock(ApprovalPolicy.class);
        ApprovalResultPublisher publisher = mock(ApprovalResultPublisher.class);
        PendingApprovalStore pendingApprovalStore = mock(PendingApprovalStore.class);
        when(policy.canAutoApprove(any())).thenReturn(true);
        ApproveInvoiceService service = new ApproveInvoiceService(policy, publisher, pendingApprovalStore);
        ValidatedInvoiceListener listener = new ValidatedInvoiceListener(
                new ObjectMapper(), service, new InMemoryProcessedEventStore());
        String message = """
                {
                  "invoiceId":"87a857ee-4182-4cd1-92a5-6cf43320b559",
                  "invoiceNumber":"INV-2026-001",
                  "totalAmount":1500.00,
                  "currency":"EUR",
                  "createdAt":"2026-08-29T15:59:43.418038Z",
                  "eventId":"f960d20d-34b7-42bf-b836-9a8fb66f2079"
                }
                """;

        listener.onMessage(message);
        listener.onMessage(message);

        verify(policy, times(1)).canAutoApprove(new BigDecimal("1500.00"));
        verify(publisher, times(1)).publishApproved(any());
        verify(publisher, never()).publishApprovalRequired(any());
    }
}
