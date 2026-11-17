package com.example.outbox.event;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class OutboxEvent {
    @Id
    private UUID id;
    private String aggregateId;
    private String eventType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    private boolean published = false;

    protected OutboxEvent() { }

    public OutboxEvent(UUID id, String aggregateId, String eventType, String payload) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public UUID getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
