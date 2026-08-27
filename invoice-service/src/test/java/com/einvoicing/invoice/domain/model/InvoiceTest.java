package com.einvoicing.invoice.domain.model;

import com.einvoicing.invoice.domain.exception.InvalidInvoiceStateException;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.enums.InvoiceStatus;
import com.einvoicing.invoice.domain.model.valueObject.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

public class InvoiceTest {

    @Test
    void create_shouldCalculateTotal_andSetStatusReceived() {

        InvoiceLine line = new InvoiceLine(
                "consulting",
                10,
                Money.eur("150.00")
        );

        Invoice invoice = Invoice.create(
                "INV-2026-001",
                "BE0123456789",
                "BE9876543210",
                List.of(line)
        );

        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-2026-001");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.RECEIVED);
        assertThat(invoice.getTotalAmount().amount()).isEqualByComparingTo("1500.00");
        assertThat(invoice.getTotalAmount().currency()).isEqualTo("EUR");
        assertThat(invoice.getLines()).hasSize(1);
        assertThat(invoice.getId()).isNotNull();
    }

    @Test
    void create_shouldReject_whenNoLines(){

        assertThatThrownBy(() -> Invoice.create("INV-1", "BE01", "BE02", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invoice must have at least one line");
    }

    @Test
    void markAsValidated_shouldWork_fromReceived(){
        Invoice invoice = Invoice.create(
                "INV-1",
                "BE01",
                "BE02",
                List.of(new InvoiceLine("X", 1, Money.eur("10.00")))
        );

        invoice.markAsValidated();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.VALIDATED);
    }

    @Test
    void markAsValidated_shouldReject_whenNotReceived(){
        Invoice invoice = Invoice.create(
                "INV-1",
                "BE01",
                "BE02",
                List.of(new InvoiceLine("X", 1, Money.eur("10.00")))
        );

        invoice.markAsValidated();

        assertThatThrownBy(invoice::markAsValidated)
        .isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void markAsApproved_shouldWork_fromWaitingApproval(){
        Invoice invoice = Invoice.create(
                "INV-1",
                "BE01",
                "BE02",
                List.of(new InvoiceLine("X", 1, Money.eur("10.00")))
        );

        invoice.markAsValidated();
        invoice.markAsWaitingApproval();
        invoice.markAsApproved();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.APPROVED);
    }

    @Test
    void markAsApproved_shouldReject_whenNotWaitingApproval(){
        Invoice invoice = Invoice.create(
                "INV-1",
                "BE01",
                "BE02",
                List.of(new InvoiceLine("X", 1, Money.eur("10.00")))
        );

        invoice.markAsValidated();
        invoice.markAsWaitingApproval();
        invoice.markAsApproved();

        assertThatThrownBy(invoice::markAsApproved)
                .isInstanceOf(InvalidInvoiceStateException.class);
    }


}
