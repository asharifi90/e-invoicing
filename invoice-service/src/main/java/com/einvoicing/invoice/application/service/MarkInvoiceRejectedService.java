package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.in.MarkInvoiceRejectedUseCase;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.enums.InvoiceStatus;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
public class MarkInvoiceRejectedService implements MarkInvoiceRejectedUseCase {

    private final InvoiceRepository invoiceRepository;
    private final TransactionTemplate transactionTemplate;
    private final InvoiceMetricsRecorder metricsRecorder;

    public MarkInvoiceRejectedService(
            InvoiceRepository invoiceRepository,
            TransactionTemplate transactionTemplate,
            InvoiceMetricsRecorder metricsRecorder) {
        this.invoiceRepository = invoiceRepository;
        this.transactionTemplate = transactionTemplate;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public void markRejected(UUID invoiceId, String reason) {
        Instant completedAt = Instant.now();

        Invoice transitioned = transactionTemplate.execute(status -> {
            Invoice invoice = invoiceRepository.findById(new InvoiceId(invoiceId))
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

            InvoiceStatus previousStatus = invoice.getStatus();
            invoice.markAsRejected(reason);

            if (previousStatus == invoice.getStatus()) {
                return null;
            }
            return invoiceRepository.save(invoice);
        });

        metricsRecorder.recordE2eDuration(transitioned, completedAt, "REJECTED");
    }
}