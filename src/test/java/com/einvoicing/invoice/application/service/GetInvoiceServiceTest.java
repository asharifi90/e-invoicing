package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.exception.InvoiceNotFoundException;
import com.einvoicing.invoice.domain.model.InvoiceLine;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetInvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private GetInvoiceService service;

    @Test
    void getInvoiceById_shouldReturnInvoice(){

        InvoiceId id = InvoiceId.generate();

        Invoice invoice = Invoice.reconstitute(
                id,
                "INV-1",
                "BE01",
                "BE02",
                List.of(new InvoiceLine("calculating", 1, Money.eur("15.00"))),
                Money.eur("15.00"),
                InvoiceStatus.RECEIVED,
                Instant.now(),
                null
        );

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        Invoice result = service.getById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-1");
    }

    @Test
    void getInvoiceById_shouldThrowException(){
        InvoiceId id = InvoiceId.from(UUID.randomUUID().toString());
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(InvoiceNotFoundException.class);
    }
}
