package com.einvoicing.invoice.application.port.out;

public interface OutboxWriter {

    void enqueue(String aggregateType, String aggregateId, String eventType, Object event);
}
