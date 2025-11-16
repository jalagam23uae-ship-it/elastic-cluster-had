package com.dynamic.xsd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for service status response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceStatusResponse {

    private String id;
    private String serviceName;
    private String version;
    private String status;
    private LocalDateTime deployedAt;
    private LocalDateTime updatedAt;
    private String deployedBy;
    private EndpointsInfo endpoints;
    private MetricsInfo metrics;
    private String deploymentErrors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EndpointsInfo {
        private List<EndpointDto> restEndpoints;
        private List<EndpointDto> soapOperations;
        private String wsdlUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EndpointDto {
        private String path;
        private String httpMethod;
        private String operationName;
        private String description;
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricsInfo {
        private Long totalRequests;
        private Long successfulRequests;
        private Long failedRequests;
        private Double successRate;
        private Double averageResponseTime;
    }
}
