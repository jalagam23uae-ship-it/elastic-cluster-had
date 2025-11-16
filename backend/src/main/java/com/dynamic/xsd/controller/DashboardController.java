package com.dynamic.xsd.controller;

import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.domain.entity.User;
import com.dynamic.xsd.domain.enums.UserRole;
import com.dynamic.xsd.dto.ApiResponse;
import com.dynamic.xsd.repository.EndpointMappingRepository;
import com.dynamic.xsd.repository.SchemaMetadataRepository;
import com.dynamic.xsd.repository.ServiceDefinitionRepository;
import com.dynamic.xsd.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for dashboard metrics and statistics.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard metrics and statistics")
public class DashboardController {

    private final SchemaMetadataRepository schemaRepository;
    private final ServiceDefinitionRepository serviceRepository;
    private final EndpointMappingRepository endpointRepository;
    private final UserService userService;

    @GetMapping("/metrics")
    @Operation(summary = "Get Dashboard Metrics", description = "Get platform metrics and statistics")
    public ResponseEntity<ApiResponse<DashboardMetrics>> getDashboardMetrics() {
        log.debug("Get dashboard metrics request");

        try {
            DashboardMetrics metrics = new DashboardMetrics();

            // Schema metrics
            metrics.totalSchemas = schemaRepository.count();
            metrics.activeSchemas = schemaRepository.countByStatus(SchemaMetadata.SchemaStatus.ACTIVE);
            metrics.failedSchemas = schemaRepository.countByStatus(SchemaMetadata.SchemaStatus.FAILED);
            metrics.processingSchemas = schemaRepository.countByStatus(SchemaMetadata.SchemaStatus.VALIDATING) +
                schemaRepository.countByStatus(SchemaMetadata.SchemaStatus.GENERATING) +
                schemaRepository.countByStatus(SchemaMetadata.SchemaStatus.COMPILING);

            // Service metrics
            metrics.totalServices = serviceRepository.count();
            metrics.activeServices = serviceRepository.countByStatus(ServiceDefinition.ServiceStatus.DEPLOYED);
            metrics.failedServices = serviceRepository.countByStatus(ServiceDefinition.ServiceStatus.FAILED) +
                serviceRepository.countByStatus(ServiceDefinition.ServiceStatus.DEPLOYMENT_FAILED);

            // Endpoint metrics
            metrics.totalEndpoints = endpointRepository.count();

            // User metrics
            metrics.totalUsers = userService.countAllUsers();
            metrics.activeUsers = userService.countActiveUsers();
            metrics.adminUsers = userService.countUsersByRole(UserRole.ADMIN);
            metrics.regularUsers = userService.countUsersByRole(UserRole.USER);

            // System metrics
            metrics.timestamp = LocalDateTime.now();
            metrics.systemStatus = determineSystemStatus(metrics);

            return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics retrieved successfully"));

        } catch (Exception e) {
            log.error("Failed to get dashboard metrics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get metrics: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get Platform Statistics", description = "Get detailed platform statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformStatistics() {
        log.debug("Get platform statistics request");

        try {
            Map<String, Object> stats = new HashMap<>();

            // Schema statistics by status
            Map<String, Long> schemaStats = new HashMap<>();
            for (SchemaMetadata.SchemaStatus status : SchemaMetadata.SchemaStatus.values()) {
                schemaStats.put(status.name(), schemaRepository.countByStatus(status));
            }
            stats.put("schemasByStatus", schemaStats);

            // Service statistics by status
            Map<String, Long> serviceStats = new HashMap<>();
            for (ServiceDefinition.ServiceStatus status : ServiceDefinition.ServiceStatus.values()) {
                serviceStats.put(status.name(), serviceRepository.countByStatus(status));
            }
            stats.put("servicesByStatus", serviceStats);

            // User statistics by role
            Map<String, Long> userStats = new HashMap<>();
            for (UserRole role : UserRole.values()) {
                userStats.put(role.name(), userService.countUsersByRole(role));
            }
            stats.put("usersByRole", userStats);

            // Endpoint statistics
            stats.put("totalEndpoints", endpointRepository.count());

            // Recent activity
            stats.put("recentSchemas", schemaRepository.findTop10ByOrderByUploadedAtDesc());
            stats.put("recentServices", serviceRepository.findTop10ByOrderByDeployedAtDesc());

            return ResponseEntity.ok(ApiResponse.success(stats, "Statistics retrieved successfully"));

        } catch (Exception e) {
            log.error("Failed to get platform statistics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Get platform health status")
    public ResponseEntity<ApiResponse<HealthStatus>> getHealthStatus() {
        log.debug("Health check request");

        try {
            HealthStatus health = new HealthStatus();
            health.status = "UP";
            health.timestamp = LocalDateTime.now();

            // Check database connectivity
            health.database = "UP";
            try {
                schemaRepository.count();
            } catch (Exception e) {
                health.database = "DOWN";
                health.status = "DOWN";
                log.error("Database health check failed", e);
            }

            // Check active services
            long activeServices = serviceRepository.countByStatus(ServiceDefinition.ServiceStatus.DEPLOYED);
            health.activeServices = activeServices;

            // Check failed services
            long failedServices = serviceRepository.countByStatus(ServiceDefinition.ServiceStatus.FAILED) +
                serviceRepository.countByStatus(ServiceDefinition.ServiceStatus.DEPLOYMENT_FAILED);
            health.failedServices = failedServices;

            if (failedServices > 0) {
                health.warnings = "There are " + failedServices + " failed services";
            }

            return ResponseEntity.ok(ApiResponse.success(health, "Health check completed"));

        } catch (Exception e) {
            log.error("Health check failed", e);
            HealthStatus health = new HealthStatus();
            health.status = "DOWN";
            health.timestamp = LocalDateTime.now();
            health.database = "UNKNOWN";

            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Health check failed", health));
        }
    }

    /**
     * Determines overall system status based on metrics.
     */
    private String determineSystemStatus(DashboardMetrics metrics) {
        if (metrics.failedSchemas > 0 || metrics.failedServices > 0) {
            return "WARNING";
        }
        if (metrics.processingSchemas > 0) {
            return "PROCESSING";
        }
        if (metrics.totalSchemas == 0) {
            return "IDLE";
        }
        return "HEALTHY";
    }

    /**
     * Dashboard metrics response.
     */
    public static class DashboardMetrics {
        // Schema metrics
        public long totalSchemas;
        public long activeSchemas;
        public long failedSchemas;
        public long processingSchemas;

        // Service metrics
        public long totalServices;
        public long activeServices;
        public long failedServices;

        // Endpoint metrics
        public long totalEndpoints;

        // User metrics
        public long totalUsers;
        public long activeUsers;
        public long adminUsers;
        public long regularUsers;

        // System status
        public String systemStatus;
        public LocalDateTime timestamp;
    }

    /**
     * Health status response.
     */
    public static class HealthStatus {
        public String status;
        public String database;
        public long activeServices;
        public long failedServices;
        public String warnings;
        public LocalDateTime timestamp;
    }
}
