package com.einvoicing.invoice.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record ReceiveInvoiceRequest(
        @NotBlank String invoiceNumber,
        @NotBlank String sellerVatNumber,
        @NotBlank String buyerVatNumber,
        @NotEmpty @Valid List<InvoiceLineRequest> lines
) {
    public record InvoiceLineRequest(
            @NotBlank String description,
            @Positive int quantity,
            @NotNull BigDecimal unitPrice,
            @NotBlank String currency
    ) {}
}
