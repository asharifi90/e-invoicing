package com.einvoicing.invoice.domain.exception;

public class IdempotencyKeyConflictException extends RuntimeException {
    public IdempotencyKeyConflictException(String key) {
        super("Idempotency key was already used for a different request: " + key);
    }
}
