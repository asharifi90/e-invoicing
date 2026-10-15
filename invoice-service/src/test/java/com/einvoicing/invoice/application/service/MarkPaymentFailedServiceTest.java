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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;



@ExtendWith(MockitoExtension.class)
class MarkPaymentFailedServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private MarkPaymentFailedService service;

    @Test
    void markPaymentFailed_shouldMarkFailedAndSave() {
        UUID invoiceUuid = UUID.randomUUID();
        InvoiceId invoiceId = new InvoiceId(invoiceUuid);
        Invoice invoice = invoice(invoiceId, InvoiceStatus.APPROVED);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        service.markPaymentFailed(invoiceUuid, "provider down");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAYMENT_FAILED);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markPaymentFailed_whenReasonNull_usesUnknown() {
        UUID invoiceUuid = UUID.randomUUID();
        InvoiceId invoiceId = new InvoiceId(invoiceUuid);
        Invoice invoice = invoice(invoiceId, InvoiceStatus.APPROVED);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        service.markPaymentFailed(invoiceUuid, null);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAYMENT_FAILED);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markPaymentFailed_whenMissing_throwsAndDoesNotSave() {
        UUID invoiceUuid = UUID.randomUUID();
        InvoiceId invoiceId = new InvoiceId(invoiceUuid);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaymentFailed(invoiceUuid, "x"))
                .isInstanceOf(IllegalArgumentException.class);

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
