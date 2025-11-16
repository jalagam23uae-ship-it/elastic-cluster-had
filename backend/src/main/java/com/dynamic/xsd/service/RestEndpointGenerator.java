package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.EndpointMapping;
import com.dynamic.xsd.domain.enums.EndpointType;
import com.dynamic.xsd.domain.enums.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates and registers dynamic REST endpoints for POJO classes.
 * Supports both JSON and XML content negotiation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestEndpointGenerator {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final ObjectMapper objectMapper;

    // Track registered endpoints for cleanup
    private final Map<String, List<RequestMappingInfo>> registeredMappings = new ConcurrentHashMap<>();

    // In-memory storage for dynamic resources (in production, use a database)
    private final Map<String, Map<String, Object>> resourceStorage = new ConcurrentHashMap<>();

    /**
     * Generates REST endpoints for a set of root classes.
     */
    public List<EndpointMapping> generateRestEndpoints(String serviceName, Set<Class<?>> rootClasses, ClassLoader classLoader) {
        log.info("Generating REST endpoints for service: {} with {} root classes", serviceName, rootClasses.size());

        List<EndpointMapping> endpoints = new ArrayList<>();
        List<RequestMappingInfo> mappings = new ArrayList<>();

        for (Class<?> rootClass : rootClasses) {
            String resourceName = getResourceName(rootClass);
            String basePath = String.format("/api/v1/services/%s/%s", serviceName, resourceName);

            log.info("Creating REST endpoints for resource: {} at {}", resourceName, basePath);

            // Initialize storage for this resource
            resourceStorage.putIfAbsent(getStorageKey(serviceName, resourceName), new ConcurrentHashMap<>());

            // Create CRUD endpoints
            endpoints.addAll(createCrudEndpoints(serviceName, resourceName, basePath, rootClass, mappings));
        }

        // Store mappings for cleanup
        registeredMappings.put(serviceName, mappings);

        log.info("Generated {} REST endpoints for service: {}", endpoints.size(), serviceName);
        return endpoints;
    }

    /**
     * Creates CRUD endpoints for a resource.
     */
    private List<EndpointMapping> createCrudEndpoints(String serviceName, String resourceName,
                                                      String basePath, Class<?> resourceClass,
                                                      List<RequestMappingInfo> mappings) {
        List<EndpointMapping> endpoints = new ArrayList<>();

        try {
            // Create JAXB context for XML support
            JAXBContext jaxbContext = JAXBContext.newInstance(resourceClass);

            // GET /resources - List all
            endpoints.add(registerGetAll(serviceName, resourceName, basePath, resourceClass, jaxbContext, mappings));

            // GET /resources/{id} - Get by ID
            endpoints.add(registerGetById(serviceName, resourceName, basePath, resourceClass, jaxbContext, mappings));

            // POST /resources - Create
            endpoints.add(registerCreate(serviceName, resourceName, basePath, resourceClass, jaxbContext, mappings));

            // PUT /resources/{id} - Update
            endpoints.add(registerUpdate(serviceName, resourceName, basePath, resourceClass, jaxbContext, mappings));

            // DELETE /resources/{id} - Delete
            endpoints.add(registerDelete(serviceName, resourceName, basePath, resourceClass, jaxbContext, mappings));

        } catch (JAXBException e) {
            log.error("Failed to create JAXB context for class: {}", resourceClass.getName(), e);
        }

        return endpoints;
    }

    /**
     * Registers GET /resources endpoint.
     */
    private EndpointMapping registerGetAll(String serviceName, String resourceName, String basePath,
                                           Class<?> resourceClass, JAXBContext jaxbContext,
                                           List<RequestMappingInfo> mappings) {
        String path = basePath;
        RequestMappingInfo mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.GET)
            .produces(MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE)
            .build();

        DynamicRestHandler handler = new DynamicRestHandler(serviceName, resourceName, resourceClass, jaxbContext, objectMapper, resourceStorage);

        try {
            Method handlerMethod = DynamicRestHandler.class.getMethod("handleGetAll", HttpServletRequest.class);
            requestMappingHandlerMapping.registerMapping(mappingInfo, handler, handlerMethod);
            mappings.add(mappingInfo);

            log.debug("Registered GET {} endpoint", path);
        } catch (NoSuchMethodException e) {
            log.error("Failed to register GET all endpoint", e);
        }

        return createEndpointMapping(path, HttpMethod.GET, EndpointType.REST, "Get all " + resourceName);
    }

    /**
     * Registers GET /resources/{id} endpoint.
     */
    private EndpointMapping registerGetById(String serviceName, String resourceName, String basePath,
                                            Class<?> resourceClass, JAXBContext jaxbContext,
                                            List<RequestMappingInfo> mappings) {
        String path = basePath + "/{id}";
        RequestMappingInfo mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.GET)
            .produces(MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE)
            .build();

        DynamicRestHandler handler = new DynamicRestHandler(serviceName, resourceName, resourceClass, jaxbContext, objectMapper, resourceStorage);

        try {
            Method handlerMethod = DynamicRestHandler.class.getMethod("handleGetById", String.class, HttpServletRequest.class);
            requestMappingHandlerMapping.registerMapping(mappingInfo, handler, handlerMethod);
            mappings.add(mappingInfo);

            log.debug("Registered GET {} endpoint", path);
        } catch (NoSuchMethodException e) {
            log.error("Failed to register GET by ID endpoint", e);
        }

        return createEndpointMapping(path, HttpMethod.GET, EndpointType.REST, "Get " + resourceName + " by ID");
    }

    /**
     * Registers POST /resources endpoint.
     */
    private EndpointMapping registerCreate(String serviceName, String resourceName, String basePath,
                                           Class<?> resourceClass, JAXBContext jaxbContext,
                                           List<RequestMappingInfo> mappings) {
        String path = basePath;
        RequestMappingInfo mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.POST)
            .consumes(MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE)
            .build();

        DynamicRestHandler handler = new DynamicRestHandler(serviceName, resourceName, resourceClass, jaxbContext, objectMapper, resourceStorage);

        try {
            Method handlerMethod = DynamicRestHandler.class.getMethod("handleCreate", HttpServletRequest.class);
            requestMappingHandlerMapping.registerMapping(mappingInfo, handler, handlerMethod);
            mappings.add(mappingInfo);

            log.debug("Registered POST {} endpoint", path);
        } catch (NoSuchMethodException e) {
            log.error("Failed to register POST endpoint", e);
        }

        return createEndpointMapping(path, HttpMethod.POST, EndpointType.REST, "Create " + resourceName);
    }

    /**
     * Registers PUT /resources/{id} endpoint.
     */
    private EndpointMapping registerUpdate(String serviceName, String resourceName, String basePath,
                                           Class<?> resourceClass, JAXBContext jaxbContext,
                                           List<RequestMappingInfo> mappings) {
        String path = basePath + "/{id}";
        RequestMappingInfo mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.PUT)
            .consumes(MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE)
            .build();

        DynamicRestHandler handler = new DynamicRestHandler(serviceName, resourceName, resourceClass, jaxbContext, objectMapper, resourceStorage);

        try {
            Method handlerMethod = DynamicRestHandler.class.getMethod("handleUpdate", String.class, HttpServletRequest.class);
            requestMappingHandlerMapping.registerMapping(mappingInfo, handler, handlerMethod);
            mappings.add(mappingInfo);

            log.debug("Registered PUT {} endpoint", path);
        } catch (NoSuchMethodException e) {
            log.error("Failed to register PUT endpoint", e);
        }

        return createEndpointMapping(path, HttpMethod.PUT, EndpointType.REST, "Update " + resourceName);
    }

    /**
     * Registers DELETE /resources/{id} endpoint.
     */
    private EndpointMapping registerDelete(String serviceName, String resourceName, String basePath,
                                           Class<?> resourceClass, JAXBContext jaxbContext,
                                           List<RequestMappingInfo> mappings) {
        String path = basePath + "/{id}";
        RequestMappingInfo mappingInfo = RequestMappingInfo
            .paths(path)
            .methods(RequestMethod.DELETE)
            .build();

        DynamicRestHandler handler = new DynamicRestHandler(serviceName, resourceName, resourceClass, jaxbContext, objectMapper, resourceStorage);

        try {
            Method handlerMethod = DynamicRestHandler.class.getMethod("handleDelete", String.class, HttpServletRequest.class);
            requestMappingHandlerMapping.registerMapping(mappingInfo, handler, handlerMethod);
            mappings.add(mappingInfo);

            log.debug("Registered DELETE {} endpoint", path);
        } catch (NoSuchMethodException e) {
            log.error("Failed to register DELETE endpoint", e);
        }

        return createEndpointMapping(path, HttpMethod.DELETE, EndpointType.REST, "Delete " + resourceName);
    }

    /**
     * Unregisters all endpoints for a service.
     */
    public void unregisterEndpoints(List<EndpointMapping> endpoints) {
        for (EndpointMapping endpoint : endpoints) {
            String serviceName = extractServiceName(endpoint.getPath());
            List<RequestMappingInfo> mappings = registeredMappings.get(serviceName);

            if (mappings != null) {
                for (RequestMappingInfo mapping : mappings) {
                    requestMappingHandlerMapping.unregisterMapping(mapping);
                    log.debug("Unregistered endpoint: {}", mapping);
                }
                registeredMappings.remove(serviceName);
            }

            // Clear storage for this service
            String resourceName = extractResourceName(endpoint.getPath());
            resourceStorage.remove(getStorageKey(serviceName, resourceName));
        }
    }

    private String getResourceName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase() + "s";
    }

    private String getStorageKey(String serviceName, String resourceName) {
        return serviceName + ":" + resourceName;
    }

    private String extractServiceName(String path) {
        // Extract from path like /api/v1/services/{serviceName}/{resource}
        String[] parts = path.split("/");
        return parts.length > 4 ? parts[4] : "";
    }

    private String extractResourceName(String path) {
        String[] parts = path.split("/");
        return parts.length > 5 ? parts[5] : "";
    }

    private EndpointMapping createEndpointMapping(String path, HttpMethod method, EndpointType type, String description) {
        EndpointMapping mapping = new EndpointMapping();
        mapping.setPath(path);
        mapping.setHttpMethod(method);
        mapping.setEndpointType(type);
        mapping.setDescription(description);
        mapping.setCreatedAt(LocalDateTime.now());
        return mapping;
    }

    /**
     * Dynamic handler for REST requests.
     * Handles JSON/XML serialization and CRUD operations.
     */
    public static class DynamicRestHandler {
        private final String serviceName;
        private final String resourceName;
        private final Class<?> resourceClass;
        private final JAXBContext jaxbContext;
        private final ObjectMapper objectMapper;
        private final Map<String, Map<String, Object>> resourceStorage;
        private final AtomicLong idGenerator = new java.util.concurrent.atomic.AtomicLong(1);

        public DynamicRestHandler(String serviceName, String resourceName, Class<?> resourceClass,
                                  JAXBContext jaxbContext, ObjectMapper objectMapper,
                                  Map<String, Map<String, Object>> resourceStorage) {
            this.serviceName = serviceName;
            this.resourceName = resourceName;
            this.resourceClass = resourceClass;
            this.jaxbContext = jaxbContext;
            this.objectMapper = objectMapper;
            this.resourceStorage = resourceStorage;
        }

        public ResponseEntity<?> handleGetAll(HttpServletRequest request) {
            Map<String, Object> storage = getStorage();
            Collection<Object> resources = storage.values();

            return serializeResponse(resources, request);
        }

        public ResponseEntity<?> handleGetById(String id, HttpServletRequest request) {
            Map<String, Object> storage = getStorage();
            Object resource = storage.get(id);

            if (resource == null) {
                return ResponseEntity.notFound().build();
            }

            return serializeResponse(resource, request);
        }

        public ResponseEntity<?> handleCreate(HttpServletRequest request) throws Exception {
            Object resource = deserializeRequest(request);

            Map<String, Object> storage = getStorage();
            String id = String.valueOf(idGenerator.getAndIncrement());

            // Set ID using reflection if possible
            setIdField(resource, id);

            storage.put(id, resource);

            return ResponseEntity.status(HttpStatus.CREATED).body(serializeObject(resource, request));
        }

        public ResponseEntity<?> handleUpdate(String id, HttpServletRequest request) throws Exception {
            Map<String, Object> storage = getStorage();

            if (!storage.containsKey(id)) {
                return ResponseEntity.notFound().build();
            }

            Object resource = deserializeRequest(request);
            setIdField(resource, id);
            storage.put(id, resource);

            return serializeResponse(resource, request);
        }

        public ResponseEntity<?> handleDelete(String id, HttpServletRequest request) {
            Map<String, Object> storage = getStorage();
            Object removed = storage.remove(id);

            if (removed == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.noContent().build();
        }

        private Map<String, Object> getStorage() {
            return resourceStorage.computeIfAbsent(
                serviceName + ":" + resourceName,
                k -> new ConcurrentHashMap<>()
            );
        }

        private Object deserializeRequest(HttpServletRequest request) throws Exception {
            String contentType = request.getContentType();

            if (contentType != null && contentType.contains("xml")) {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                return unmarshaller.unmarshal(request.getInputStream());
            } else {
                return objectMapper.readValue(request.getInputStream(), resourceClass);
            }
        }

        private ResponseEntity<?> serializeResponse(Object data, HttpServletRequest request) {
            try {
                Object serialized = serializeObject(data, request);
                return ResponseEntity.ok(serialized);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Serialization error: " + e.getMessage());
            }
        }

        private Object serializeObject(Object data, HttpServletRequest request) throws Exception {
            String acceptHeader = request.getHeader("Accept");

            if (acceptHeader != null && acceptHeader.contains("xml")) {
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                StringWriter writer = new StringWriter();
                marshaller.marshal(data, writer);
                return writer.toString();
            } else {
                // Return as-is for JSON (Spring will serialize)
                return data;
            }
        }

        private void setIdField(Object resource, String id) {
            try {
                java.lang.reflect.Field idField = findIdField(resource.getClass());
                if (idField != null) {
                    idField.setAccessible(true);
                    idField.set(resource, id);
                }
            } catch (Exception e) {
                // ID field not found or not settable, ignore
            }
        }

        private java.lang.reflect.Field findIdField(Class<?> clazz) {
            // Look for common ID field names
            for (String fieldName : Arrays.asList("id", "ID", "identifier")) {
                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    // Continue searching
                }
            }
            return null;
        }
    }
}
