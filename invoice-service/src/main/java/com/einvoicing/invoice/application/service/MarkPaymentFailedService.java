package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.in.MarkPaymentFailedUseCase;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MarkPaymentFailedService implements MarkPaymentFailedUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkPaymentFailedService.class);

    private final InvoiceRepository invoiceRepository;

    public MarkPaymentFailedService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional
    public void markPaymentFailed(UUID invoiceId, String reason) {

        Invoice invoice = invoiceRepository.findById(new InvoiceId(invoiceId)).orElseThrow(
                () -> new IllegalArgumentException("Invoice not found" +  invoiceId)
        );

        invoice.markPaymentFailed(reason != null ? "payment failed : " + reason : "Unknown");
        invoiceRepository.save(invoice);

        log.info("invoice {} marked payment failed with reason {}", invoiceId, reason);
    }
}
