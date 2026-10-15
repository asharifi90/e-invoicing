package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.enums.InvoiceStatus;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;
import com.einvoicing.invoice.domain.model.valueObject.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkPaymentSucceededServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private MarkPaymentSucceededService service;

    @Test
    void markPaid_shouldMarkInvoicePaidAndSaveIt() {
        UUID invoiceUuid = UUID.randomUUID();
        InvoiceId invoiceId = new InvoiceId(invoiceUuid);
        Invoice invoice = invoice(invoiceId, InvoiceStatus.APPROVED);
        String providerReference = "payment-123";
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        service.markPaid(invoiceUuid, providerReference);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(invoiceRepository).findById(invoiceId);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markPaid_shouldThrowAndNotSaveWhenInvoiceDoesNotExist() {
        UUID invoiceUuid = UUID.randomUUID();
        InvoiceId invoiceId = new InvoiceId(invoiceUuid);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaid(invoiceUuid, "payment-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invoice not found: " + invoiceUuid);

        verify(invoiceRepository).findById(invoiceId);
        org.mockito.Mockito.verifyNoMoreInteractions(invoiceRepository);
    }

    private Invoice invoice(InvoiceId id, InvoiceStatus status) {
        return Invoice.reconstitute(
                id,
                "INV-1",
                "BE0123456789",
                "BE9876543210",
                List.of(),
                Money.eur("10.00"),
                status,
                Instant.now(),
                null
        );
    }
}
