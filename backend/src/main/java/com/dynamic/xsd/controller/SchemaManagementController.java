package com.dynamic.xsd.controller;

import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.dto.SchemaMetadataDto;
import com.dynamic.xsd.dto.SchemaUploadRequest;
import com.dynamic.xsd.dto.SchemaUploadResponse;
import com.dynamic.xsd.service.SchemaManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for Schema Management operations.
 *
 * Endpoints:
 * - POST /api/v1/schema/upload - Upload XSD schema
 * - GET /api/v1/schema/{serviceName} - Get schema by service name
 * - GET /api/v1/schema/list - List all schemas
 * - DELETE /api/v1/schema/{serviceName} - Delete schema
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schema")
@RequiredArgsConstructor
@Tag(name = "Schema Management", description = "APIs for managing XSD schemas")
public class SchemaManagementController {

    private final SchemaManagementService schemaManagementService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload XSD schema",
              description = "Uploads an XSD schema file and generates POJOs, compiles them, and loads classes dynamically")
    public ResponseEntity<SchemaUploadResponse> uploadSchema(
            @Parameter(description = "XSD file to upload", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Service name (unique, lowercase, alphanumeric with hyphens)", required = true)
            @RequestParam("serviceName") String serviceName,

            @Parameter(description = "Version (x.y or x.y.z format)")
            @RequestParam(value = "version", defaultValue = "1.0") String version,

            @Parameter(description = "Service description")
            @RequestParam(value = "description", required = false) String description,

            @Parameter(description = "Target namespace")
            @RequestParam(value = "namespace", required = false) String namespace,

            @Parameter(description = "Auto-deploy after upload")
            @RequestParam(value = "autoDeploy", defaultValue = "false") Boolean autoDeploy
    ) {
        log.info("Received schema upload request for service: {}", serviceName);

        SchemaUploadRequest request = SchemaUploadRequest.builder()
            .serviceName(serviceName)
            .version(version)
            .description(description)
            .namespace(namespace)
            .autoDeploy(autoDeploy)
            .build();

        // TODO: Get username from security context
        String uploadedBy = "admin";

        SchemaUploadResponse response = schemaManagementService.uploadSchema(file, request, uploadedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{serviceName}")
    @Operation(summary = "Get schema by service name",
              description = "Retrieves schema metadata for a specific service")
    public ResponseEntity<SchemaMetadataDto> getSchema(
            @Parameter(description = "Service name", required = true)
            @PathVariable String serviceName
    ) {
        log.info("Retrieving schema for service: {}", serviceName);

        SchemaMetadataDto schema = schemaManagementService.getSchemaByServiceName(serviceName);
        return ResponseEntity.ok(schema);
    }

    @GetMapping("/list")
    @Operation(summary = "List all schemas",
              description = "Retrieves a paginated list of all schemas with optional filtering")
    public ResponseEntity<Page<SchemaMetadataDto>> listSchemas(
            @Parameter(description = "Filter by status")
            @RequestParam(value = "status", required = false) SchemaMetadata.SchemaStatus status,

            @Parameter(description = "Search by service name")
            @RequestParam(value = "search", required = false) String search,

            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Listing schemas with status: {}, search: {}", status, search);

        Page<SchemaMetadataDto> schemas = schemaManagementService.listSchemas(status, search, pageable);
        return ResponseEntity.ok(schemas);
    }

    @DeleteMapping("/{serviceName}")
    @Operation(summary = "Delete schema",
              description = "Deletes a schema and all associated resources")
    public ResponseEntity<Void> deleteSchema(
            @Parameter(description = "Service name", required = true)
            @PathVariable String serviceName
    ) {
        log.info("Deleting schema for service: {}", serviceName);

        // TODO: Get username from security context
        String deletedBy = "admin";

        schemaManagementService.deleteSchema(serviceName, deletedBy);

        return ResponseEntity.noContent().build();
    }
}
