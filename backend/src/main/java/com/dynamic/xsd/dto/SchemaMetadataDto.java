package com.dynamic.xsd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for schema metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaMetadataDto {

    private String id;
    private String serviceName;
    private String version;
    private String namespace;
    private String description;
    private String status;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private GeneratedArtifactsDto generatedArtifacts;
    private String validationErrors;
    private String compilationErrors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeneratedArtifactsDto {
        private List<String> pojos;
        private Integer restEndpointCount;
        private Integer soapOperationCount;
    }
}
