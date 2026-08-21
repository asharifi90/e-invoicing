package com.einvoicing.invoice.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        String sellerVatNumber,
        String buyerVatNumber,
        BigDecimal totalAmount,
        String currency,
        String status,
        Instant receivedAt,
        List<InvoiceLineResponse> lines
) {
    public record InvoiceLineResponse(
            String description,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            String currency
    ) {}
}
