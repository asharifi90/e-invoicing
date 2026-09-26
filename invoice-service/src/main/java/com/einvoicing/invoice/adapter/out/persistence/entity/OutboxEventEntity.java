package com.einvoicing.invoice.adapter.out.persistence.entity;

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

    @Column(nullable = false, name = "aggregate_type")
    private String aggregatedType;

    @Column(nullable = false, name = "aggregate_id")
    private String aggregatedId;

    @Column(nullable = false, name = "event_type")
    private String eventType;

    @Column(name = "payload")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static OutboxEventEntity create(
            String aggregatedType, String aggregatedId, String eventType, String payload
    ){
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.id = UUID.randomUUID();
        entity.aggregatedType = aggregatedType;
        entity.aggregatedId = aggregatedId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.createdAt = Instant.now();
        return entity;
    }

    public void setPublishedAt() {
        this.publishedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getAggregatedType() {
        return aggregatedType;
    }

    public String getAggregatedId() {
        return aggregatedId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
