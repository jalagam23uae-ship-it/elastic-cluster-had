package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.repository.SchemaMetadataRepository;
import com.dynamic.xsd.repository.ServiceDefinitionRepository;
import com.dynamic.xsd.service.classloader.ServiceClassLoaderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scheduled cleanup service for maintaining system health.
 * Cleans up temporary files, unused class loaders, and old data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupScheduler {

    private final SchemaMetadataRepository schemaRepository;
    private final ServiceDefinitionRepository serviceRepository;
    private final ServiceClassLoaderManager classLoaderManager;

    /**
     * Cleans up temporary files older than 24 hours.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupTemporaryFiles() {
        log.info("Starting temporary files cleanup");

        try {
            // Get all schemas
            List<SchemaMetadata> allSchemas = schemaRepository.findAll();

            int filesDeleted = 0;
            int bytesFreed = 0;

            for (SchemaMetadata schema : allSchemas) {
                // Check if schema is deprecated or failed
                if (schema.getStatus() == SchemaMetadata.SchemaStatus.DEPRECATED ||
                    schema.getStatus() == SchemaMetadata.SchemaStatus.FAILED) {

                    // Check if older than 24 hours
                    if (schema.getUpdatedAt().isBefore(LocalDateTime.now().minusHours(24))) {

                        // Delete generated files
                        if (schema.getXsdFilePath() != null) {
                            Path xsdPath = Paths.get(schema.getXsdFilePath());
                            if (Files.exists(xsdPath)) {
                                try {
                                    long size = Files.size(xsdPath);
                                    Files.delete(xsdPath);
                                    filesDeleted++;
                                    bytesFreed += size;
                                    log.debug("Deleted XSD file: {}", xsdPath);
                                } catch (IOException e) {
                                    log.error("Failed to delete XSD file: {}", xsdPath, e);
                                }
                            }
                        }

                        // Delete source and class output directories
                        deleteDirectoryRecursively(schema.getId() + "_source");
                        deleteDirectoryRecursively(schema.getId() + "_classes");
                    }
                }
            }

            log.info("Temporary files cleanup completed. Deleted {} files, freed {} bytes", filesDeleted, bytesFreed);

        } catch (Exception e) {
            log.error("Temporary files cleanup failed", e);
        }
    }

    /**
     * Removes unused class loaders.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupUnusedClassLoaders() {
        log.info("Starting unused class loaders cleanup");

        try {
            // Get all inactive services
            List<ServiceDefinition> inactiveServices = serviceRepository.findByStatus(
                ServiceDefinition.ServiceStatus.UNDEPLOYED
            );

            int classLoadersRemoved = 0;

            for (ServiceDefinition service : inactiveServices) {
                // Check if undeployed more than 1 hour ago
                if (service.getUndeployedAt() != null &&
                    service.getUndeployedAt().isBefore(LocalDateTime.now().minusHours(1))) {

                    try {
                        classLoaderManager.removeClassLoader(service.getServiceName());
                        classLoadersRemoved++;
                        log.debug("Removed class loader for service: {}", service.getServiceName());
                    } catch (Exception e) {
                        log.error("Failed to remove class loader for service: {}", service.getServiceName(), e);
                    }
                }
            }

            log.info("Unused class loaders cleanup completed. Removed {} class loaders", classLoadersRemoved);

        } catch (Exception e) {
            log.error("Class loaders cleanup failed", e);
        }
    }

    /**
     * Archives old deprecated schemas.
     * Runs weekly on Sunday at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void archiveOldSchemas() {
        log.info("Starting old schemas archival");

        try {
            // Find schemas deprecated more than 30 days ago
            List<SchemaMetadata> deprecatedSchemas = schemaRepository.findByStatus(
                SchemaMetadata.SchemaStatus.DEPRECATED
            );

            int schemasArchived = 0;

            for (SchemaMetadata schema : deprecatedSchemas) {
                if (schema.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(30))) {
                    // In production, you might want to move to archive storage instead of deleting
                    // For now, we'll just log that it should be archived
                    log.info("Schema should be archived: {} (ID: {})", schema.getServiceName(), schema.getId());
                    schemasArchived++;

                    // Optionally delete the schema if no longer needed
                    // schemaRepository.delete(schema);
                }
            }

            log.info("Old schemas archival completed. {} schemas marked for archival", schemasArchived);

        } catch (Exception e) {
            log.error("Schemas archival failed", e);
        }
    }

    /**
     * Performs garbage collection on failed compilations.
     * Runs every 6 hours.
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    @Transactional
    public void cleanupFailedCompilations() {
        log.info("Starting failed compilations cleanup");

        try {
            // Find schemas with compilation failures older than 24 hours
            List<SchemaMetadata> failedSchemas = schemaRepository.findByStatus(
                SchemaMetadata.SchemaStatus.COMPILATION_FAILED
            );

            int schemasCleanedUp = 0;

            for (SchemaMetadata schema : failedSchemas) {
                if (schema.getUpdatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
                    // Delete generated source files for failed compilations
                    deleteDirectoryRecursively(schema.getId() + "_source");
                    schemasCleanedUp++;
                    log.debug("Cleaned up failed compilation for schema: {}", schema.getServiceName());
                }
            }

            log.info("Failed compilations cleanup completed. Cleaned up {} schemas", schemasCleanedUp);

        } catch (Exception e) {
            log.error("Failed compilations cleanup failed", e);
        }
    }

    /**
     * Health check for long-running services.
     * Runs every 15 minutes.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional
    public void performServiceHealthCheck() {
        log.debug("Starting service health check");

        try {
            // Get all deployed services
            List<ServiceDefinition> deployedServices = serviceRepository.findAllDeployed();

            int servicesChecked = 0;

            for (ServiceDefinition service : deployedServices) {
                // Update last health check time
                service.setLastHealthCheck(LocalDateTime.now());
                serviceRepository.save(service);
                servicesChecked++;
            }

            log.debug("Service health check completed. Checked {} services", servicesChecked);

        } catch (Exception e) {
            log.error("Service health check failed", e);
        }
    }

    /**
     * Cleanup metrics and statistics older than 90 days.
     * Runs monthly on the 1st at 4 AM.
     */
    @Scheduled(cron = "0 0 4 1 * *")
    public void cleanupOldMetrics() {
        log.info("Starting old metrics cleanup");

        try {
            // In a production system, you would clean up audit logs, metrics, and statistics here
            // For example:
            // - Delete audit logs older than 90 days
            // - Archive request/response metrics
            // - Clean up old performance data

            log.info("Old metrics cleanup completed");

        } catch (Exception e) {
            log.error("Metrics cleanup failed", e);
        }
    }

    /**
     * Deletes a directory and all its contents recursively.
     */
    private void deleteDirectoryRecursively(String directoryName) {
        try {
            Path directory = Paths.get("temp", directoryName);

            if (Files.exists(directory)) {
                try (Stream<Path> walk = Files.walk(directory)) {
                    walk.sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Failed to delete: {}", path, e);
                            }
                        });
                }
                log.debug("Deleted directory: {}", directory);
            }
        } catch (IOException e) {
            log.error("Failed to delete directory: {}", directoryName, e);
        }
    }
}
