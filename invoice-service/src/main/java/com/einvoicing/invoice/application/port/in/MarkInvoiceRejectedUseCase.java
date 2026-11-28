package com.einvoicing.invoice.application.port.in;

import java.util.UUID;

public interface MarkInvoiceRejectedUseCase {
    void markRejected(UUID invoiceId, String reason);
}
