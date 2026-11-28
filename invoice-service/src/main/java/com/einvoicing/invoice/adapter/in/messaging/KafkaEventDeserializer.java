package com.einvoicing.invoice.adapter.in.messaging;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class KafkaEventDeserializer {

    private final ObjectMapper objectMapper;

    public KafkaEventDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T deserialize(String message, Class<T> targetType) throws Exception {
        String payload = message;
        if (payload != null && payload.startsWith("\"")) {
            payload = objectMapper.readValue(payload, String.class);
        }
        return objectMapper.readValue(payload, targetType);
    }
}