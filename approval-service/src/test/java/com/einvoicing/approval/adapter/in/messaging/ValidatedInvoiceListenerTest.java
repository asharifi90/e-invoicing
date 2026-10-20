package com.einvoicing.approval.adapter.in.messaging;

import com.einvoicing.approval.application.port.in.ApproveInvoiceUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ValidatedInvoiceListenerTest {

    @Mock
    private ApproveInvoiceUseCase approveInvoiceUseCase;

    @Test
    void onMessage_rethrowsProcessingFailure() {
        RuntimeException failure = new RuntimeException("approval failed");
        doThrow(failure).when(approveInvoiceUseCase).handleValidated(any());
        ValidatedInvoiceListener listener =
                new ValidatedInvoiceListener(new ObjectMapper(), approveInvoiceUseCase);
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
                new ValidatedInvoiceListener(new ObjectMapper(), approveInvoiceUseCase);

        assertThatThrownBy(() -> listener.onMessage("not-json"))
                .isInstanceOf(Exception.class);
    }
}
