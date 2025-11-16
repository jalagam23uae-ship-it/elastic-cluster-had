package com.dynamic.xsd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WebSocket event broadcast message for pub/sub pattern.
 * Sent to all subscribers when data changes occur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketEvent {

    /**
     * Event type: CREATED, UPDATED, DELETED, CUSTOM
     */
    private String event;

    /**
     * Service name that triggered the event
     */
    private String service;

    /**
     * Entity type (e.g., "customer", "order")
     */
    private String entityType;

    /**
     * Entity ID (if applicable)
     */
    private String entityId;

    /**
     * Event data payload
     */
    private Object data;

    /**
     * Event timestamp (epoch milliseconds)
     */
    @Builder.Default
    private Long timestamp = Instant.now().toEpochMilli();

    /**
     * Optional user/session that triggered the event
     */
    private String triggeredBy;

    /**
     * Additional metadata
     */
    private Object metadata;

    /**
     * Create a CREATED event
     */
    public static WebSocketEvent created(String service, String entityType, String entityId, Object data) {
        return WebSocketEvent.builder()
                .event("CREATED")
                .service(service)
                .entityType(entityType)
                .entityId(entityId)
                .data(data)
                .build();
    }

    /**
     * Create an UPDATED event
     */
    public static WebSocketEvent updated(String service, String entityType, String entityId, Object data) {
        return WebSocketEvent.builder()
                .event("UPDATED")
                .service(service)
                .entityType(entityType)
                .entityId(entityId)
                .data(data)
                .build();
    }

    /**
     * Create a DELETED event
     */
    public static WebSocketEvent deleted(String service, String entityType, String entityId) {
        return WebSocketEvent.builder()
                .event("DELETED")
                .service(service)
                .entityType(entityType)
                .entityId(entityId)
                .build();
    }
}
