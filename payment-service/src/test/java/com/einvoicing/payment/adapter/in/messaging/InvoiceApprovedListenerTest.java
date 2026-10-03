package com.einvoicing.payment.adapter.in.messaging;

import com.einvoicing.payment.application.service.ProcessApprovedInvoiceService;
import com.einvoicing.payment.domain.event.InvoiceApprovedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvoiceApprovedListenerTest {

    @Mock
    private ProcessApprovedInvoiceService useCase;

    private InvoiceApprovedListener listener;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        listener = new InvoiceApprovedListener(useCase, objectMapper);
    }

    @Test
    void onMessage_validJson_delegatesToUseCase() {
        UUID invoiceId = UUID.randomUUID();
        String json = """
                {
                  "invoiceId": "%s",
                  "invoiceNumber": "INV-1",
                  "totalAmount": 30.00,
                  "currency": "EUR",
                  "mode": "AUTO"
                }
                """.formatted(invoiceId);

        listener.onMessage(json.trim());

        ArgumentCaptor<InvoiceApprovedEvent> captor =
                ArgumentCaptor.forClass(InvoiceApprovedEvent.class);
        verify(useCase).handle(captor.capture());

        InvoiceApprovedEvent event = captor.getValue();
        assertThat(event.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(event.getInvoiceNumber()).isEqualTo("INV-1");
    }

    @Test
    void onMessage_invalidJson_throws() {
        assertThatThrownBy(() -> listener.onMessage("not-json"))
                .isInstanceOf(RuntimeException.class);
    }
}