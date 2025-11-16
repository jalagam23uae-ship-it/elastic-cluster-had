package com.dynamic.xsd.controller;

import com.dynamic.xsd.dto.ApiResponse;
import com.dynamic.xsd.service.ServiceDeploymentService;
import com.dynamic.xsd.service.SoapServiceGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for managing dynamic service deployment and operations.
 * Handles deployment, undeployment, and SOAP WSDL serving.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Dynamic Services", description = "APIs for managing dynamically generated services")
public class DynamicServiceController {

    private final ServiceDeploymentService deploymentService;
    private final SoapServiceGenerator soapServiceGenerator;

    @PostMapping("/api/v1/services/deploy/{schemaId}")
    @Operation(summary = "Deploy Service", description = "Deploy a service from an uploaded schema")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deployService(@PathVariable String schemaId) {
        log.info("Deploy service request for schema ID: {}", schemaId);

        try {
            ServiceDeploymentService.DeploymentResult result = deploymentService.deployService(schemaId);

            if (result.success) {
                Map<String, Object> data = new HashMap<>();
                data.put("serviceName", result.serviceName);
                data.put("restEndpoints", result.restEndpoints.size());
                data.put("soapEndpoints", result.soapEndpoints.size());
                data.put("message", result.message);

                return ResponseEntity.ok(ApiResponse.success(data, result.message));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(result.message));
            }

        } catch (Exception e) {
            log.error("Service deployment failed for schema ID: {}", schemaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Deployment failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/v1/services/undeploy/{schemaId}")
    @Operation(summary = "Undeploy Service", description = "Undeploy a running service")
    public ResponseEntity<ApiResponse<String>> undeployService(@PathVariable String schemaId) {
        log.info("Undeploy service request for schema ID: {}", schemaId);

        try {
            deploymentService.undeployService(schemaId);
            return ResponseEntity.ok(ApiResponse.success(null, "Service undeployed successfully"));

        } catch (Exception e) {
            log.error("Service undeployment failed for schema ID: {}", schemaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Undeployment failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/v1/services/redeploy/{schemaId}")
    @Operation(summary = "Redeploy Service", description = "Redeploy a service (undeploy + deploy)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> redeployService(@PathVariable String schemaId) {
        log.info("Redeploy service request for schema ID: {}", schemaId);

        try {
            ServiceDeploymentService.DeploymentResult result = deploymentService.redeployService(schemaId);

            if (result.success) {
                Map<String, Object> data = new HashMap<>();
                data.put("serviceName", result.serviceName);
                data.put("restEndpoints", result.restEndpoints.size());
                data.put("soapEndpoints", result.soapEndpoints.size());
                data.put("message", result.message);

                return ResponseEntity.ok(ApiResponse.success(data, result.message));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(result.message));
            }

        } catch (Exception e) {
            log.error("Service redeployment failed for schema ID: {}", schemaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Redeployment failed: " + e.getMessage()));
        }
    }

    @GetMapping("/api/v1/services/status/{schemaId}")
    @Operation(summary = "Get Deployment Status", description = "Get deployment status for a schema")
    public ResponseEntity<ApiResponse<ServiceDeploymentService.DeploymentStatus>> getDeploymentStatus(
            @PathVariable String schemaId) {
        log.info("Get deployment status for schema ID: {}", schemaId);

        try {
            ServiceDeploymentService.DeploymentStatus status = deploymentService.getDeploymentStatus(schemaId);
            return ResponseEntity.ok(ApiResponse.success(status, "Status retrieved successfully"));

        } catch (Exception e) {
            log.error("Failed to get deployment status for schema ID: {}", schemaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get status: " + e.getMessage()));
        }
    }

    /**
     * Serves WSDL documents for deployed SOAP services.
     * Accessible at: /ws/{serviceName}?wsdl
     */
    @GetMapping(value = "/ws/{serviceName}", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Get WSDL", description = "Get WSDL document for a SOAP service")
    public ResponseEntity<String> getWsdl(@PathVariable String serviceName,
                                          @RequestParam(required = false) String wsdl) {
        log.info("WSDL request for service: {}", serviceName);

        // Only serve WSDL if 'wsdl' parameter is present (standard SOAP convention)
        // Standard URL: /ws/{serviceName}?wsdl
        if (wsdl == null) {
            return ResponseEntity.badRequest()
                .body("<?xml version=\"1.0\"?><error>Use ?wsdl parameter to get WSDL</error>");
        }

        String wsdlContent = soapServiceGenerator.getWsdl(serviceName);

        if (wsdlContent != null) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(wsdlContent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Handles SOAP requests.
     * Receives SOAP envelope, processes operation, and returns SOAP response.
     */
    @PostMapping(value = "/ws/{serviceName}",
        consumes = MediaType.TEXT_XML_VALUE,
        produces = MediaType.TEXT_XML_VALUE)
    @Operation(summary = "SOAP Endpoint", description = "Handle SOAP requests for deployed services")
    public ResponseEntity<String> handleSoapRequest(@PathVariable String serviceName,
                                                     HttpServletRequest request) {
        try {
            log.debug("SOAP request received for service: {}", serviceName);

            // Read request body
            StringBuilder requestBody = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
            }

            String soapRequest = requestBody.toString();
            log.debug("SOAP request body: {}", soapRequest);

            // Extract operation name from SOAP body (simplified)
            String operationName = extractOperationName(soapRequest);

            if (operationName == null) {
                return ResponseEntity.badRequest()
                    .body(createSoapFault("Client", "Could not determine operation from SOAP request"));
            }

            // Process SOAP request
            String soapResponse = soapServiceGenerator.processSoapRequest(serviceName, operationName, soapRequest);

            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_XML)
                .body(soapResponse);

        } catch (Exception e) {
            log.error("SOAP request processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createSoapFault("Server", "Request processing failed: " + e.getMessage()));
        }
    }

    /**
     * Extracts operation name from SOAP request.
     * This is a simplified implementation - in production, use a proper SOAP parser.
     */
    private String extractOperationName(String soapRequest) {
        // Look for pattern like <operation>...</operation> or <ns:operation>...
        // This is a simplified extraction - real implementation should use XML parser

        // Common patterns
        if (soapRequest.contains("<soap:Body>")) {
            int bodyStart = soapRequest.indexOf("<soap:Body>") + 11;
            int bodyEnd = soapRequest.indexOf("</soap:Body>");

            if (bodyStart > 0 && bodyEnd > bodyStart) {
                String body = soapRequest.substring(bodyStart, bodyEnd).trim();

                // Find first element in body
                int firstTagStart = body.indexOf('<');
                int firstTagEnd = body.indexOf('>');

                if (firstTagStart >= 0 && firstTagEnd > firstTagStart) {
                    String tag = body.substring(firstTagStart + 1, firstTagEnd);

                    // Remove namespace prefix if present
                    if (tag.contains(":")) {
                        tag = tag.substring(tag.indexOf(':') + 1);
                    }

                    // Remove attributes if present
                    if (tag.contains(" ")) {
                        tag = tag.substring(0, tag.indexOf(' '));
                    }

                    return tag;
                }
            }
        }

        return null;
    }

    /**
     * Creates a SOAP fault response.
     */
    private String createSoapFault(String faultCode, String faultString) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soap:Body>\n" +
            "    <soap:Fault>\n" +
            "      <faultcode>soap:" + faultCode + "</faultcode>\n" +
            "      <faultstring>" + faultString + "</faultstring>\n" +
            "    </soap:Fault>\n" +
            "  </soap:Body>\n" +
            "</soap:Envelope>";
    }
}
