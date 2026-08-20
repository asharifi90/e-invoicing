package com.einvoicing.invoice.application.port.in;

import com.einvoicing.invoice.domain.Invoice;
import com.einvoicing.invoice.domain.InvoiceLine;

import java.util.List;

public interface ReceiveInvoiceUseCase {

    Invoice receive(ReceiveInvoiceCommand command);

    record ReceiveInvoiceCommand(
      String invoiceNumber,
      String sellerVatNumber,
      String buyerVatNumber,
      List<InvoiceLine> lines
    ){}
}
