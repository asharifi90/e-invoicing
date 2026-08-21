package com.einvoicing.invoice.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }

        amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        currency = currency.toUpperCase();
    }

    public static Money eur(BigDecimal amount) {
        return new Money(amount, "EUR");
    }

    public static Money eur(String amount) {
        return eur(new BigDecimal(amount));
    }

    public Money add(Money other){
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("can not add different currency");
        }

        return new Money(this.amount.add(other.amount), currency);
    }

    public Money multiply(int quantity){
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}
