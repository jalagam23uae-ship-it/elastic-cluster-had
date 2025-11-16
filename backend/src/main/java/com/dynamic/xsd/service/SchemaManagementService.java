package com.dynamic.xsd.service;

import com.dynamic.xsd.config.DynamicServiceProperties;
import com.dynamic.xsd.domain.entity.AuditLog;
import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.dto.*;
import com.dynamic.xsd.repository.AuditLogRepository;
import com.dynamic.xsd.repository.SchemaMetadataRepository;
import com.dynamic.xsd.service.classloader.ClassLoaderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing XSD schemas.
 *
 * Orchestrates:
 * - Schema upload and validation
 * - POJO generation from XSD
 * - Dynamic compilation
 * - Class loading
 * - Metadata management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaManagementService {

    private final SchemaMetadataRepository schemaMetadataRepository;
    private final AuditLogRepository auditLogRepository;
    private final XsdValidatorService xsdValidatorService;
    private final PojoGeneratorService pojoGeneratorService;
    private final DynamicCompilerService dynamicCompilerService;
    private final ClassLoaderManager classLoaderManager;
    private final DynamicServiceProperties properties;

    /**
     * Uploads and processes an XSD schema.
     */
    @Transactional
    public SchemaUploadResponse uploadSchema(MultipartFile file, SchemaUploadRequest request,
                                            String uploadedBy) {
        log.info("Starting schema upload for service: {}", request.getServiceName());

        try {
            // Validate file
            validateFile(file);

            // Check if service name already exists
            if (schemaMetadataRepository.existsByServiceName(request.getServiceName())) {
                throw new IllegalArgumentException("Service name already exists: " + request.getServiceName());
            }

            byte[] xsdContent = file.getBytes();

            // Create schema metadata entity
            SchemaMetadata metadata = SchemaMetadata.builder()
                .serviceName(request.getServiceName())
                .version(request.getVersion())
                .description(request.getDescription())
                .uploadedBy(uploadedBy)
                .status(SchemaMetadata.SchemaStatus.VALIDATING)
                .xsdContent(new String(xsdContent))
                .build();

            // Save initial metadata
            metadata = schemaMetadataRepository.save(metadata);

            // Validate XSD
            XsdValidatorService.ValidationResult validationResult =
                xsdValidatorService.validate(xsdContent);

            if (!validationResult.isValid()) {
                metadata.setStatus(SchemaMetadata.SchemaStatus.VALIDATION_FAILED);
                metadata.setValidationErrors(String.join("; ", validationResult.getErrors()));
                schemaMetadataRepository.save(metadata);

                logAudit(uploadedBy, AuditLog.AuditAction.SCHEMA_UPLOAD, request.getServiceName(),
                    AuditLog.AuditStatus.FAILED, "Validation failed");

                return buildUploadResponse(metadata, validationResult);
            }

            // Set namespace from validation result
            if (request.getNamespace() == null && validationResult.getTargetNamespace() != null) {
                metadata.setNamespace(validationResult.getTargetNamespace());
            } else {
                metadata.setNamespace(request.getNamespace());
            }

            // Store XSD file persistently (optional)
            String xsdFilePath = storeXsdFile(request.getServiceName(), xsdContent);
            metadata.setXsdFilePath(xsdFilePath);

            // Generate POJOs
            metadata.setStatus(SchemaMetadata.SchemaStatus.GENERATING);
            metadata = schemaMetadataRepository.save(metadata);

            PojoGeneratorService.GenerationResult generationResult =
                pojoGeneratorService.generate(request.getServiceName(), xsdContent);

            if (!generationResult.isSuccess()) {
                metadata.setStatus(SchemaMetadata.SchemaStatus.GENERATION_FAILED);
                metadata.setCompilationErrors(String.join("; ", generationResult.getErrors()));
                schemaMetadataRepository.save(metadata);

                logAudit(uploadedBy, AuditLog.AuditAction.SCHEMA_UPLOAD, request.getServiceName(),
                    AuditLog.AuditStatus.FAILED, "POJO generation failed");

                return buildUploadResponseFromGeneration(metadata, generationResult);
            }

            // Compile generated sources
            metadata.setStatus(SchemaMetadata.SchemaStatus.COMPILING);
            metadata = schemaMetadataRepository.save(metadata);

            DynamicCompilerService.CompilationResult compilationResult =
                dynamicCompilerService.compile(request.getServiceName(),
                    generationResult.getSourceOutputDirectory());

            if (!compilationResult.isSuccess()) {
                metadata.setStatus(SchemaMetadata.SchemaStatus.COMPILATION_FAILED);
                metadata.setCompilationErrors(String.join("; ", compilationResult.getErrors()));
                schemaMetadataRepository.save(metadata);

                logAudit(uploadedBy, AuditLog.AuditAction.SCHEMA_UPLOAD, request.getServiceName(),
                    AuditLog.AuditStatus.FAILED, "Compilation failed");

                return buildUploadResponseFromCompilation(metadata, compilationResult);
            }

            // Load compiled classes
            classLoaderManager.getOrCreateClassLoader(request.getServiceName(),
                compilationResult.getClassOutputDirectory());

            // Update metadata with success
            metadata.setStatus(SchemaMetadata.SchemaStatus.ACTIVE);
            metadata.setGeneratedPojos(compilationResult.getCompiledClasses());
            metadata = schemaMetadataRepository.save(metadata);

            logAudit(uploadedBy, AuditLog.AuditAction.SCHEMA_UPLOAD, request.getServiceName(),
                AuditLog.AuditStatus.SUCCESS, "Schema uploaded and processed successfully");

            log.info("Schema upload completed successfully for service: {}", request.getServiceName());

            return SchemaUploadResponse.builder()
                .id(metadata.getId())
                .serviceName(metadata.getServiceName())
                .version(metadata.getVersion())
                .namespace(metadata.getNamespace())
                .status(metadata.getStatus().name())
                .uploadedAt(metadata.getUploadedAt())
                .validationWarnings(validationResult.getWarnings())
                .message("Schema uploaded and processed successfully")
                .build();

        } catch (Exception e) {
            log.error("Schema upload failed for service: {}", request.getServiceName(), e);

            logAudit(uploadedBy, AuditLog.AuditAction.SCHEMA_UPLOAD, request.getServiceName(),
                AuditLog.AuditStatus.FAILED, e.getMessage());

            throw new RuntimeException("Schema upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets schema metadata by service name.
     */
    public SchemaMetadataDto getSchemaByServiceName(String serviceName) {
        SchemaMetadata metadata = schemaMetadataRepository.findByServiceName(serviceName)
            .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + serviceName));

        return convertToDto(metadata);
    }

    /**
     * Lists all schemas with optional filtering.
     */
    public Page<SchemaMetadataDto> listSchemas(SchemaMetadata.SchemaStatus status,
                                               String search, Pageable pageable) {
        Page<SchemaMetadata> schemas = schemaMetadataRepository.findByFilters(status, search, pageable);
        return schemas.map(this::convertToDto);
    }

    /**
     * Deletes a schema.
     */
    @Transactional
    public void deleteSchema(String serviceName, String deletedBy) {
        log.info("Deleting schema for service: {}", serviceName);

        SchemaMetadata metadata = schemaMetadataRepository.findByServiceName(serviceName)
            .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + serviceName));

        // Remove classloader
        classLoaderManager.removeClassLoader(serviceName);

        // Delete XSD file
        if (metadata.getXsdFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(metadata.getXsdFilePath()));
            } catch (IOException e) {
                log.warn("Failed to delete XSD file: {}", metadata.getXsdFilePath(), e);
            }
        }

        // Delete metadata
        schemaMetadataRepository.delete(metadata);

        logAudit(deletedBy, AuditLog.AuditAction.SCHEMA_DELETE, serviceName,
            AuditLog.AuditStatus.SUCCESS, "Schema deleted");

        log.info("Schema deleted successfully for service: {}", serviceName);
    }

    /**
     * Validates uploaded file.
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xsd")) {
            throw new IllegalArgumentException("File must be an XSD file");
        }

        if (file.getSize() > properties.getXsd().getMaxFileSize()) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }
    }

    /**
     * Stores XSD file persistently.
     */
    private String storeXsdFile(String serviceName, byte[] content) throws IOException {
        Path storagePath = Paths.get(properties.getXsd().getStoragePath());
        Files.createDirectories(storagePath);

        Path filePath = storagePath.resolve(serviceName + ".xsd");
        Files.write(filePath, content);

        log.debug("Stored XSD file: {}", filePath);
        return filePath.toString();
    }

    /**
     * Converts entity to DTO.
     */
    private SchemaMetadataDto convertToDto(SchemaMetadata metadata) {
        return SchemaMetadataDto.builder()
            .id(metadata.getId())
            .serviceName(metadata.getServiceName())
            .version(metadata.getVersion())
            .namespace(metadata.getNamespace())
            .description(metadata.getDescription())
            .status(metadata.getStatus().name())
            .uploadedBy(metadata.getUploadedBy())
            .uploadedAt(metadata.getUploadedAt())
            .updatedAt(metadata.getUpdatedAt())
            .generatedArtifacts(SchemaMetadataDto.GeneratedArtifactsDto.builder()
                .pojos(metadata.getGeneratedPojos())
                .restEndpointCount(metadata.getRestEndpointCount())
                .soapOperationCount(metadata.getSoapOperationCount())
                .build())
            .validationErrors(metadata.getValidationErrors())
            .compilationErrors(metadata.getCompilationErrors())
            .build();
    }

    /**
     * Builds upload response from validation result.
     */
    private SchemaUploadResponse buildUploadResponse(SchemaMetadata metadata,
                                                     XsdValidatorService.ValidationResult result) {
        return SchemaUploadResponse.builder()
            .id(metadata.getId())
            .serviceName(metadata.getServiceName())
            .version(metadata.getVersion())
            .namespace(metadata.getNamespace())
            .status(metadata.getStatus().name())
            .uploadedAt(metadata.getUploadedAt())
            .validationErrors(result.getErrors())
            .validationWarnings(result.getWarnings())
            .message("Schema validation failed")
            .build();
    }

    /**
     * Builds upload response from generation result.
     */
    private SchemaUploadResponse buildUploadResponseFromGeneration(SchemaMetadata metadata,
                                                                   PojoGeneratorService.GenerationResult result) {
        return SchemaUploadResponse.builder()
            .id(metadata.getId())
            .serviceName(metadata.getServiceName())
            .version(metadata.getVersion())
            .status(metadata.getStatus().name())
            .uploadedAt(metadata.getUploadedAt())
            .validationErrors(result.getErrors())
            .validationWarnings(result.getWarnings())
            .message("POJO generation failed")
            .build();
    }

    /**
     * Builds upload response from compilation result.
     */
    private SchemaUploadResponse buildUploadResponseFromCompilation(SchemaMetadata metadata,
                                                                    DynamicCompilerService.CompilationResult result) {
        return SchemaUploadResponse.builder()
            .id(metadata.getId())
            .serviceName(metadata.getServiceName())
            .version(metadata.getVersion())
            .status(metadata.getStatus().name())
            .uploadedAt(metadata.getUploadedAt())
            .validationErrors(result.getErrors())
            .validationWarnings(result.getWarnings())
            .message("Compilation failed")
            .build();
    }

    /**
     * Logs audit entry.
     */
    private void logAudit(String username, AuditLog.AuditAction action, String serviceName,
                         AuditLog.AuditStatus status, String details) {
        AuditLog auditLog = AuditLog.builder()
            .username(username)
            .action(action)
            .serviceName(serviceName)
            .status(status)
            .details(details)
            .build();

        auditLogRepository.save(auditLog);
    }
}
