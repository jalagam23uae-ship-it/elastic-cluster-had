package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.EndpointMapping;
import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.domain.enums.EndpointType;
import com.dynamic.xsd.repository.EndpointMappingRepository;
import com.dynamic.xsd.repository.SchemaMetadataRepository;
import com.dynamic.xsd.repository.ServiceDefinitionRepository;
import com.dynamic.xsd.service.classloader.ServiceClassLoaderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for deploying and undeploying dynamically generated services.
 * Orchestrates REST and SOAP endpoint generation and registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceDeploymentService {

    private final SchemaMetadataRepository schemaRepository;
    private final ServiceDefinitionRepository serviceRepository;
    private final EndpointMappingRepository endpointRepository;
    private final ServiceClassLoaderManager classLoaderManager;
    private final RestEndpointGenerator restEndpointGenerator;
    private final SoapServiceGenerator soapServiceGenerator;
    private final WsdlGenerator wsdlGenerator;
    private final WebSocketServiceGenerator webSocketServiceGenerator;

    /**
     * Deploys a service based on schema metadata.
     * Generates both REST and SOAP endpoints.
     */
    @Transactional
    public DeploymentResult deployService(String schemaId) {
        log.info("Starting service deployment for schema ID: {}", schemaId);

        SchemaMetadata schema = schemaRepository.findById(schemaId)
            .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaId));

        if (schema.getStatus() != SchemaMetadata.SchemaStatus.ACTIVE) {
            throw new IllegalStateException("Schema must be ACTIVE to deploy. Current status: " + schema.getStatus());
        }

        DeploymentResult result = new DeploymentResult();
        result.schemaId = schemaId;
        result.serviceName = schema.getServiceName();

        try {
            // Get or create class loader for this service
            ClassLoader serviceClassLoader = classLoaderManager.getOrCreateClassLoader(
                schema.getServiceName(),
                schema.getClassOutputPath()
            );

            // Find root classes to expose as services
            Set<Class<?>> rootClasses = findRootClasses(schema, serviceClassLoader);

            if (rootClasses.isEmpty()) {
                throw new IllegalStateException("No root classes found to expose as services");
            }

            log.info("Found {} root classes to expose for service: {}", rootClasses.size(), schema.getServiceName());

            // Create or update service definition
            ServiceDefinition serviceDefinition = createOrUpdateServiceDefinition(schema);
            // Save immediately to get ID for endpoint mappings
            serviceDefinition = serviceRepository.save(serviceDefinition);

            // Generate and register REST endpoints
            List<EndpointMapping> restEndpoints = restEndpointGenerator.generateRestEndpoints(
                schema.getServiceName(),
                rootClasses,
                serviceClassLoader
            );
            result.restEndpoints.addAll(restEndpoints);
            saveEndpointMappings(serviceDefinition, restEndpoints);

            // Generate WSDL
            String wsdlContent = wsdlGenerator.generateWsdl(
                schema.getServiceName(),
                schema.getTargetNamespace(),
                rootClasses,
                schema.getXsdContent()
            );
            serviceDefinition.setWsdlContent(wsdlContent);

            // Generate and register SOAP endpoints
            List<EndpointMapping> soapEndpoints = soapServiceGenerator.generateSoapEndpoints(
                schema.getServiceName(),
                rootClasses,
                serviceClassLoader,
                wsdlContent
            );
            result.soapEndpoints.addAll(soapEndpoints);
            saveEndpointMappings(serviceDefinition, soapEndpoints);

            // Generate and register WebSocket endpoints (enabled by default)
            List<EndpointMapping> webSocketEndpoints = webSocketServiceGenerator.generateWebSocketEndpoints(
                schema.getServiceName(),
                rootClasses
            );
            result.webSocketEndpoints.addAll(webSocketEndpoints);
            saveEndpointMappings(serviceDefinition, webSocketEndpoints);
            serviceDefinition.setWebsocketEnabled(true);

            // Update service status
            serviceDefinition.setStatus(ServiceDefinition.ServiceStatus.DEPLOYED);
            serviceDefinition.setDeployedAt(LocalDateTime.now());
            serviceDefinition.setLastHealthCheck(LocalDateTime.now());
            serviceRepository.save(serviceDefinition);

            result.success = true;
            result.message = String.format("Successfully deployed service with %d REST endpoints, %d SOAP endpoints, and %d WebSocket endpoints",
                restEndpoints.size(), soapEndpoints.size(), webSocketEndpoints.size());

            log.info("Service deployment completed successfully: {}", result.message);

        } catch (Exception e) {
            log.error("Service deployment failed for schema ID: {}", schemaId, e);
            result.success = false;
            result.message = "Deployment failed: " + e.getMessage();
            result.error = e;

            // Update service status to FAILED if exists
            serviceRepository.findFirstBySchemaMetadataId(schemaId).ifPresent(service -> {
                service.setStatus(ServiceDefinition.ServiceStatus.DEPLOYMENT_FAILED);
                serviceRepository.save(service);
            });
        }

        return result;
    }

    /**
     * Undeploys a service and removes all its endpoints.
     */
    @Transactional
    public void undeployService(String schemaId) {
        log.info("Starting service undeployment for schema ID: {}", schemaId);

        SchemaMetadata schema = schemaRepository.findById(schemaId)
            .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaId));

        ServiceDefinition service = serviceRepository.findFirstBySchemaMetadataId(schemaId)
            .orElse(null);

        if (service == null) {
            log.warn("No service definition found for schema ID: {}", schemaId);
            return;
        }

        try {
            // Unregister REST endpoints
            List<EndpointMapping> restEndpoints = endpointRepository
                .findByServiceDefinitionAndEndpointType(service, EndpointType.REST);
            restEndpointGenerator.unregisterEndpoints(restEndpoints);

            // Unregister SOAP endpoints
            List<EndpointMapping> soapEndpoints = endpointRepository
                .findByServiceDefinitionAndEndpointType(service, EndpointType.SOAP);
            soapServiceGenerator.unregisterEndpoints(soapEndpoints);

            // Unregister WebSocket endpoints
            List<EndpointMapping> webSocketEndpoints = endpointRepository
                .findByServiceDefinitionAndEndpointType(service, EndpointType.WEBSOCKET);
            webSocketServiceGenerator.unregisterEndpoints(webSocketEndpoints);

            // Delete endpoint mappings
            endpointRepository.deleteAll(restEndpoints);
            endpointRepository.deleteAll(soapEndpoints);
            endpointRepository.deleteAll(webSocketEndpoints);

            // Remove class loader
            classLoaderManager.removeClassLoader(schema.getServiceName());

            // Update service status
            service.setStatus(ServiceDefinition.ServiceStatus.UNDEPLOYED);
            service.setUndeployedAt(LocalDateTime.now());
            serviceRepository.save(service);

            log.info("Service undeployment completed successfully for schema ID: {}", schemaId);

        } catch (Exception e) {
            log.error("Service undeployment failed for schema ID: {}", schemaId, e);
            throw new RuntimeException("Failed to undeploy service", e);
        }
    }

    /**
     * Redeploys a service (undeploy + deploy).
     */
    @Transactional
    public DeploymentResult redeployService(String schemaId) {
        log.info("Redeploying service for schema ID: {}", schemaId);
        undeployService(schemaId);
        return deployService(schemaId);
    }

    /**
     * Gets deployment status for a schema.
     */
    public DeploymentStatus getDeploymentStatus(String schemaId) {
        SchemaMetadata schema = schemaRepository.findById(schemaId)
            .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaId));

        ServiceDefinition service = serviceRepository.findFirstBySchemaMetadataId(schemaId)
            .orElse(null);

        DeploymentStatus status = new DeploymentStatus();
        status.schemaId = schemaId;
        status.serviceName = schema.getServiceName();
        status.schemaStatus = schema.getStatus();

        if (service != null) {
            status.serviceStatus = service.getStatus();
            status.deployedAt = service.getDeployedAt();
            status.endpointCount = endpointRepository.countByServiceDefinition(service);
            status.restEndpointCount = endpointRepository
                .countByServiceDefinitionAndEndpointType(service, EndpointType.REST);
            status.soapEndpointCount = endpointRepository
                .countByServiceDefinitionAndEndpointType(service, EndpointType.SOAP);
            status.webSocketEndpointCount = endpointRepository
                .countByServiceDefinitionAndEndpointType(service, EndpointType.WEBSOCKET);
            status.webSocketEnabled = service.getWebsocketEnabled();
        }

        return status;
    }

    /**
     * Finds root classes from compiled output that should be exposed as services.
     * These are typically classes annotated with @XmlRootElement or referenced in ObjectFactory with @XmlElementDecl.
     */
    private Set<Class<?>> findRootClasses(SchemaMetadata schema, ClassLoader classLoader) {
        Set<Class<?>> rootClasses = new java.util.HashSet<>();

        try {
            String packageName = schema.getPackageName();
            java.nio.file.Path classOutputPath = java.nio.file.Paths.get(schema.getClassOutputPath());

            if (!java.nio.file.Files.exists(classOutputPath)) {
                return rootClasses;
            }

            // First, try to find ObjectFactory and extract root elements from @XmlElementDecl
            try {
                String objectFactoryClass = packageName + ".ObjectFactory";
                Class<?> objectFactory = classLoader.loadClass(objectFactoryClass);

                // Get all methods annotated with @XmlElementDecl
                for (java.lang.reflect.Method method : objectFactory.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(jakarta.xml.bind.annotation.XmlElementDecl.class)) {
                        // The return type is JAXBElement<T>, extract T
                        java.lang.reflect.Type returnType = method.getGenericReturnType();
                        if (returnType instanceof java.lang.reflect.ParameterizedType) {
                            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) returnType;
                            java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                                Class<?> rootClass = (Class<?>) typeArgs[0];
                                rootClasses.add(rootClass);
                                log.debug("Found root class from ObjectFactory: {}", rootClass.getName());
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                log.debug("No ObjectFactory found for package: {}", packageName);
            }

            // Also scan for classes with @XmlRootElement (less common with xjc)
            java.nio.file.Files.walk(classOutputPath)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(classFile -> {
                    try {
                        String className = getClassNameFromFile(classOutputPath, classFile, packageName);
                        Class<?> clazz = classLoader.loadClass(className);

                        // Check if class has @XmlRootElement annotation
                        if (clazz.isAnnotationPresent(jakarta.xml.bind.annotation.XmlRootElement.class)) {
                            rootClasses.add(clazz);
                            log.debug("Found root class with @XmlRootElement: {}", className);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load class from file: {}", classFile, e);
                    }
                });

        } catch (Exception e) {
            log.error("Failed to find root classes for schema: {}", schema.getServiceName(), e);
        }

        return rootClasses;
    }

    /**
     * Converts a class file path to fully qualified class name.
     */
    private String getClassNameFromFile(java.nio.file.Path basePath, java.nio.file.Path classFile, String packageName) {
        String relativePath = basePath.relativize(classFile).toString();
        String className = relativePath.replace(java.io.File.separator, ".")
            .replace(".class", "");
        return className;
    }

    /**
     * Creates or updates service definition entity.
     */
    private ServiceDefinition createOrUpdateServiceDefinition(SchemaMetadata schema) {
        return serviceRepository.findFirstBySchemaMetadataId(schema.getId())
            .map(existing -> {
                existing.setStatus(ServiceDefinition.ServiceStatus.DEPLOYING);
                return existing;
            })
            .orElseGet(() -> {
                ServiceDefinition newService = new ServiceDefinition();
                newService.setSchemaMetadata(schema);
                newService.setServiceName(schema.getServiceName());
                newService.setVersion(schema.getVersion());
                newService.setStatus(ServiceDefinition.ServiceStatus.DEPLOYING);
                return newService;
            });
    }

    /**
     * Saves endpoint mappings to database.
     */
    private void saveEndpointMappings(ServiceDefinition service, List<EndpointMapping> endpoints) {
        endpoints.forEach(endpoint -> {
            endpoint.setServiceDefinition(service);
            endpointRepository.save(endpoint);
        });
    }

    /**
     * Result of a deployment operation.
     */
    public static class DeploymentResult {
        public boolean success;
        public String message;
        public String schemaId;
        public String serviceName;
        public List<EndpointMapping> restEndpoints = new ArrayList<>();
        public List<EndpointMapping> soapEndpoints = new ArrayList<>();
        public List<EndpointMapping> webSocketEndpoints = new ArrayList<>();
        public Exception error;
    }

    /**
     * Status of a deployed service.
     */
    public static class DeploymentStatus {
        public String schemaId;
        public String serviceName;
        public SchemaMetadata.SchemaStatus schemaStatus;
        public ServiceDefinition.ServiceStatus serviceStatus;
        public LocalDateTime deployedAt;
        public Long endpointCount;
        public Long restEndpointCount;
        public Long soapEndpointCount;
        public Long webSocketEndpointCount;
        public Boolean webSocketEnabled;
    }
}
