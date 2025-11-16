package com.dynamic.xsd.graphql;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic data fetcher for GraphQL queries and mutations.
 * Handles runtime data fetching for dynamically generated services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicGraphQLDataFetcher {

    // In-memory storage for demo purposes (would use database in production)
    // Structure: serviceName -> typeName -> entityId -> entity data
    private final Map<String, Map<String, Map<String, Map<String, Object>>>> storage = new ConcurrentHashMap<>();

    /**
     * Fetch entity by ID.
     */
    public Map<String, Object> getById(String serviceName, String typeName, String id) {
        log.debug("GraphQL: Fetching {} with id {} from service {}", typeName, id, serviceName);

        Map<String, Map<String, Object>> typeStorage = getTypeStorage(serviceName, typeName);
        return typeStorage.get(id);
    }

    /**
     * Fetch all entities of a type.
     */
    public List<Map<String, Object>> getAll(String serviceName, String typeName, Integer page, Integer pageSize) {
        log.debug("GraphQL: Fetching all {} from service {} (page: {}, pageSize: {})",
                  typeName, serviceName, page, pageSize);

        Map<String, Map<String, Object>> typeStorage = getTypeStorage(serviceName, typeName);
        List<Map<String, Object>> allEntities = new ArrayList<>(typeStorage.values());

        // Apply pagination if requested
        if (page != null && pageSize != null) {
            int start = page * pageSize;
            int end = Math.min(start + pageSize, allEntities.size());

            if (start < allEntities.size()) {
                return allEntities.subList(start, end);
            }
            return Collections.emptyList();
        }

        return allEntities;
    }

    /**
     * Search entities with filter.
     */
    public List<Map<String, Object>> search(String serviceName, String typeName, Map<String, Object> filter) {
        log.debug("GraphQL: Searching {} in service {} with filter {}", typeName, serviceName, filter);

        Map<String, Map<String, Object>> typeStorage = getTypeStorage(serviceName, typeName);

        if (filter == null || filter.isEmpty()) {
            return new ArrayList<>(typeStorage.values());
        }

        // Simple filtering logic
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> entity : typeStorage.values()) {
            if (matchesFilter(entity, filter)) {
                results.add(entity);
            }
        }

        return results;
    }

    /**
     * Create a new entity.
     */
    public Map<String, Object> create(String serviceName, String typeName, Map<String, Object> input) {
        log.debug("GraphQL: Creating {} in service {} with data {}", typeName, serviceName, input);

        Map<String, Map<String, Object>> typeStorage = getTypeStorage(serviceName, typeName);

        // Generate ID if not provided
        String id = (String) input.getOrDefault("id", UUID.randomUUID().toString());

        Map<String, Object> entity = new HashMap<>(input);
        entity.put("id", id);

        typeStorage.put(id, entity);

        log.info("GraphQL: Created {} with id {} in service {}", typeName, id, serviceName);
        return entity;
    }

    /**
     * Update an existing entity.
     */
    public Map<String, Object> update(String serviceName, String typeName, String id, Map<String, Object> input) {
        log.debug("GraphQL: Updating {} with id {} in service {}", typeName, id, serviceName);

        Map<String, Map<String, Object>> typeStorage = getTypeStorage(serviceName, typeName);

        Map<String, Object> existing = typeStorage.get(id);
        if (existing == null) {
            throw new RuntimeException("Entity not found: " + typeName + " with id " + id);
        }

        // Merge updates
        Map<String, Object> updated = new HashMap<>(existing);
        updated.putAll(input);
        updated.put("id", id); // Ensure ID doesn't change

        typeStorage.put(id, updated);

        log.info("GraphQL: Updated {} with id {} in service {}", typeName, id, serviceName);
        return updated;
    }

    /**
     * Delete an entity.
     */
    public Map<String, Object> delete(String serviceName, String typeName, String id) {
        log.debug("GraphQL: Deleting {} with id {} from service {}", typeName, id, serviceName);

        Map<String, Map<String, Object>> typeStorage = getTypeStorage(serviceName, typeName);

        Map<String, Object> deleted = typeStorage.remove(id);

        if (deleted == null) {
            log.warn("GraphQL: Entity not found for deletion: {} with id {}", typeName, id);
            return Map.of("success", false, "message", "Entity not found");
        }

        log.info("GraphQL: Deleted {} with id {} from service {}", typeName, id, serviceName);
        return Map.of("success", true, "message", "Entity deleted successfully");
    }

    /**
     * Get or create storage for a specific service and type.
     */
    private Map<String, Map<String, Object>> getTypeStorage(String serviceName, String typeName) {
        Map<String, Map<String, Map<String, Object>>> serviceStorage =
            storage.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>());
        return serviceStorage.computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
    }

    /**
     * Check if an entity matches the filter criteria.
     */
    private boolean matchesFilter(Map<String, Object> entity, Map<String, Object> filter) {
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Object filterValue = entry.getValue();
            Object entityValue = entity.get(key);

            if (entityValue == null) {
                if (filterValue != null) {
                    return false;
                }
            } else if (!entityValue.equals(filterValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clear all data (for testing purposes).
     */
    public void clearAll() {
        storage.clear();
        log.info("GraphQL: Cleared all storage");
    }

    /**
     * Get storage stats.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("services", storage.size());

        int totalEntities = 0;
        for (Map<String, Map<String, Map<String, Object>>> serviceStorage : storage.values()) {
            for (Map<String, Map<String, Object>> typeStorage : serviceStorage.values()) {
                totalEntities += typeStorage.size();
            }
        }
        stats.put("totalEntities", totalEntities);

        return stats;
    }
}
