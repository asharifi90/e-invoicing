package com.einvoicing.validation.application.service;

import com.einvoicing.validation.application.port.in.ValidateInvoiceUseCase;
import com.einvoicing.validation.application.port.out.ValidationResultPublisher;
import com.einvoicing.validation.domain.event.InvoiceReceivedEvent;
import com.einvoicing.validation.domain.event.InvoiceRejectedEvent;
import com.einvoicing.validation.domain.event.InvoiceValidatedEvent;
import com.einvoicing.validation.domain.service.InvoiceRulesValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValidationInvoiceService implements ValidateInvoiceUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ValidationInvoiceService.class);

    private final InvoiceRulesValidator invoiceRulesValidator;
    private final ValidationResultPublisher validationResultPublisher;

    public ValidationInvoiceService(InvoiceRulesValidator invoiceRulesValidator, ValidationResultPublisher validationResultPublisher) {
        this.invoiceRulesValidator = invoiceRulesValidator;
        this.validationResultPublisher = validationResultPublisher;
    }

    @Override
    public void validateReceivedEvent(InvoiceReceivedEvent event) {

        logger.info("validating received invoice with invoice number {}", event.getInvoiceNumber());

        List<String> validate = invoiceRulesValidator.validate(event);
        if (!validate.isEmpty()) {
            String reason = String.join(", ", validate);
            validationResultPublisher.publishRejectedEvent(
                    InvoiceRejectedEvent.from(event.getInvoiceNumber(), event.getInvoiceId(), reason));
            logger.info("Invoice rejected invoiceId : {}, reason : {} ", event.getInvoiceId(), reason);
        }
        if (validate.isEmpty()) {
            validationResultPublisher.publishValidatedEvent(
                    InvoiceValidatedEvent.of(event.getInvoiceId(), event.getInvoiceNumber()));
            logger.info("Invoice validated invoice with id : {} ", event.getInvoiceId());
        }

    }
}
