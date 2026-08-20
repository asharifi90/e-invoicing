package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.in.ReceiveInvoiceUseCase;
import com.einvoicing.invoice.application.port.out.DomainEventPublisher;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.Invoice;
import com.einvoicing.invoice.event.InvoiceReceivedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiveInvoiceService implements ReceiveInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final DomainEventPublisher eventPublisher;

    public ReceiveInvoiceService(InvoiceRepository invoiceRepository, DomainEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Invoice receive(ReceiveInvoiceCommand command) {

        if (invoiceRepository.existsByInvoiceNumber(command.invoiceNumber())){
            throw new IllegalArgumentException("Invoice with number " + command.invoiceNumber() + " already exists");
        }

        Invoice invoice = Invoice.create(
                command.invoiceNumber(),
                command.sellerVatNumber(),
                command.buyerVatNumber(),
                command.lines()
        );

        Invoice saved = invoiceRepository.save(invoice);

        eventPublisher.publish(new InvoiceReceivedEvent(
                saved.getId().value(),
                saved.getInvoiceNumber(),
                saved.getTotalAmount().value(),
                saved.getTotalAmount().currency()
        ));

        return saved;
    }
}
