package com.einvoicing.payment.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(name = "topic", nullable = false, length = 255)
    private String topic;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "trace_parent")
    private String traceParent;

    protected OutboxEventEntity() {
    }

    public static OutboxEventEntity create(String topic, String payload, String traceParent) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.id = UUID.randomUUID();
        event.topic = topic;
        event.payload = payload;
        event.createdAt = Instant.now();
        event.traceParent = traceParent;
        return event;
    }

    public String getTraceParent() {
        return traceParent;
    }

    public void setTraceParent(String traceParent) {
        this.traceParent = traceParent;
    }

    public void markPublished() {
        publishedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }
}
