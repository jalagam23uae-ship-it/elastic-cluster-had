package com.dynamic.xsd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for service deployment request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDeployRequest {

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @Builder.Default
    private Boolean generateRest = true;

    @Builder.Default
    private Boolean generateSoap = true;

    @Builder.Default
    private Boolean enableCaching = false;

    private Integer timeout;

    private Integer rateLimit;
}
