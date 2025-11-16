package com.dynamic.xsd.controller;

import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.dto.WebSocketEvent;
import com.dynamic.xsd.dto.WebSocketMessage;
import com.dynamic.xsd.dto.WebSocketResponse;
import com.dynamic.xsd.repository.ServiceDefinitionRepository;
import com.dynamic.xsd.service.classloader.ServiceClassLoaderManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.annotation.SubscribeMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket controller for handling real-time communication with dynamically generated services.
 * Supports CRUD operations and pub/sub pattern for real-time updates.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DynamicWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ServiceDefinitionRepository serviceRepository;
    private final ServiceClassLoaderManager classLoaderManager;
    private final ObjectMapper objectMapper;

    // In-memory storage for demo - in production, use database or Redis
    private final Map<String, Map<String, Object>> serviceDataStore = new ConcurrentHashMap<>();

    /**
     * Handles all WebSocket messages sent to /app/service/{serviceName}
     * Routes to appropriate operation handler based on message.operation
     */
    @MessageMapping("/service/{serviceName}")
    public void handleServiceMessage(@DestinationVariable String serviceName,
                                      @Payload WebSocketMessage message) {
        log.info("WebSocket message received for service '{}': operation={}, messageId={}",
                serviceName, message.getOperation(), message.getMessageId());

        WebSocketResponse response;

        try {
            // Validate service exists and is deployed
            ServiceDefinition service = serviceRepository.findByServiceName(serviceName)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));

            if (service.getStatus() != ServiceDefinition.ServiceStatus.DEPLOYED) {
                throw new IllegalStateException("Service is not deployed: " + serviceName);
            }

            // Route to appropriate operation handler
            switch (message.getOperation().toUpperCase()) {
                case "CREATE":
                    response = handleCreate(serviceName, message);
                    break;
                case "GET":
                    response = handleGet(serviceName, message);
                    break;
                case "UPDATE":
                    response = handleUpdate(serviceName, message);
                    break;
                case "DELETE":
                    response = handleDelete(serviceName, message);
                    break;
                case "LIST":
                    response = handleList(serviceName, message);
                    break;
                default:
                    response = WebSocketResponse.error(
                            message.getMessageId(),
                            serviceName,
                            message.getOperation(),
                            "Unsupported operation: " + message.getOperation(),
                            400
                    );
            }

            // Send response back to client
            sendResponse(message.getMessageId(), response);

        } catch (Exception e) {
            log.error("Error processing WebSocket message for service '{}': {}", serviceName, e.getMessage(), e);
            response = WebSocketResponse.error(
                    message.getMessageId(),
                    serviceName,
                    message.getOperation(),
                    e.getMessage(),
                    500
            );
            sendResponse(message.getMessageId(), response);
        }
    }

    /**
     * Handle CREATE operation - creates a new entity
     */
    private WebSocketResponse handleCreate(String serviceName, WebSocketMessage message) {
        try {
            Map<String, Object> data = message.getData();
            if (data == null || data.isEmpty()) {
                return WebSocketResponse.error(
                        message.getMessageId(),
                        serviceName,
                        "CREATE",
                        "Data is required for CREATE operation",
                        400
                );
            }

            // Generate unique ID for the entity
            String entityId = UUID.randomUUID().toString();
            data.put("id", entityId);

            // Store in service data store
            serviceDataStore.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>())
                    .put(entityId, data);

            log.info("Created entity '{}' in service '{}'", entityId, serviceName);

            // Broadcast CREATED event to all subscribers
            broadcastEvent(WebSocketEvent.created(serviceName, "entity", entityId, data));

            return WebSocketResponse.builder()
                    .messageId(message.getMessageId())
                    .service(serviceName)
                    .operation("CREATE")
                    .success(true)
                    .data(data)
                    .statusCode(201)
                    .build();

        } catch (Exception e) {
            log.error("CREATE operation failed for service '{}'", serviceName, e);
            return WebSocketResponse.error(
                    message.getMessageId(),
                    serviceName,
                    "CREATE",
                    "Create failed: " + e.getMessage(),
                    500
            );
        }
    }

    /**
     * Handle GET operation - retrieves a specific entity by ID
     */
    private WebSocketResponse handleGet(String serviceName, WebSocketMessage message) {
        try {
            Map<String, Object> filter = message.getFilter();
            if (filter == null || !filter.containsKey("id")) {
                return WebSocketResponse.error(
                        message.getMessageId(),
                        serviceName,
                        "GET",
                        "Entity ID is required in filter for GET operation",
                        400
                );
            }

            String entityId = filter.get("id").toString();
            Map<String, Object> serviceData = serviceDataStore.get(serviceName);

            if (serviceData == null || !serviceData.containsKey(entityId)) {
                return WebSocketResponse.error(
                        message.getMessageId(),
                        serviceName,
                        "GET",
                        "Entity not found: " + entityId,
                        404
                );
            }

            Object entity = serviceData.get(entityId);
            log.info("Retrieved entity '{}' from service '{}'", entityId, serviceName);

            return WebSocketResponse.success(
                    message.getMessageId(),
                    serviceName,
                    "GET",
                    entity
            );

        } catch (Exception e) {
            log.error("GET operation failed for service '{}'", serviceName, e);
            return WebSocketResponse.error(
                    message.getMessageId(),
                    serviceName,
                    "GET",
                    "Get failed: " + e.getMessage(),
                    500
            );
        }
    }

    /**
     * Handle LIST operation - retrieves all entities
     */
    private WebSocketResponse handleList(String serviceName, WebSocketMessage message) {
        try {
            Map<String, Object> serviceData = serviceDataStore.get(serviceName);
            List<Object> entities = serviceData != null
                    ? new ArrayList<>(serviceData.values())
                    : new ArrayList<>();

            log.info("Listed {} entities from service '{}'", entities.size(), serviceName);

            Map<String, Object> result = new HashMap<>();
            result.put("total", entities.size());
            result.put("items", entities);

            return WebSocketResponse.success(
                    message.getMessageId(),
                    serviceName,
                    "LIST",
                    result
            );

        } catch (Exception e) {
            log.error("LIST operation failed for service '{}'", serviceName, e);
            return WebSocketResponse.error(
                    message.getMessageId(),
                    serviceName,
                    "LIST",
                    "List failed: " + e.getMessage(),
                    500
            );
        }
    }

    /**
     * Handle UPDATE operation - updates an existing entity
     */
    private WebSocketResponse handleUpdate(String serviceName, WebSocketMessage message) {
        try {
            Map<String, Object> data = message.getData();
            if (data == null || !data.containsKey("id")) {
                return WebSocketResponse.error(
                        message.getMessageId(),
                        serviceName,
                        "UPDATE",
                        "Entity ID is required in data for UPDATE operation",
                        400
                );
            }

            String entityId = data.get("id").toString();
            Map<String, Object> serviceData = serviceDataStore.get(serviceName);

            if (serviceData == null || !serviceData.containsKey(entityId)) {
                return WebSocketResponse.error(
                        message.getMessageId(),
                        serviceName,
                        "UPDATE",
                        "Entity not found: " + entityId,
                        404
                );
            }

            // Update entity
            serviceData.put(entityId, data);
            log.info("Updated entity '{}' in service '{}'", entityId, serviceName);

            // Broadcast UPDATED event to all subscribers
            broadcastEvent(WebSocketEvent.updated(serviceName, "entity", entityId, data));

            return WebSocketResponse.success(
                    message.getMessageId(),
                    serviceName,
                    "UPDATE",
                    data
            );

        } catch (Exception e) {
            log.error("UPDATE operation failed for service '{}'", serviceName, e);
            return WebSocketResponse.error(
                    message.getMessageId(),
                    serviceName,
                    "UPDATE",
                    "Update failed: " + e.getMessage(),
                    500
            );
        }
    }

    /**
     * Handle DELETE operation - deletes an entity
     */
    private WebSocketResponse handleDelete(String serviceName, WebSocketMessage message) {
        try {
            Map<String, Object> filter = message.getFilter();
            if (filter == null || !filter.containsKey("id")) {
                return WebSocketResponse.error(
                        message.getMessageId(),
                        serviceName,
                        "DELETE",
                        "Entity ID is required in filter for DELETE operation",
                        400
                );
            }

            String entityId = filter.get("id").toString();
            Map<String, Object> serviceData = serviceDataStore.get(serviceName);

            if (serviceData == null || !serviceData.containsKey(entityId)) {
                return WebSocketResponse.error(
                        message.getMessageId(),
                        serviceName,
                        "DELETE",
                        "Entity not found: " + entityId,
                        404
                );
            }

            // Delete entity
            Object deletedEntity = serviceData.remove(entityId);
            log.info("Deleted entity '{}' from service '{}'", entityId, serviceName);

            // Broadcast DELETED event to all subscribers
            broadcastEvent(WebSocketEvent.deleted(serviceName, "entity", entityId));

            Map<String, Object> result = new HashMap<>();
            result.put("id", entityId);
            result.put("deleted", true);

            return WebSocketResponse.success(
                    message.getMessageId(),
                    serviceName,
                    "DELETE",
                    result
            );

        } catch (Exception e) {
            log.error("DELETE operation failed for service '{}'", serviceName, e);
            return WebSocketResponse.error(
                    message.getMessageId(),
                    serviceName,
                    "DELETE",
                    "Delete failed: " + e.getMessage(),
                    500
            );
        }
    }

    /**
     * Handle subscription to service events
     * Clients subscribe to /topic/service/{serviceName}
     */
    @SubscribeMapping("/topic/service/{serviceName}")
    public void handleSubscription(@DestinationVariable String serviceName) {
        log.info("Client subscribed to service events: {}", serviceName);

        // Send a welcome message to the subscriber
        Map<String, Object> welcomeData = new HashMap<>();
        welcomeData.put("message", "Subscribed to " + serviceName + " events");
        welcomeData.put("service", serviceName);

        WebSocketEvent welcomeEvent = WebSocketEvent.builder()
                .event("SUBSCRIBED")
                .service(serviceName)
                .entityType("subscription")
                .data(welcomeData)
                .build();

        messagingTemplate.convertAndSend("/topic/service/" + serviceName, welcomeEvent);
    }

    /**
     * Sends response back to the client
     */
    private void sendResponse(String messageId, WebSocketResponse response) {
        // Send to user-specific destination based on message ID
        messagingTemplate.convertAndSend("/user/queue/reply", response);
    }

    /**
     * Broadcasts event to all subscribers of a service
     */
    private void broadcastEvent(WebSocketEvent event) {
        String topic = "/topic/service/" + event.getService();
        log.debug("Broadcasting {} event to topic: {}", event.getEvent(), topic);
        messagingTemplate.convertAndSend(topic, event);
    }

    /**
     * Health check endpoint for WebSocket
     */
    @SubscribeMapping("/topic/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("services", serviceDataStore.keySet().size());
        return health;
    }
}
