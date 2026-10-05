package com.einvoicing.invoice.application.port.in;

import java.util.UUID;

public interface MarkPaymentSucceededUseCase {
    void markPaid(UUID invoiceId, String providerReference);
}
