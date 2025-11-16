package com.dynamic.xsd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WebSocket response message sent back to clients.
 * Contains the result of the operation or error information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketResponse {

    /**
     * Original message ID for correlation
     */
    private String messageId;

    /**
     * Operation success status
     */
    private boolean success;

    /**
     * Response data (if successful)
     */
    private Object data;

    /**
     * Error message (if failed)
     */
    private String error;

    /**
     * Response timestamp (epoch milliseconds)
     */
    @Builder.Default
    private Long timestamp = Instant.now().toEpochMilli();

    /**
     * HTTP-like status code for WebSocket operations
     * 200 = OK, 201 = Created, 400 = Bad Request, 404 = Not Found, 500 = Internal Error
     */
    private Integer statusCode;

    /**
     * Service name for context
     */
    private String service;

    /**
     * Operation that was performed
     */
    private String operation;

    /**
     * Create a success response
     */
    public static WebSocketResponse success(String messageId, String service, String operation, Object data) {
        return WebSocketResponse.builder()
                .messageId(messageId)
                .service(service)
                .operation(operation)
                .success(true)
                .data(data)
                .statusCode(200)
                .build();
    }

    /**
     * Create an error response
     */
    public static WebSocketResponse error(String messageId, String service, String operation, String error, Integer statusCode) {
        return WebSocketResponse.builder()
                .messageId(messageId)
                .service(service)
                .operation(operation)
                .success(false)
                .error(error)
                .statusCode(statusCode)
                .build();
    }
}
