package com.einvoicing.approval.application.port.out;

public interface ProcessedEventStore {
    boolean tryClaim(String eventKey);

    void release(String eventKey);
}
