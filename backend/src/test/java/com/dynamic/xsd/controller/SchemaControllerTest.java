package com.dynamic.xsd.controller;

import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.domain.enums.SchemaStatus;
import com.dynamic.xsd.dto.ValidationResult;
import com.dynamic.xsd.repository.SchemaMetadataRepository;
import com.dynamic.xsd.service.AuditLogService;
import com.dynamic.xsd.service.DynamicCodeCompilationService;
import com.dynamic.xsd.service.XSDCodeGenerationService;
import com.dynamic.xsd.service.XSDValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SchemaController
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchemaMetadataRepository schemaRepository;

    @MockBean
    private XSDValidationService validationService;

    @MockBean
    private XSDCodeGenerationService codeGenerationService;

    @MockBean
    private DynamicCodeCompilationService compilationService;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    void testUploadSchema_ValidXsd_ReturnsSuccess() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "customer.xsd",
                "text/xml",
                "<?xml version=\"1.0\"?><xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"></xs:schema>".getBytes()
        );

        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);
        validationResult.setTargetNamespace("http://example.com/customer");

        when(validationService.validateXsd(any(Path.class))).thenReturn(validationResult);
        when(codeGenerationService.generateCode(any(Path.class), anyString(), anyString()))
                .thenReturn(Arrays.asList(Path.of("Customer.java")));
        when(compilationService.compile(any(List.class), any(Path.class))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/schemas/upload")
                        .file(file)
                        .param("serviceName", "customer-service")
                        .param("version", "1.0")
                        .param("description", "Test service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceName").value("customer-service"))
                .andExpect(jsonPath("$.data.version").value("1.0"));
    }

    @Test
    void testUploadSchema_InvalidServiceName_ReturnsBadRequest() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xsd",
                "text/xml",
                "<?xml version=\"1.0\"?><xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"></xs:schema>".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/schemas/upload")
                        .file(file)
                        .param("serviceName", "Invalid Service Name") // Spaces not allowed
                        .param("version", "1.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUploadSchema_MissingFile_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/v1/schemas/upload")
                        .param("serviceName", "customer-service")
                        .param("version", "1.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListSchemas_WithPagination_ReturnsPagedResults() throws Exception {
        // Arrange - Create test data
        SchemaMetadata schema1 = createTestSchema("test-service-1", "1.0");
        SchemaMetadata schema2 = createTestSchema("test-service-2", "1.0");
        schemaRepository.save(schema1);
        schemaRepository.save(schema2);

        // Act & Assert
        mockMvc.perform(get("/api/v1/schemas")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }

    @Test
    void testListSchemas_WithStatusFilter_ReturnsFilteredResults() throws Exception {
        // Arrange
        SchemaMetadata activeSchema = createTestSchema("active-service", "1.0");
        activeSchema.setStatus(SchemaStatus.ACTIVE);

        SchemaMetadata failedSchema = createTestSchema("failed-service", "1.0");
        failedSchema.setStatus(SchemaStatus.FAILED);

        schemaRepository.save(activeSchema);
        schemaRepository.save(failedSchema);

        // Act & Assert
        mockMvc.perform(get("/api/v1/schemas")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetSchemaByServiceName_ExistingSchema_ReturnsSchema() throws Exception {
        // Arrange
        SchemaMetadata schema = createTestSchema("test-service", "1.0");
        schemaRepository.save(schema);

        // Act & Assert
        mockMvc.perform(get("/api/v1/schemas/test-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.version").value("1.0"));
    }

    @Test
    void testGetSchemaByServiceName_NonExistent_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/schemas/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteSchema_ExistingSchema_ReturnsSuccess() throws Exception {
        // Arrange
        SchemaMetadata schema = createTestSchema("delete-test", "1.0");
        schemaRepository.save(schema);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/schemas/delete-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testDeleteSchema_NonExistent_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/schemas/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUploadSchema_WithAutoDeploy_DeploysAutomatically() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "auto-deploy.xsd",
                "text/xml",
                "<?xml version=\"1.0\"?><xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"></xs:schema>".getBytes()
        );

        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);
        validationResult.setTargetNamespace("http://example.com/autodeploy");

        when(validationService.validateXsd(any(Path.class))).thenReturn(validationResult);
        when(codeGenerationService.generateCode(any(Path.class), anyString(), anyString()))
                .thenReturn(Arrays.asList(Path.of("Test.java")));
        when(compilationService.compile(any(List.class), any(Path.class))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/schemas/upload")
                        .file(file)
                        .param("serviceName", "auto-deploy-service")
                        .param("version", "1.0")
                        .param("autoDeploy", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // Helper method to create test schema
    private SchemaMetadata createTestSchema(String serviceName, String version) {
        SchemaMetadata schema = new SchemaMetadata();
        schema.setServiceName(serviceName);
        schema.setVersion(version);
        schema.setTargetNamespace("http://example.com/" + serviceName);
        schema.setStatus(SchemaStatus.ACTIVE);
        schema.setUploadedBy("test-user");
        schema.setUploadedAt(LocalDateTime.now());
        schema.setXsdFilePath("/tmp/" + serviceName + ".xsd");
        schema.setPackageName("com.example." + serviceName.replace("-", "."));
        return schema;
    }
}
