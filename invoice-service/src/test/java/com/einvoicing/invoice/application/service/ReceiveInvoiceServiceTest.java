package com.einvoicing.invoice.application.service;


import com.einvoicing.invoice.application.port.in.ReceiveInvoiceUseCase;
import com.einvoicing.invoice.application.port.out.DomainEventPublisher;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.event.InvoiceReceivedEvent;
import com.einvoicing.invoice.domain.exception.InvoiceAlreadyExistsException;
import com.einvoicing.invoice.domain.model.InvoiceLine;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.valueObject.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceiveInvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private ReceiveInvoiceService receiveInvoiceService;

    @Test
    void receive_shouldSaveInvoice_andPublishEvent() {

        when(invoiceRepository.existsByInvoiceNumber("INV-1")).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        ReceiveInvoiceUseCase.ReceiveInvoiceCommand command = new ReceiveInvoiceUseCase.ReceiveInvoiceCommand(
                "INV-1",
                "BE0123456789",
                "BE9876543210",
                List.of(new InvoiceLine("Consulting", 10, Money.eur("150.00")))
        );

        Invoice result = receiveInvoiceService.receive(command);

        assertThat(result.getInvoiceNumber()).isEqualTo("INV-1");
        assertThat(result.getTotalAmount().amount()).isEqualByComparingTo("1500.00");

        verify(invoiceRepository).save(any(Invoice.class));
        verify(domainEventPublisher).publish(any(InvoiceReceivedEvent.class));
    }

    @Test
    void receive_shouldThrow_whenDuplicateInvoiceNumber() {

        when(invoiceRepository.existsByInvoiceNumber("INV-1")).thenReturn(true);

        ReceiveInvoiceUseCase.ReceiveInvoiceCommand command = new ReceiveInvoiceUseCase.ReceiveInvoiceCommand(
                "INV-1",
                "BE01",
                "BE02",
                List.of(new InvoiceLine("X", 1, Money.eur("10.00")))
        );


        assertThatThrownBy(() -> receiveInvoiceService.receive(command))
                .isInstanceOf(InvoiceAlreadyExistsException.class);

        verify(invoiceRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
    }
}
