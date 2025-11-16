package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.EndpointMapping;
import com.dynamic.xsd.domain.enums.EndpointType;
import com.dynamic.xsd.domain.enums.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates and registers dynamic SOAP web service endpoints.
 * Uses JAXB for marshalling/unmarshalling XML messages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoapServiceGenerator {

    // Track registered SOAP endpoints for cleanup
    private final Map<String, List<SoapEndpointInfo>> registeredEndpoints = new ConcurrentHashMap<>();

    // In-memory storage for SOAP operations (in production, use a database)
    private final Map<String, Map<String, Object>> soapStorage = new ConcurrentHashMap<>();

    /**
     * Generates SOAP endpoints for a set of root classes.
     */
    public List<EndpointMapping> generateSoapEndpoints(String serviceName, Set<Class<?>> rootClasses,
                                                       ClassLoader classLoader, String wsdlContent) {
        log.info("Generating SOAP endpoints for service: {} with {} root classes", serviceName, rootClasses.size());

        List<EndpointMapping> endpoints = new ArrayList<>();
        List<SoapEndpointInfo> endpointInfos = new ArrayList<>();

        for (Class<?> rootClass : rootClasses) {
            try {
                String operationName = getOperationName(rootClass);
                String endpoint = String.format("/ws/%s", serviceName);

                log.info("Creating SOAP endpoint for operation: {} at {}", operationName, endpoint);

                // Create JAXB context for this class
                JAXBContext jaxbContext = JAXBContext.newInstance(rootClass);

                // Create endpoint info
                SoapEndpointInfo endpointInfo = new SoapEndpointInfo();
                endpointInfo.serviceName = serviceName;
                endpointInfo.operationName = operationName;
                endpointInfo.rootClass = rootClass;
                endpointInfo.jaxbContext = jaxbContext;
                endpointInfo.endpoint = endpoint;
                endpointInfo.wsdlContent = wsdlContent;

                endpointInfos.add(endpointInfo);

                // Create endpoint mapping
                EndpointMapping mapping = createEndpointMapping(
                    endpoint,
                    operationName,
                    rootClass.getSimpleName()
                );
                endpoints.add(mapping);

                // Initialize storage for this operation
                soapStorage.putIfAbsent(getStorageKey(serviceName, operationName), new ConcurrentHashMap<>());

            } catch (JAXBException e) {
                log.error("Failed to create JAXB context for class: {}", rootClass.getName(), e);
            }
        }

        // Store endpoint infos for cleanup
        registeredEndpoints.put(serviceName, endpointInfos);

        log.info("Generated {} SOAP endpoints for service: {}", endpoints.size(), serviceName);
        return endpoints;
    }

    /**
     * Processes a SOAP request.
     * This method would be called by a SOAP message dispatcher.
     */
    public String processSoapRequest(String serviceName, String operationName, String requestXml) {
        try {
            log.debug("Processing SOAP request for service: {}, operation: {}", serviceName, operationName);

            SoapEndpointInfo endpointInfo = findEndpointInfo(serviceName, operationName);
            if (endpointInfo == null) {
                return createSoapFault("Server", "Unknown operation: " + operationName);
            }

            // Unmarshal request
            Unmarshaller unmarshaller = endpointInfo.jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(requestXml);
            Object request = unmarshaller.unmarshal(reader);

            // Extract actual object if wrapped in JAXBElement
            if (request instanceof JAXBElement) {
                request = ((JAXBElement<?>) request).getValue();
            }

            // Process request (store in memory)
            Object response = processOperation(serviceName, operationName, request);

            // Marshal response
            Marshaller marshaller = endpointInfo.jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            StringWriter writer = new StringWriter();

            // Wrap in JAXBElement if class doesn't have @XmlRootElement
            if (response.getClass().getAnnotation(XmlRootElement.class) == null) {
                QName qName = new QName(getNamespace(serviceName), response.getClass().getSimpleName());
                JAXBElement<?> jaxbElement = new JAXBElement<>(qName, (Class) response.getClass(), response);
                marshaller.marshal(jaxbElement, writer);
            } else {
                marshaller.marshal(response, writer);
            }

            return wrapSoapResponse(writer.toString());

        } catch (Exception e) {
            log.error("SOAP request processing failed", e);
            return createSoapFault("Server", "Request processing failed: " + e.getMessage());
        }
    }

    /**
     * Gets WSDL content for a service.
     */
    public String getWsdl(String serviceName) {
        List<SoapEndpointInfo> endpoints = registeredEndpoints.get(serviceName);
        if (endpoints != null && !endpoints.isEmpty()) {
            return endpoints.get(0).wsdlContent;
        }
        return null;
    }

    /**
     * Unregisters all SOAP endpoints for a service.
     */
    public void unregisterEndpoints(List<EndpointMapping> endpoints) {
        for (EndpointMapping endpoint : endpoints) {
            String serviceName = extractServiceName(endpoint.getPath());
            registeredEndpoints.remove(serviceName);

            // Clear storage
            String operationName = extractOperationName(endpoint.getDescription());
            soapStorage.remove(getStorageKey(serviceName, operationName));

            log.debug("Unregistered SOAP endpoint: {}", endpoint.getPath());
        }
    }

    /**
     * Processes a SOAP operation (simple in-memory CRUD).
     */
    private Object processOperation(String serviceName, String operationName, Object request) throws Exception {
        Map<String, Object> storage = soapStorage.get(getStorageKey(serviceName, operationName));

        if (storage == null) {
            storage = new ConcurrentHashMap<>();
            soapStorage.put(getStorageKey(serviceName, operationName), storage);
        }

        // Generate ID for the object
        String id = UUID.randomUUID().toString();

        // Try to set ID field if it exists
        setIdField(request, id);

        // Store object
        storage.put(id, request);

        log.debug("Stored object with ID: {} for operation: {}", id, operationName);

        // Return the same object as response (echo pattern)
        // In a real implementation, you might want to return a different response object
        return request;
    }

    /**
     * Finds endpoint info by service name and operation.
     */
    private SoapEndpointInfo findEndpointInfo(String serviceName, String operationName) {
        List<SoapEndpointInfo> endpoints = registeredEndpoints.get(serviceName);
        if (endpoints != null) {
            return endpoints.stream()
                .filter(e -> e.operationName.equals(operationName))
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    /**
     * Wraps response in SOAP envelope.
     */
    private String wrapSoapResponse(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soap:Body>\n" +
            "    " + body + "\n" +
            "  </soap:Body>\n" +
            "</soap:Envelope>";
    }

    /**
     * Creates a SOAP fault response.
     */
    private String createSoapFault(String faultCode, String faultString) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soap:Body>\n" +
            "    <soap:Fault>\n" +
            "      <faultcode>soap:" + faultCode + "</faultcode>\n" +
            "      <faultstring>" + faultString + "</faultstring>\n" +
            "    </soap:Fault>\n" +
            "  </soap:Body>\n" +
            "</soap:Envelope>";
    }

    /**
     * Sets ID field on an object using reflection.
     */
    private void setIdField(Object obj, String id) {
        try {
            java.lang.reflect.Field idField = findIdField(obj.getClass());
            if (idField != null) {
                idField.setAccessible(true);
                idField.set(obj, id);
            }
        } catch (Exception e) {
            // ID field not found or not settable, ignore
            log.debug("Could not set ID field", e);
        }
    }

    /**
     * Finds ID field in a class.
     */
    private java.lang.reflect.Field findIdField(Class<?> clazz) {
        for (String fieldName : Arrays.asList("id", "ID", "identifier")) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Continue searching
            }
        }
        return null;
    }

    private String getOperationName(Class<?> rootClass) {
        return "process" + rootClass.getSimpleName();
    }

    private String getStorageKey(String serviceName, String operationName) {
        return serviceName + ":" + operationName;
    }

    private String extractServiceName(String path) {
        // Extract from path like /ws/{serviceName}
        String[] parts = path.split("/");
        return parts.length > 2 ? parts[2] : "";
    }

    private String extractOperationName(String description) {
        // Extract from description like "SOAP operation: processXXX"
        if (description != null && description.contains("process")) {
            return description.substring(description.indexOf("process"));
        }
        return "";
    }

    private String getNamespace(String serviceName) {
        return "http://dynamicxsd.com/services/" + serviceName;
    }

    private EndpointMapping createEndpointMapping(String path, String operation, String description) {
        EndpointMapping mapping = new EndpointMapping();
        mapping.setPath(path);
        mapping.setHttpMethod(HttpMethod.POST); // SOAP always uses POST
        mapping.setEndpointType(EndpointType.SOAP);
        mapping.setDescription("SOAP operation: " + operation + " for " + description);
        mapping.setCreatedAt(LocalDateTime.now());
        return mapping;
    }

    /**
     * Information about a registered SOAP endpoint.
     */
    private static class SoapEndpointInfo {
        String serviceName;
        String operationName;
        Class<?> rootClass;
        JAXBContext jaxbContext;
        String endpoint;
        String wsdlContent;
    }
}
