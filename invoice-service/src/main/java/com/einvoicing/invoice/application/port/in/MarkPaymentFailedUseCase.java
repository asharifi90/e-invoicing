package com.einvoicing.invoice.application.port.in;

import java.util.UUID;

public interface MarkPaymentFailedUseCase {

    void markPaymentFailed(UUID invoiceId, String reason);
}
