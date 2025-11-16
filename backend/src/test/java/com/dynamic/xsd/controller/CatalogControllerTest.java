package com.dynamic.xsd.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CatalogController
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceDefinitionRepository serviceRepository;

    @Autowired
    private SchemaMetadataRepository schemaRepository;

    @Test
    void testListServices_ReturnsAllDeployedServices() throws Exception {
        // Arrange
        ServiceDefinition service = createTestService("catalog-test-1");
        serviceRepository.save(service);

        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testGetServiceDetails_ExistingService_ReturnsDetails() throws Exception {
        // Arrange
        SchemaMetadata schema = createTestSchema("detail-test");
        ServiceDefinition service = createTestService("detail-test");
        service.setSchemaMetadata(schema);

        schemaRepository.save(schema);
        serviceRepository.save(service);

        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/services/detail-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").exists())
                .andExpect(jsonPath("$.data.schema").exists());
    }

    @Test
    void testGetServiceDetails_NonExistent_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/services/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListSchemas_ReturnsAllSchemas() throws Exception {
        // Arrange
        SchemaMetadata schema = createTestSchema("schema-list-test");
        schemaRepository.save(schema);

        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/schemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testListSchemas_WithStatusFilter_ReturnsFiltered() throws Exception {
        // Arrange
        SchemaMetadata activeSchema = createTestSchema("active-schema");
        activeSchema.setStatus(SchemaStatus.ACTIVE);

        SchemaMetadata failedSchema = createTestSchema("failed-schema");
        failedSchema.setStatus(SchemaStatus.FAILED);

        schemaRepository.save(activeSchema);
        schemaRepository.save(failedSchema);

        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/schemas")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testSearchServices_WithQuery_ReturnsMatching() throws Exception {
        // Arrange
        ServiceDefinition service1 = createTestService("customer-service");
        ServiceDefinition service2 = createTestService("order-service");
        serviceRepository.save(service1);
        serviceRepository.save(service2);

        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/search")
                        .param("query", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testSearchServices_NoQuery_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListEndpoints_ReturnsAllEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/endpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // Helper methods
    private ServiceDefinition createTestService(String serviceName) {
        ServiceDefinition service = new ServiceDefinition();
        service.setServiceName(serviceName);
        service.setVersion("1.0");
        service.setStatus(ServiceStatus.DEPLOYED);
        service.setDeployedAt(LocalDateTime.now());
        service.setDeployedBy("test-user");
        return service;
    }

    private SchemaMetadata createTestSchema(String serviceName) {
        SchemaMetadata schema = new SchemaMetadata();
        schema.setServiceName(serviceName);
        schema.setVersion("1.0");
        schema.setTargetNamespace("http://example.com/" + serviceName);
        schema.setStatus(SchemaStatus.ACTIVE);
        schema.setUploadedBy("test-user");
        schema.setUploadedAt(LocalDateTime.now());
        schema.setXsdFilePath("/tmp/" + serviceName + ".xsd");
        schema.setPackageName("com.example." + serviceName.replace("-", "."));
        return schema;
    }
}
