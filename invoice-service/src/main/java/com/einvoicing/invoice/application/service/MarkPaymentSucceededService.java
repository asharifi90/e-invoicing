package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.in.MarkPaymentSucceededUseCase;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MarkPaymentSucceededService implements MarkPaymentSucceededUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkPaymentSucceededService.class);

    private final InvoiceRepository invoiceRepository;

    public MarkPaymentSucceededService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional
    public void markPaid(UUID invoiceId, String providerReference) {
        Invoice invoice = invoiceRepository.findById(new InvoiceId(invoiceId))
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        invoice.markPaid(providerReference);
        invoiceRepository.save(invoice);

        log.info("Invoice {} marked PAID ref={}", invoiceId, providerReference);
    }
}