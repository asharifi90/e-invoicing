package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.in.GetInvoiceUseCase;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.exception.InvoiceNotFoundException;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetInvoiceService implements GetInvoiceUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetInvoiceService.class);

    private final InvoiceRepository invoiceRepository;

    public GetInvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getById(InvoiceId id) {
        log.info("Fetching invoice with id={}", id.value());

        return invoiceRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Invoice not found with id={}", id.value());
                    return new InvoiceNotFoundException(id.value().toString());
                });
    }
}