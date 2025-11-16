package com.dynamic.xsd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for dashboard metrics response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricsResponse {

    private SummaryMetrics summary;
    private List<ChartDataPoint> requestVolume;
    private Map<String, Long> serviceStatusDistribution;
    private List<RecentActivity> recentActivities;
    private SystemHealth systemHealth;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryMetrics {
        private Long totalServices;
        private Long activeServices;
        private Long totalRequests;
        private Double successRate;
        private TrendInfo trends;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendInfo {
        private Integer servicesChange;
        private Double requestsChangePercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartDataPoint {
        private String timestamp;
        private Long value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentActivity {
        private String id;
        private String timestamp;
        private String username;
        private String action;
        private String serviceName;
        private String status;
        private String details;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemHealth {
        private Double cpuUsage;
        private Double memoryUsage;
        private Integer activeConnections;
        private Integer classloadersCount;
    }
}
