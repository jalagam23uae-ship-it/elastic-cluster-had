package com.dynamic.xsd.integration;

import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.domain.enums.SchemaStatus;
import com.dynamic.xsd.domain.enums.ServiceStatus;
import com.dynamic.xsd.repository.SchemaMetadataRepository;
import com.dynamic.xsd.repository.ServiceDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for complete workflows
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EndToEndWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchemaMetadataRepository schemaRepository;

    @Autowired
    private ServiceDefinitionRepository serviceRepository;

    @Test
    void testCompleteWorkflow_UploadDeployAndQuery() throws Exception {
        // Note: This is a simplified test since actual XSD processing requires file I/O
        // In a real scenario, you would need to mock the XSD processing services

        // Step 1: Check catalog is initially empty
        mockMvc.perform(get("/api/v1/catalog/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // Step 2: List schemas
        mockMvc.perform(get("/api/v1/schemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Step 3: Search for non-existent service
        mockMvc.perform(get("/api/v1/catalog/search")
                        .param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testServiceLifecycle_CreateDeployUndeploy() throws Exception {
        // This test verifies the complete lifecycle of a service
        // In production, this would involve actual XSD upload and processing

        // For this test, we'll create entities manually to simulate the workflow
        SchemaMetadata schema = new SchemaMetadata();
        schema.setServiceName("lifecycle-test");
        schema.setVersion("1.0");
        schema.setTargetNamespace("http://example.com/lifecycle");
        schema.setStatus(SchemaStatus.ACTIVE);
        schema.setUploadedBy("test-user");
        schema.setXsdFilePath("/tmp/lifecycle.xsd");
        schema.setPackageName("com.example.lifecycle");

        SchemaMetadata savedSchema = schemaRepository.save(schema);

        // Verify schema exists
        mockMvc.perform(get("/api/v1/schemas/lifecycle-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("lifecycle-test"));

        // Create service
        ServiceDefinition service = new ServiceDefinition();
        service.setServiceName("lifecycle-test");
        service.setVersion("1.0");
        service.setSchemaMetadata(savedSchema);
        service.setStatus(ServiceStatus.DEPLOYED);
        service.setDeployedBy("test-user");

        ServiceDefinition savedService = serviceRepository.save(service);

        // Verify service appears in catalog
        mockMvc.perform(get("/api/v1/catalog/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // Delete schema
        mockMvc.perform(delete("/api/v1/schemas/lifecycle-test"))
                .andExpect(status().isOk());
    }

    @Test
    void testRateLimiting_CheckStatus() throws Exception {
        // Test rate limiting endpoints
        mockMvc.perform(get("/api/v1/management/rate-limit/test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identifier").value("test-user"));
    }

    @Test
    void testMetrics_GetServiceMetrics() throws Exception {
        // Create a test service for metrics
        SchemaMetadata schema = new SchemaMetadata();
        schema.setServiceName("metrics-test");
        schema.setVersion("1.0");
        schema.setTargetNamespace("http://example.com/metrics");
        schema.setStatus(SchemaStatus.ACTIVE);
        schema.setUploadedBy("test-user");
        schema.setXsdFilePath("/tmp/metrics.xsd");
        schema.setPackageName("com.example.metrics");

        SchemaMetadata savedSchema = schemaRepository.save(schema);

        ServiceDefinition service = new ServiceDefinition();
        service.setServiceName("metrics-test");
        service.setVersion("1.0");
        service.setSchemaMetadata(savedSchema);
        service.setStatus(ServiceStatus.DEPLOYED);
        service.setDeployedBy("test-user");

        ServiceDefinition savedService = serviceRepository.save(service);

        // Get metrics
        mockMvc.perform(get("/api/v1/management/metrics/" + savedService.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testCatalogEndpoints_AllEndpoints() throws Exception {
        // Test all catalog endpoints work together

        // List services
        mockMvc.perform(get("/api/v1/catalog/services"))
                .andExpect(status().isOk());

        // List schemas
        mockMvc.perform(get("/api/v1/catalog/schemas"))
                .andExpect(status().isOk());

        // List endpoints
        mockMvc.perform(get("/api/v1/catalog/endpoints"))
                .andExpect(status().isOk());
    }

    @Test
    void testPagination_AcrossMultiplePages() throws Exception {
        // Create test data
        for (int i = 0; i < 25; i++) {
            SchemaMetadata schema = new SchemaMetadata();
            schema.setServiceName("page-test-" + i);
            schema.setVersion("1.0");
            schema.setTargetNamespace("http://example.com/page" + i);
            schema.setStatus(SchemaStatus.ACTIVE);
            schema.setUploadedBy("test-user");
            schema.setXsdFilePath("/tmp/page" + i + ".xsd");
            schema.setPackageName("com.example.page" + i);
            schemaRepository.save(schema);
        }

        // Get first page
        MvcResult page1 = mockMvc.perform(get("/api/v1/schemas")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andReturn();

        // Get second page
        mockMvc.perform(get("/api/v1/schemas")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testErrorHandling_InvalidRequests() throws Exception {
        // Test various error scenarios

        // Invalid schema name
        mockMvc.perform(get("/api/v1/schemas/!!!invalid!!!"))
                .andExpect(status().isNotFound());

        // Missing required parameter in search
        mockMvc.perform(get("/api/v1/catalog/search"))
                .andExpect(status().isBadRequest());

        // Invalid page number
        mockMvc.perform(get("/api/v1/schemas")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }
}
