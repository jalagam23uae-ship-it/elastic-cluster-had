package com.dynamic.xsd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for schema upload response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaUploadResponse {

    private String id;
    private String serviceName;
    private String version;
    private String namespace;
    private String status;
    private LocalDateTime uploadedAt;
    private List<String> validationWarnings;
    private List<String> validationErrors;
    private String message;
}
