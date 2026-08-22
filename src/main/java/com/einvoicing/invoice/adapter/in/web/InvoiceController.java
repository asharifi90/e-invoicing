package com.einvoicing.invoice.adapter.in.web;

import com.einvoicing.invoice.adapter.in.web.dto.InvoiceResponse;
import com.einvoicing.invoice.adapter.in.web.dto.ReceiveInvoiceRequest;
import com.einvoicing.invoice.adapter.in.web.mapper.InvoiceWebMapper;
import com.einvoicing.invoice.application.port.in.ReceiveInvoiceUseCase;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final ReceiveInvoiceUseCase receiveInvoiceUseCase;
    private final InvoiceWebMapper mapper;

    public InvoiceController(ReceiveInvoiceUseCase receiveInvoiceUseCase,
                             InvoiceWebMapper mapper) {
        this.receiveInvoiceUseCase = receiveInvoiceUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> receiveInvoice(
            @Valid @RequestBody ReceiveInvoiceRequest request) {

        Invoice invoice = receiveInvoiceUseCase.receive(mapper.toCommand(request));
        InvoiceResponse response = mapper.toResponse(invoice);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
