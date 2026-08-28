package com.einvoicing.validation.domain.service;

import com.einvoicing.validation.domain.event.InvoiceReceivedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class InvoiceRulesValidator {

    public List<String> validate(InvoiceReceivedEvent event) {
        List<String> errors = new ArrayList<>();

        if (event.getInvoiceNumber() == null || event.getInvoiceNumber().isBlank()) {
            errors.add("Invoice number is required");
        }
        if (event.getTotalAmount() == null || event.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Total amount must be greater than zero");
        }
        if (event.getCurrency() == null || event.getCurrency().isBlank()) {
            errors.add("Currency is required");
        } else if (!event.getCurrency().equalsIgnoreCase("EUR")) {
            errors.add("Only EUR is supported");
        }
        if (event.getInvoiceId() == null) {
            errors.add("Invoice id is required");
        }
        return errors;
    }
}
