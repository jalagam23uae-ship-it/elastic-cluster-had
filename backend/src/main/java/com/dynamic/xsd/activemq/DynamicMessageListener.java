package com.dynamic.xsd.activemq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic message listener and processor for ActiveMQ messages.
 * Handles CRUD operations received via JMS queues.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMessageListener {

    // In-memory storage for demo purposes
    private final Map<String, Map<String, Map<String, Object>>> storage = new ConcurrentHashMap<>();
    private final ActiveMQMessageService messageService;

    /**
     * Process incoming service message.
     */
    public Map<String, Object> processMessage(Map<String, Object> message) {
        String serviceName = (String) message.get("service");
        String operation = (String) message.get("operation");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) message.get("data");

        log.info("Processing ActiveMQ message - Service: {}, Operation: {}", serviceName, operation);

        try {
            Map<String, Object> result = switch (operation) {
                case "CREATE" -> handleCreate(serviceName, data);
                case "GET" -> handleGet(serviceName, data);
                case "UPDATE" -> handleUpdate(serviceName, data);
                case "DELETE" -> handleDelete(serviceName, data);
                case "LIST" -> handleList(serviceName);
                default -> Map.of("error", "Unknown operation: " + operation);
            };

            log.debug("Operation {} completed successfully for service {}", operation, serviceName);
            return result;

        } catch (Exception e) {
            log.error("Failed to process message for service {}: {}", serviceName, e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Handle CREATE operation.
     */
    private Map<String, Object> handleCreate(String serviceName, Map<String, Object> data) {
        Map<String, Map<String, Object>> serviceStorage = getServiceStorage(serviceName);

        String id = (String) data.getOrDefault("id", UUID.randomUUID().toString());
        Map<String, Object> entity = new HashMap<>(data);
        entity.put("id", id);
        entity.put("createdAt", System.currentTimeMillis());

        serviceStorage.put(id, entity);

        // Publish event
        messageService.publishServiceEvent(serviceName, "CREATED", entity);

        log.info("Created entity with id {} in service {}", id, serviceName);
        return entity;
    }

    /**
     * Handle GET operation.
     */
    private Map<String, Object> handleGet(String serviceName, Map<String, Object> data) {
        String id = (String) data.get("id");
        Map<String, Map<String, Object>> serviceStorage = getServiceStorage(serviceName);

        Map<String, Object> entity = serviceStorage.get(id);
        if (entity == null) {
            return Map.of("error", "Entity not found with id: " + id);
        }

        log.debug("Retrieved entity with id {} from service {}", id, serviceName);
        return entity;
    }

    /**
     * Handle UPDATE operation.
     */
    private Map<String, Object> handleUpdate(String serviceName, Map<String, Object> data) {
        String id = (String) data.get("id");
        Map<String, Map<String, Object>> serviceStorage = getServiceStorage(serviceName);

        Map<String, Object> existing = serviceStorage.get(id);
        if (existing == null) {
            return Map.of("error", "Entity not found with id: " + id);
        }

        Map<String, Object> updated = new HashMap<>(existing);
        updated.putAll(data);
        updated.put("id", id);
        updated.put("updatedAt", System.currentTimeMillis());

        serviceStorage.put(id, updated);

        // Publish event
        messageService.publishServiceEvent(serviceName, "UPDATED", updated);

        log.info("Updated entity with id {} in service {}", id, serviceName);
        return updated;
    }

    /**
     * Handle DELETE operation.
     */
    private Map<String, Object> handleDelete(String serviceName, Map<String, Object> data) {
        String id = (String) data.get("id");
        Map<String, Map<String, Object>> serviceStorage = getServiceStorage(serviceName);

        Map<String, Object> deleted = serviceStorage.remove(id);
        if (deleted == null) {
            return Map.of("error", "Entity not found with id: " + id);
        }

        // Publish event
        messageService.publishServiceEvent(serviceName, "DELETED", Map.of("id", id));

        log.info("Deleted entity with id {} from service {}", id, serviceName);
        return Map.of("success", true, "id", id);
    }

    /**
     * Handle LIST operation.
     */
    private Map<String, Object> handleList(String serviceName) {
        Map<String, Map<String, Object>> serviceStorage = getServiceStorage(serviceName);
        List<Map<String, Object>> entities = new ArrayList<>(serviceStorage.values());

        log.debug("Listed {} entities from service {}", entities.size(), serviceName);
        return Map.of("entities", entities, "count", entities.size());
    }

    /**
     * Get or create storage for a service.
     */
    private Map<String, Map<String, Object>> getServiceStorage(String serviceName) {
        return storage.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>());
    }

    /**
     * Clear all storage (for testing).
     */
    public void clearAll() {
        storage.clear();
        log.info("Cleared all ActiveMQ storage");
    }

    /**
     * Get statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("services", storage.size());

        int totalEntities = 0;
        for (Map<String, Map<String, Object>> serviceStorage : storage.values()) {
            totalEntities += serviceStorage.size();
        }
        stats.put("totalEntities", totalEntities);

        return stats;
    }
}
