package com.einvoicing.validation.adapter.in.messaging;

import com.einvoicing.validation.application.port.in.ValidateInvoiceUseCase;
import com.einvoicing.validation.domain.event.InvoiceReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceReceivedListenerTest {

    @Mock
    private ValidateInvoiceUseCase validateInvoiceUseCase;

    private ObjectMapper objectMapper;
    private InvoiceReceivedListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new InvoiceReceivedListener(validateInvoiceUseCase, objectMapper);
    }

    @Test
    void onMessage_parsesJson_and_callsUseCase() {
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

        ArgumentCaptor<InvoiceReceivedEvent> captor =
                ArgumentCaptor.forClass(InvoiceReceivedEvent.class);
        verify(validateInvoiceUseCase).validateReceivedEvent(captor.capture());

        InvoiceReceivedEvent event = captor.getValue();
        assertThat(event.getInvoiceNumber()).isEqualTo("INV-2026-001");
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getTotalAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void onMessage_invalidJson_throws() {
        String bad = "not-json";

        assertThatThrownBy(() -> listener.onMessage(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid message");

        verify(validateInvoiceUseCase, never()).validateReceivedEvent(any());
    }
}