package com.einvoicing.invoice.adapter.in.web.mapper;

import com.einvoicing.invoice.adapter.in.web.dto.InvoiceResponse;
import com.einvoicing.invoice.adapter.in.web.dto.ReceiveInvoiceRequest;
import com.einvoicing.invoice.application.port.in.ReceiveInvoiceUseCase.ReceiveInvoiceCommand;
import com.einvoicing.invoice.domain.Invoice;
import com.einvoicing.invoice.domain.InvoiceLine;
import com.einvoicing.invoice.domain.Money;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceWebMapper {

    public ReceiveInvoiceCommand toCommand(ReceiveInvoiceRequest request) {
        List<InvoiceLine> lines = request.lines().stream()
                .map(line -> new InvoiceLine(
                        line.description(),
                        line.quantity(),
                        new Money(line.unitPrice(), line.currency())
                ))
                .toList();

        return new ReceiveInvoiceCommand(
                request.invoiceNumber(),
                request.sellerVatNumber(),
                request.buyerVatNumber(),
                lines
        );
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceResponse.InvoiceLineResponse> lines = invoice.getLines().stream()
                .map(line -> new InvoiceResponse.InvoiceLineResponse(
                        line.getDescription(),
                        line.getQuantity(),
                        line.getUnitPrice().amount(),
                        line.getLineTotal().amount(),
                        line.getUnitPrice().currency()
                ))
                .toList();

        return new InvoiceResponse(
                invoice.getId().value(),
                invoice.getInvoiceNumber(),
                invoice.getSellerVatNumber(),
                invoice.getBuyerVatNumber(),
                invoice.getTotalAmount().amount(),
                invoice.getTotalAmount().currency(),
                invoice.getStatus().name(),
                invoice.getReceivedAt(),
                lines
        );
    }
}
