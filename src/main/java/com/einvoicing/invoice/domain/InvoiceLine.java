package com.einvoicing.invoice.domain;

import java.util.Objects;

public class InvoiceLine {

    private final String description;
    private final int quantity;
    private final Money unitPrice;
    private final Money lineTotal;

    public InvoiceLine(String description, int quantity, Money unitPrice) {

        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("description is required");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }

        Objects.requireNonNull(unitPrice, "unitPrice is  required");
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(quantity);
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getLineTotal() {
        return lineTotal;
    }
}
