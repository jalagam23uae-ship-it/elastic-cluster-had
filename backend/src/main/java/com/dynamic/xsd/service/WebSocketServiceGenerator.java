package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.EndpointMapping;
import com.dynamic.xsd.domain.enums.EndpointType;
import com.dynamic.xsd.domain.enums.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates and manages WebSocket endpoints for dynamically generated services.
 * Unlike REST/SOAP, WebSocket uses a message-based protocol with topics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceGenerator {

    // Track enabled WebSocket services
    private final Map<String, WebSocketServiceMetadata> enabledServices = new ConcurrentHashMap<>();

    /**
     * Generates WebSocket endpoints for a service.
     * WebSocket endpoints are message-based, not URL-based like REST.
     */
    public List<EndpointMapping> generateWebSocketEndpoints(String serviceName, Set<Class<?>> rootClasses) {
        log.info("Generating WebSocket endpoints for service: {} with {} root classes",
                serviceName, rootClasses.size());

        List<EndpointMapping> endpoints = new ArrayList<>();

        // Create metadata for this service
        WebSocketServiceMetadata metadata = new WebSocketServiceMetadata();
        metadata.serviceName = serviceName;
        metadata.rootClasses = new HashSet<>(rootClasses);
        metadata.enabledAt = new Date();
        metadata.topics = generateTopics(serviceName, rootClasses);

        // Store service metadata
        enabledServices.put(serviceName, metadata);

        // Create endpoint mappings for documentation/tracking
        // WebSocket endpoints:
        // 1. Message endpoint: /app/service/{serviceName}
        // 2. Subscription topic: /topic/service/{serviceName}
        // 3. User queue: /user/queue/reply

        // Message endpoint
        endpoints.add(createEndpointMapping(
                "/app/service/" + serviceName,
                "WebSocket message endpoint for " + serviceName,
                "Handles CREATE, GET, UPDATE, DELETE, LIST operations"
        ));

        // Subscription topic
        endpoints.add(createEndpointMapping(
                "/topic/service/" + serviceName,
                "WebSocket pub/sub topic for " + serviceName,
                "Broadcasts CREATED, UPDATED, DELETED events"
        ));

        // Health check
        endpoints.add(createEndpointMapping(
                "/app/health/" + serviceName,
                "WebSocket health check for " + serviceName,
                "Returns service health status"
        ));

        log.info("Generated {} WebSocket endpoints for service: {}", endpoints.size(), serviceName);
        return endpoints;
    }

    /**
     * Unregisters WebSocket endpoints for a service.
     */
    public void unregisterEndpoints(List<EndpointMapping> endpoints) {
        for (EndpointMapping endpoint : endpoints) {
            String serviceName = extractServiceName(endpoint.getPath());
            if (serviceName != null) {
                enabledServices.remove(serviceName);
                log.info("Unregistered WebSocket service: {}", serviceName);
            }
        }
    }

    /**
     * Checks if WebSocket is enabled for a service.
     */
    public boolean isWebSocketEnabled(String serviceName) {
        return enabledServices.containsKey(serviceName);
    }

    /**
     * Gets metadata for a WebSocket-enabled service.
     */
    public WebSocketServiceMetadata getServiceMetadata(String serviceName) {
        return enabledServices.get(serviceName);
    }

    /**
     * Gets all WebSocket-enabled services.
     */
    public Set<String> getEnabledServices() {
        return new HashSet<>(enabledServices.keySet());
    }

    /**
     * Gets service statistics.
     */
    public Map<String, Object> getServiceStatistics(String serviceName) {
        WebSocketServiceMetadata metadata = enabledServices.get(serviceName);
        if (metadata == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("serviceName", metadata.serviceName);
        stats.put("enabledAt", metadata.enabledAt);
        stats.put("rootClasses", metadata.rootClasses.stream()
                .map(Class::getSimpleName)
                .toList());
        stats.put("topics", metadata.topics);
        stats.put("isEnabled", true);

        return stats;
    }

    /**
     * Generates topic paths for a service.
     */
    private List<String> generateTopics(String serviceName, Set<Class<?>> rootClasses) {
        List<String> topics = new ArrayList<>();

        // Main service topic
        topics.add("/topic/service/" + serviceName);

        // Operation-specific topics (optional, for granular subscriptions)
        topics.add("/topic/service/" + serviceName + "/created");
        topics.add("/topic/service/" + serviceName + "/updated");
        topics.add("/topic/service/" + serviceName + "/deleted");

        // Entity-specific topics for each root class
        for (Class<?> rootClass : rootClasses) {
            String entityType = rootClass.getSimpleName().toLowerCase();
            topics.add("/topic/service/" + serviceName + "/" + entityType);
        }

        return topics;
    }

    /**
     * Creates an endpoint mapping for WebSocket.
     */
    private EndpointMapping createEndpointMapping(String path, String description, String details) {
        EndpointMapping mapping = new EndpointMapping();
        mapping.setPath(path);
        mapping.setHttpMethod(HttpMethod.WS); // WebSocket method
        mapping.setEndpointType(EndpointType.WEBSOCKET);
        mapping.setDescription(description);
        mapping.setOperationName(details);
        return mapping;
    }

    /**
     * Extracts service name from WebSocket path.
     */
    private String extractServiceName(String path) {
        // Extract from patterns like "/app/service/{serviceName}" or "/topic/service/{serviceName}"
        if (path != null && path.contains("/service/")) {
            String[] parts = path.split("/service/");
            if (parts.length > 1) {
                return parts[1].split("/")[0];
            }
        }
        return null;
    }

    /**
     * Metadata for a WebSocket-enabled service.
     */
    public static class WebSocketServiceMetadata {
        public String serviceName;
        public Set<Class<?>> rootClasses;
        public Date enabledAt;
        public List<String> topics;
        public Map<String, Object> configuration = new HashMap<>();

        // Operation support flags
        public boolean supportsCreate = true;
        public boolean supportsRead = true;
        public boolean supportsUpdate = true;
        public boolean supportsDelete = true;
        public boolean supportsPubSub = true;
    }
}
