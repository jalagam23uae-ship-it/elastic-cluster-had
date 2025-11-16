package com.dynamic.xsd.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing time-series metrics for services.
 */
@Entity
@Table(name = "service_metrics", indexes = {
    @Index(name = "idx_metrics_service_id", columnList = "service_definition_id"),
    @Index(name = "idx_metrics_timestamp", columnList = "timestamp"),
    @Index(name = "idx_metrics_type", columnList = "metric_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "service_definition_id", nullable = false)
    private String serviceDefinitionId;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tags;

    @Column(name = "aggregation_window")
    private String aggregationWindow;  // "1m", "5m", "15m", "1h", "1d"

    public enum MetricType {
        REQUEST_COUNT,      // Number of requests
        RESPONSE_TIME,      // Response time in milliseconds
        ERROR_RATE,         // Error percentage
        THROUGHPUT,         // Requests per second
        CPU_USAGE,          // CPU usage percentage
        MEMORY_USAGE        // Memory usage in MB
    }
}
