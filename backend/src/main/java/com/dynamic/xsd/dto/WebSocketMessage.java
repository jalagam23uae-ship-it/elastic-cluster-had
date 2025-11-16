package com.dynamic.xsd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket message envelope for client-server communication.
 * Used for all WebSocket operations including CRUD and subscriptions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage {

    /**
     * Service name (e.g., "customerservice")
     */
    private String service;

    /**
     * Operation type: GET, CREATE, UPDATE, DELETE, SUBSCRIBE, UNSUBSCRIBE
     */
    private String operation;

    /**
     * Unique message identifier for request-response correlation
     */
    private String messageId;

    /**
     * Message payload data
     */
    private Map<String, Object> data;

    /**
     * Message timestamp (epoch milliseconds)
     */
    @Builder.Default
    private Long timestamp = Instant.now().toEpochMilli();

    /**
     * Optional topic for subscription operations
     */
    private String topic;

    /**
     * Optional filter criteria for GET operations
     */
    private Map<String, Object> filter;
}
