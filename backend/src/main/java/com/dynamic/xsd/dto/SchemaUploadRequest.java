package com.dynamic.xsd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for schema upload request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaUploadRequest {

    @NotBlank(message = "Service name is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Service name must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 3, max = 50, message = "Service name must be between 3 and 50 characters")
    private String serviceName;

    @Pattern(regexp = "^\\d+\\.\\d+(\\.\\d+)?$", message = "Version must be in format x.y or x.y.z")
    @Builder.Default
    private String version = "1.0";

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String namespace;

    @Builder.Default
    private Boolean autoDeploy = false;
}
