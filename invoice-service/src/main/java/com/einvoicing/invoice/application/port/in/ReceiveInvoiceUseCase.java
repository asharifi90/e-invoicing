package com.einvoicing.invoice.application.port.in;

import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.InvoiceLine;

import java.util.List;

public interface ReceiveInvoiceUseCase {

    Invoice receive(ReceiveInvoiceCommand command);

    default ReceiveResult receive(ReceiveInvoiceCommand command, String idempotencyKey) {
        return new ReceiveResult(receive(command), true);
    }

    record ReceiveResult(Invoice invoice, boolean created) {}

    record ReceiveInvoiceCommand(
      String invoiceNumber,
      String sellerVatNumber,
      String buyerVatNumber,
      List<InvoiceLine> lines
    ){}
}
