package com.dynamic.xsd.controller;

import com.dynamic.xsd.domain.entity.RateLimitBucket;
import com.dynamic.xsd.domain.entity.ServiceMetric;
import com.dynamic.xsd.dto.ApiResponse;
import com.dynamic.xsd.service.AuditLogService;
import com.dynamic.xsd.service.MetricsCollectionService;
import com.dynamic.xsd.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for platform management and monitoring.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/management")
@RequiredArgsConstructor
@Tag(name = "Management", description = "Platform management and monitoring APIs")
public class ManagementController {

    private final RateLimitService rateLimitService;
    private final AuditLogService auditLogService;
    private final MetricsCollectionService metricsCollectionService;

    // ==================== Rate Limiting Management ====================

    @GetMapping("/rate-limit/{username}")
    @Operation(summary = "Get Rate Limit Status", description = "Get rate limit status for a user")
    public ResponseEntity<ApiResponse<RateLimitService.RateLimitStatus>> getRateLimitStatus(
            @PathVariable String username) {

        log.debug("Get rate limit status for user: {}", username);

        RateLimitService.RateLimitStatus status = rateLimitService.checkStatus(
            username, RateLimitBucket.BucketType.USER);

        return ResponseEntity.ok(ApiResponse.success(status, "Rate limit status retrieved"));
    }

    @PostMapping("/rate-limit/{username}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset Rate Limit", description = "Reset rate limit for a user (Admin only)")
    public ResponseEntity<ApiResponse<String>> resetRateLimit(@PathVariable String username) {

        log.info("Resetting rate limit for user: {}", username);

        rateLimitService.resetRateLimit(username, RateLimitBucket.BucketType.USER);

        return ResponseEntity.ok(ApiResponse.success(null, "Rate limit reset successfully"));
    }

    @PostMapping("/rate-limit/{username}/configure")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Configure Rate Limit", description = "Configure rate limit for a user (Admin only)")
    public ResponseEntity<ApiResponse<String>> configureRateLimit(
            @PathVariable String username,
            @RequestParam int maxTokens,
            @RequestParam int refillRate) {

        log.info("Configuring rate limit for user: {} - max: {}, rate: {}",
            username, maxTokens, refillRate);

        rateLimitService.configureRateLimit(username, RateLimitBucket.BucketType.USER,
            maxTokens, refillRate);

        return ResponseEntity.ok(ApiResponse.success(null, "Rate limit configured successfully"));
    }

    // ==================== Audit Log Management ====================

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Audit Logs", description = "Get audit logs with filtering (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "100") int limit) {

        log.debug("Get audit logs - user: {}, service: {}", username, serviceName);

        Map<String, Object> result = new HashMap<>();

        if (username != null) {
            result.put("logs", auditLogService.getAuditLogsByUsername(username,
                org.springframework.data.domain.PageRequest.of(0, limit)));
        } else if (serviceName != null) {
            result.put("logs", auditLogService.getAuditLogsByServiceName(serviceName));
        } else if (startTime != null && endTime != null) {
            result.put("logs", auditLogService.getAuditLogsBetween(startTime, endTime));
        } else {
            result.put("logs", auditLogService.getRecentFailures(limit));
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Audit logs retrieved"));
    }

    @GetMapping("/audit/statistics/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get User Audit Statistics", description = "Get audit statistics for a user (Admin only)")
    public ResponseEntity<ApiResponse<AuditLogService.AuditStatistics>> getUserAuditStatistics(
            @PathVariable String username) {

        log.debug("Get audit statistics for user: {}", username);

        AuditLogService.AuditStatistics stats = auditLogService.getUserStatistics(username);

        return ResponseEntity.ok(ApiResponse.success(stats, "Audit statistics retrieved"));
    }

    // ==================== Metrics Management ====================

    @GetMapping("/metrics/{serviceId}")
    @Operation(summary = "Get Service Metrics", description = "Get metrics for a service")
    public ResponseEntity<ApiResponse<List<ServiceMetric>>> getServiceMetrics(
            @PathVariable String serviceId,
            @RequestParam(required = false) ServiceMetric.MetricType metricType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.debug("Get metrics for service: {} - type: {}", serviceId, metricType);

        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusHours(24);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        List<ServiceMetric> metrics;
        if (metricType != null) {
            metrics = metricsCollectionService.getMetricsByType(serviceId, metricType, start, end);
        } else {
            metrics = metricsCollectionService.getMetrics(serviceId, start, end);
        }

        return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics retrieved"));
    }

    @GetMapping("/metrics/{serviceId}/statistics")
    @Operation(summary = "Get Metric Statistics", description = "Get statistical analysis of metrics")
    public ResponseEntity<ApiResponse<MetricsCollectionService.MetricStatistics>> getMetricStatistics(
            @PathVariable String serviceId,
            @RequestParam ServiceMetric.MetricType metricType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.debug("Get metric statistics for service: {} - type: {}", serviceId, metricType);

        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusHours(24);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        MetricsCollectionService.MetricStatistics stats =
            metricsCollectionService.getStatistics(serviceId, metricType, start, end);

        return ResponseEntity.ok(ApiResponse.success(stats, "Metric statistics retrieved"));
    }

    @GetMapping("/metrics/{serviceId}/timeseries")
    @Operation(summary = "Get Time Series Data", description = "Get time-series metrics with aggregation")
    public ResponseEntity<ApiResponse<List<MetricsCollectionService.TimeSeriesDataPoint>>> getTimeSeriesMetrics(
            @PathVariable String serviceId,
            @RequestParam ServiceMetric.MetricType metricType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "5") int windowMinutes) {

        log.debug("Get time series for service: {} - type: {}, window: {}m",
            serviceId, metricType, windowMinutes);

        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusHours(24);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        List<MetricsCollectionService.TimeSeriesDataPoint> timeSeries =
            metricsCollectionService.getTimeSeries(serviceId, metricType, start, end, windowMinutes);

        return ResponseEntity.ok(ApiResponse.success(timeSeries, "Time series data retrieved"));
    }

    // ==================== Cleanup Operations ====================

    @PostMapping("/cleanup/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cleanup Old Audit Logs", description = "Delete old audit logs (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupAuditLogs(
            @RequestParam(defaultValue = "90") int retentionDays) {

        log.info("Cleaning up audit logs older than {} days", retentionDays);

        int deleted = auditLogService.deleteOldAuditLogs(retentionDays);

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deleted);
        result.put("retentionDays", retentionDays);

        return ResponseEntity.ok(ApiResponse.success(result, "Audit logs cleaned up successfully"));
    }

    @PostMapping("/cleanup/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cleanup Old Metrics", description = "Delete old metrics (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupMetrics(
            @RequestParam(defaultValue = "30") int retentionDays) {

        log.info("Cleaning up metrics older than {} days", retentionDays);

        int deleted = metricsCollectionService.deleteOldMetrics(retentionDays);

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deleted);
        result.put("retentionDays", retentionDays);

        return ResponseEntity.ok(ApiResponse.success(result, "Metrics cleaned up successfully"));
    }
}
