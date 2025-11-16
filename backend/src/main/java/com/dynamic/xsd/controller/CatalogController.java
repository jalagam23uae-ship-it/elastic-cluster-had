package com.dynamic.xsd.controller;

import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.dto.ApiResponse;
import com.dynamic.xsd.repository.EndpointMappingRepository;
import com.dynamic.xsd.repository.SchemaMetadataRepository;
import com.dynamic.xsd.repository.ServiceDefinitionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for browsing available services and schemas.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Service catalog and discovery APIs")
public class CatalogController {

    private final SchemaMetadataRepository schemaRepository;
    private final ServiceDefinitionRepository serviceRepository;
    private final EndpointMappingRepository endpointRepository;

    @GetMapping("/services")
    @Operation(summary = "List All Services", description = "Get list of all deployed services")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ServiceCatalogEntry>>> listServices() {

        log.debug("List all services request");

        List<ServiceDefinition> services = serviceRepository.findByStatus(ServiceDefinition.ServiceStatus.DEPLOYED);

        List<ServiceCatalogEntry> catalog = services.stream()
            .map(service -> {
                long endpointCount = endpointRepository.countByServiceDefinition(service);
                long restEndpoints = endpointRepository.countByServiceDefinitionAndEndpointType(
                    service, com.dynamic.xsd.domain.enums.EndpointType.REST);
                long soapEndpoints = endpointRepository.countByServiceDefinitionAndEndpointType(
                    service, com.dynamic.xsd.domain.enums.EndpointType.SOAP);

                return ServiceCatalogEntry.builder()
                    .id(service.getId())
                    .serviceName(service.getServiceName())
                    .version(service.getVersion())
                    .status(service.getStatus().name())
                    .deployedAt(service.getDeployedAt())
                    .totalEndpoints(endpointCount)
                    .restEndpoints(restEndpoints)
                    .soapEndpoints(soapEndpoints)
                    .totalRequests(service.getTotalRequests() != null ? service.getTotalRequests() : 0)
                    .averageResponseTime(service.getAverageResponseTime())
                    .build();
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(catalog, "Services retrieved"));
    }

    @GetMapping("/services/{serviceName}")
    @Operation(summary = "Get Service Details", description = "Get detailed information about a service")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceDetails(
            @PathVariable String serviceName) {

        log.debug("Get service details for: {}", serviceName);

        ServiceDefinition service = serviceRepository.findByServiceName(serviceName)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));

        SchemaMetadata schema = service.getSchemaMetadata();
        if (schema == null) {
            throw new IllegalArgumentException("Schema not found");
        }

        // Build service DTO
        Map<String, Object> serviceDto = new HashMap<>();
        serviceDto.put("id", service.getId());
        serviceDto.put("serviceName", service.getServiceName());
        serviceDto.put("version", service.getVersion());
        serviceDto.put("status", service.getStatus().name());
        serviceDto.put("deployedAt", service.getDeployedAt());
        serviceDto.put("deployedBy", service.getDeployedBy());
        serviceDto.put("totalRequests", service.getTotalRequests());
        serviceDto.put("successfulRequests", service.getSuccessfulRequests());
        serviceDto.put("failedRequests", service.getFailedRequests());
        serviceDto.put("averageResponseTime", service.getAverageResponseTime());

        // Build schema DTO
        Map<String, Object> schemaDto = new HashMap<>();
        schemaDto.put("id", schema.getId());
        schemaDto.put("serviceName", schema.getServiceName());
        schemaDto.put("version", schema.getVersion());
        schemaDto.put("namespace", schema.getNamespace());
        schemaDto.put("targetNamespace", schema.getTargetNamespace());
        schemaDto.put("status", schema.getStatus().name());
        schemaDto.put("uploadedAt", schema.getUploadedAt());
        schemaDto.put("uploadedBy", schema.getUploadedBy());
        schemaDto.put("generatedPojos", schema.getGeneratedPojos());

        // Build endpoint DTOs
        List<Map<String, Object>> endpointDtos = endpointRepository.findByServiceDefinitionId(service.getId()).stream()
            .map(endpoint -> {
                Map<String, Object> endpointDto = new HashMap<>();
                endpointDto.put("id", endpoint.getId());
                endpointDto.put("type", endpoint.getEndpointType().name());
                endpointDto.put("path", endpoint.getPath());
                endpointDto.put("httpMethod", endpoint.getHttpMethod() != null ? endpoint.getHttpMethod().name() : null);
                endpointDto.put("operationName", endpoint.getOperationName());
                endpointDto.put("description", endpoint.getDescription());
                endpointDto.put("active", endpoint.getActive());
                return endpointDto;
            })
            .collect(Collectors.toList());

        Map<String, Object> details = new HashMap<>();
        details.put("service", serviceDto);
        details.put("schema", schemaDto);
        details.put("endpoints", endpointDtos);
        details.put("wsdlAvailable", service.getWsdlContent() != null && !service.getWsdlContent().isEmpty());

        return ResponseEntity.ok(ApiResponse.success(details, "Service details retrieved"));
    }

    @GetMapping("/schemas")
    @Operation(summary = "List All Schemas", description = "Get list of all schemas")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<SchemaCatalogEntry>>> listSchemas(
            @RequestParam(required = false) SchemaMetadata.SchemaStatus status) {

        log.debug("List schemas request - status: {}", status);

        List<SchemaMetadata> schemas;
        if (status != null) {
            schemas = schemaRepository.findByStatus(status);
        } else {
            schemas = schemaRepository.findAll();
        }

        List<SchemaCatalogEntry> catalog = schemas.stream()
            .map(schema -> {
                boolean deployed = serviceRepository.findBySchemaMetadataId(schema.getId()).stream()
                    .anyMatch(s -> s.getStatus() == ServiceDefinition.ServiceStatus.DEPLOYED);

                return SchemaCatalogEntry.builder()
                    .id(schema.getId())
                    .serviceName(schema.getServiceName())
                    .version(schema.getVersion())
                    .status(schema.getStatus().name())
                    .uploadedAt(schema.getUploadedAt())
                    .uploadedBy(schema.getUploadedBy())
                    .targetNamespace(schema.getTargetNamespace())
                    .generatedPojos(schema.getGeneratedPojos())
                    .deployed(deployed)
                    .build();
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(catalog, "Schemas retrieved"));
    }

    @GetMapping("/endpoints")
    @Operation(summary = "List All Endpoints", description = "Get list of all available endpoints")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAllEndpoints() {

        log.debug("List all endpoints request");

        List<Map<String, Object>> endpoints = endpointRepository.findAll().stream()
            .filter(endpoint -> endpoint.getActive())
            .map(endpoint -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", endpoint.getId());
                map.put("type", endpoint.getEndpointType().name());
                map.put("path", endpoint.getPath());
                map.put("httpMethod", endpoint.getHttpMethod() != null ? endpoint.getHttpMethod().name() : null);
                map.put("operationName", endpoint.getOperationName());
                map.put("description", endpoint.getDescription());
                map.put("serviceId", endpoint.getServiceDefinition().getId());
                map.put("serviceName", endpoint.getServiceDefinition().getServiceName());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(endpoints, "Endpoints retrieved"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Services", description = "Search services by name or description")
    public ResponseEntity<ApiResponse<List<ServiceCatalogEntry>>> searchServices(
            @RequestParam String query) {

        log.debug("Search services: {}", query);

        List<ServiceDefinition> services = serviceRepository.findAll().stream()
            .filter(s -> s.getServiceName().toLowerCase().contains(query.toLowerCase())
                      && s.getStatus() == ServiceDefinition.ServiceStatus.DEPLOYED)
            .collect(Collectors.toList());

        List<ServiceCatalogEntry> results = services.stream()
            .map(service -> ServiceCatalogEntry.builder()
                .id(service.getId())
                .serviceName(service.getServiceName())
                .version(service.getVersion())
                .status(service.getStatus().name())
                .deployedAt(service.getDeployedAt())
                .build())
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(results, "Search results"));
    }

    /**
     * Service catalog entry DTO.
     */
    @lombok.Builder
    @lombok.Data
    public static class ServiceCatalogEntry {
        private String id;
        private String serviceName;
        private String version;
        private String status;
        private java.time.LocalDateTime deployedAt;
        private Long totalEndpoints;
        private Long restEndpoints;
        private Long soapEndpoints;
        private Long totalRequests;
        private Double averageResponseTime;
    }

    /**
     * Schema catalog entry DTO.
     */
    @lombok.Builder
    @lombok.Data
    public static class SchemaCatalogEntry {
        private String id;
        private String serviceName;
        private String version;
        private String status;
        private java.time.LocalDateTime uploadedAt;
        private String uploadedBy;
        private String targetNamespace;
        private java.util.List<String> generatedPojos;
        private boolean deployed;
    }
}
