package com.einvoicing.payment.application.port.out;

public interface OutboxWriter {
    void enqueue(String topic, Object event);
}
