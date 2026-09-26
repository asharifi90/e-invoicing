package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.in.ReceiveInvoiceUseCase;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.application.port.out.OutboxWriter;
import com.einvoicing.invoice.domain.exception.InvoiceAlreadyExistsException;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiveInvoiceService implements ReceiveInvoiceUseCase {

    private static final Logger log =  LoggerFactory.getLogger(ReceiveInvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final OutboxWriter outboxWriter;



    public ReceiveInvoiceService(InvoiceRepository invoiceRepository, OutboxWriter outboxWriter) {
        this.invoiceRepository = invoiceRepository;
        this.outboxWriter = outboxWriter;
    }

    /**
     * Persists the invoice and enqueues InvoiceReceivedEvent via the transactional outbox.
     * Kafka publish is done asynchronously by OutboxPublisherJob (reliable delivery).
     * Previously: domainEventPublisher.publish(...) right after save (dual-write risk).
     */
    @Override
    @Transactional
    public Invoice receive(ReceiveInvoiceCommand command) {

        log.info("Receiving invoice with number={}", command.invoiceNumber());

        if (invoiceRepository.existsByInvoiceNumber(command.invoiceNumber())){
            log.warn("Duplicate invoice attempt: {}", command.invoiceNumber());
            throw new InvoiceAlreadyExistsException("Invoice with number " + command.invoiceNumber() + " already exists");
        }

        Invoice invoice = Invoice.create(
                command.invoiceNumber(),
                command.sellerVatNumber(),
                command.buyerVatNumber(),
                command.lines()
        );

        Invoice saved = invoiceRepository.save(invoice);

        log.info("Invoice saved successfully. id={}, number={}",
                saved.getId().value(), saved.getInvoiceNumber());

        outboxWriter.enqueue(
                "Invoice",
                invoice.getId().toString(),
                "InvoiceReceived",
                invoice
        );

        log.info("InvoiceReceivedEvent published for invoiceId={}", saved.getId().value());

        return saved;
    }
}
