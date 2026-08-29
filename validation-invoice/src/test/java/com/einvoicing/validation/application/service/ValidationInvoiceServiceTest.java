package com.einvoicing.validation.application.service;

import com.einvoicing.validation.application.port.out.ValidationResultPublisher;
import com.einvoicing.validation.domain.event.InvoiceReceivedEvent;
import com.einvoicing.validation.domain.event.InvoiceRejectedEvent;
import com.einvoicing.validation.domain.event.InvoiceValidatedEvent;
import com.einvoicing.validation.domain.service.InvoiceRulesValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ValidationInvoiceServiceTest {


    private final InvoiceRulesValidator rulesValidator = new  InvoiceRulesValidator();
    private ValidationInvoiceService service;

    @Mock
    private ValidationResultPublisher validationResultPublisher;

    @BeforeEach
    void setUp() {
        service = new ValidationInvoiceService(rulesValidator,  validationResultPublisher);
    }


    @Test
    void validate_invoiceReceivedEvent_valid() {

        InvoiceReceivedEvent event = new InvoiceReceivedEvent();
        event.setEventId(UUID.randomUUID());
        event.setInvoiceId(UUID.randomUUID());
        event.setInvoiceNumber("INV-1");
        event.setCurrency("EUR");
        event.setTotalAmount(new BigDecimal("100.00"));

        service.validateReceivedEvent(event);

        verify(validationResultPublisher).publishValidatedEvent(any(InvoiceValidatedEvent.class));
    }

    @Test
    void validate_invoiceReceivedEvent_invalid_noInvoiceNumber() {
        InvoiceReceivedEvent event = new InvoiceReceivedEvent();
        event.setEventId(UUID.randomUUID());
        event.setInvoiceId(UUID.randomUUID());
        event.setCurrency("EUR");
        event.setTotalAmount(new BigDecimal("100.00"));

        service.validateReceivedEvent(event);

        verify(validationResultPublisher).publishRejectedEvent(any(InvoiceRejectedEvent.class));
    }

    @Test
    void validate_invoiceReceivedEvent_invalid_wrongCurrency() {
        InvoiceReceivedEvent event = new InvoiceReceivedEvent();
        event.setEventId(UUID.randomUUID());
        event.setInvoiceId(UUID.randomUUID());
        event.setInvoiceNumber("INV-1");
        event.setCurrency("DOLLAR");
        event.setTotalAmount(new BigDecimal("100.00"));

        service.validateReceivedEvent(event);

        verify(validationResultPublisher).publishRejectedEvent(any(InvoiceRejectedEvent.class));
    }

    @Test
    void validate_invoiceReceivedEvent_invalid_amountIsZero() {
        InvoiceReceivedEvent event = new InvoiceReceivedEvent();
        event.setEventId(UUID.randomUUID());
        event.setInvoiceId(UUID.randomUUID());
        event.setInvoiceNumber("INV-1");
        event.setCurrency("EUR");
        event.setTotalAmount(new BigDecimal("0.00"));

        service.validateReceivedEvent(event);

        verify(validationResultPublisher).publishRejectedEvent(any(InvoiceRejectedEvent.class));
        verify(validationResultPublisher, never()).publishValidatedEvent(any(InvoiceValidatedEvent.class));
    }


}
