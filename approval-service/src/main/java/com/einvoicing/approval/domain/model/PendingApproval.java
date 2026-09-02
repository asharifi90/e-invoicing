package com.einvoicing.approval.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingApproval(
        UUID invoiceId,
        String invoiceNumber,
        BigDecimal totalAmount,
        Instant requestedAt
) {
}
