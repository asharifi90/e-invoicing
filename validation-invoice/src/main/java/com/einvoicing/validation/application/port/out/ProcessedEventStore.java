package com.einvoicing.validation.application.port.out;

public interface ProcessedEventStore {
    boolean tryClaim(String eventKey);

    void release(String eventKey);
}
