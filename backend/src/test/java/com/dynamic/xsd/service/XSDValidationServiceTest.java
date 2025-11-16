package com.dynamic.xsd.service;

import com.dynamic.xsd.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XSDValidationService
 */
@SpringBootTest
class XSDValidationServiceTest {

    @Autowired
    private XSDValidationService validationService;

    @TempDir
    Path tempDir;

    private Path validXsdPath;
    private Path invalidXsdPath;

    @BeforeEach
    void setUp() throws IOException {
        // Create valid XSD
        validXsdPath = tempDir.resolve("valid.xsd");
        String validXsd = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                           targetNamespace="http://example.com/customer"
                           elementFormDefault="qualified">
                    <xs:element name="Customer">
                        <xs:complexType>
                            <xs:sequence>
                                <xs:element name="id" type="xs:long"/>
                                <xs:element name="name" type="xs:string"/>
                                <xs:element name="email" type="xs:string"/>
                            </xs:sequence>
                        </xs:complexType>
                    </xs:element>
                </xs:schema>
                """;
        Files.writeString(validXsdPath, validXsd);

        // Create invalid XSD
        invalidXsdPath = tempDir.resolve("invalid.xsd");
        String invalidXsd = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                    <xs:element name="Customer">
                        <xs:invalidTag>
                        </xs:invalidTag>
                    </xs:element>
                </xs:schema>
                """;
        Files.writeString(invalidXsdPath, invalidXsd);
    }

    @Test
    void testValidateXsd_ValidSchema_ReturnsSuccess() {
        // Act
        ValidationResult result = validationService.validateXsd(validXsdPath);

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid(), "Valid XSD should pass validation");
        assertTrue(result.getErrors().isEmpty(), "Valid XSD should have no errors");
        assertNotNull(result.getTargetNamespace());
        assertEquals("http://example.com/customer", result.getTargetNamespace());
    }

    @Test
    void testValidateXsd_InvalidSchema_ReturnsErrors() {
        // Act
        ValidationResult result = validationService.validateXsd(invalidXsdPath);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid(), "Invalid XSD should fail validation");
        assertFalse(result.getErrors().isEmpty(), "Invalid XSD should have errors");
    }

    @Test
    void testValidateXsd_NonExistentFile_ThrowsException() {
        // Arrange
        Path nonExistent = tempDir.resolve("nonexistent.xsd");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            validationService.validateXsd(nonExistent);
        });
    }

    @Test
    void testExtractTargetNamespace_ValidXsd_ReturnsNamespace() {
        // Act
        String namespace = validationService.extractTargetNamespace(validXsdPath);

        // Assert
        assertNotNull(namespace);
        assertEquals("http://example.com/customer", namespace);
    }

    @Test
    void testExtractTargetNamespace_NoNamespace_ReturnsNull() throws IOException {
        // Arrange
        Path noNamespaceXsd = tempDir.resolve("no-namespace.xsd");
        String xsdContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                    <xs:element name="Test" type="xs:string"/>
                </xs:schema>
                """;
        Files.writeString(noNamespaceXsd, xsdContent);

        // Act
        String namespace = validationService.extractTargetNamespace(noNamespaceXsd);

        // Assert
        assertTrue(namespace == null || namespace.isEmpty());
    }

    @Test
    void testValidateXsd_LargeSchema_HandlesCorrectly() throws IOException {
        // Arrange - Create a larger XSD with multiple types
        Path largeXsd = tempDir.resolve("large.xsd");
        StringBuilder xsdBuilder = new StringBuilder();
        xsdBuilder.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                           targetNamespace="http://example.com/large"
                           elementFormDefault="qualified">
                """);

        // Add 50 element definitions
        for (int i = 0; i < 50; i++) {
            xsdBuilder.append(String.format("""
                    <xs:element name="Element%d" type="xs:string"/>
                    """, i));
        }

        xsdBuilder.append("</xs:schema>");
        Files.writeString(largeXsd, xsdBuilder.toString());

        // Act
        ValidationResult result = validationService.validateXsd(largeXsd);

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
    }
}
