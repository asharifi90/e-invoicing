package com.einvoicing.validation.adapter.out.memory;

import com.einvoicing.validation.application.port.out.ProcessedEventStore;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    // Production deployments should replace this process-local store with a durable implementation.
    private final Set<String> claimedEvents = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryClaim(String eventKey) {
        return claimedEvents.add(eventKey);
    }

    @Override
    public void release(String eventKey) {
        claimedEvents.remove(eventKey);
    }
}
