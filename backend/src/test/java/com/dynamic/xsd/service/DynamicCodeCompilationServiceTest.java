package com.dynamic.xsd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DynamicCodeCompilationService
 */
@SpringBootTest
class DynamicCodeCompilationServiceTest {

    @Autowired
    private DynamicCodeCompilationService compilationService;

    @TempDir
    Path tempDir;

    private Path validJavaFile;
    private Path invalidJavaFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create valid Java file
        validJavaFile = tempDir.resolve("Customer.java");
        String validJava = """
                package com.example.customer;

                public class Customer {
                    private Long id;
                    private String name;
                    private String email;

                    public Long getId() { return id; }
                    public void setId(Long id) { this.id = id; }

                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }

                    public String getEmail() { return email; }
                    public void setEmail(String email) { this.email = email; }
                }
                """;
        Files.writeString(validJavaFile, validJava);

        // Create invalid Java file
        invalidJavaFile = tempDir.resolve("Invalid.java");
        String invalidJava = """
                package com.example;

                public class Invalid {
                    // Syntax error - missing closing brace
                    public void test() {
                        String s = "test"
                }
                """;
        Files.writeString(invalidJavaFile, invalidJava);
    }

    @Test
    void testCompile_ValidJavaFile_ReturnsSuccess() {
        // Act
        boolean result = compilationService.compile(List.of(validJavaFile), tempDir);

        // Assert
        assertTrue(result, "Valid Java file should compile successfully");
    }

    @Test
    void testCompile_InvalidJavaFile_ReturnsFalse() {
        // Act
        boolean result = compilationService.compile(List.of(invalidJavaFile), tempDir);

        // Assert
        assertFalse(result, "Invalid Java file should fail compilation");
    }

    @Test
    void testCompile_MultipleFiles_CompilesAll() throws IOException {
        // Arrange
        Path file2 = tempDir.resolve("Address.java");
        String addressJava = """
                package com.example.customer;

                public class Address {
                    private String street;
                    private String city;

                    public String getStreet() { return street; }
                    public void setStreet(String street) { this.street = street; }

                    public String getCity() { return city; }
                    public void setCity(String city) { this.city = city; }
                }
                """;
        Files.writeString(file2, addressJava);

        // Act
        boolean result = compilationService.compile(List.of(validJavaFile, file2), tempDir);

        // Assert
        assertTrue(result, "Multiple valid files should compile successfully");
    }

    @Test
    void testCompile_EmptyList_ReturnsTrue() {
        // Act
        boolean result = compilationService.compile(List.of(), tempDir);

        // Assert
        assertTrue(result, "Empty file list should return true");
    }

    @Test
    void testCompile_NonExistentFile_ReturnsFalse() {
        // Arrange
        Path nonExistent = tempDir.resolve("NonExistent.java");

        // Act
        boolean result = compilationService.compile(List.of(nonExistent), tempDir);

        // Assert
        assertFalse(result, "Non-existent file should fail compilation");
    }

    @Test
    void testCompile_WithDependencies_CompilesSuccessfully() throws IOException {
        // Arrange - Create a class that depends on another
        Path baseClass = tempDir.resolve("Base.java");
        String baseJava = """
                package com.example;

                public class Base {
                    protected String value;
                    public String getValue() { return value; }
                }
                """;
        Files.writeString(baseClass, baseJava);

        Path derivedClass = tempDir.resolve("Derived.java");
        String derivedJava = """
                package com.example;

                public class Derived extends Base {
                    public void setValue(String value) { this.value = value; }
                }
                """;
        Files.writeString(derivedClass, derivedJava);

        // Act - Compile in correct order
        boolean result = compilationService.compile(List.of(baseClass, derivedClass), tempDir);

        // Assert
        assertTrue(result, "Classes with dependencies should compile in order");
    }

    @Test
    void testLoadCompiledClass_AfterCompilation_LoadsSuccessfully() throws Exception {
        // Arrange
        compilationService.compile(List.of(validJavaFile), tempDir);

        // Act
        Class<?> loadedClass = compilationService.loadClass("com.example.customer.Customer", tempDir);

        // Assert
        assertNotNull(loadedClass);
        assertEquals("Customer", loadedClass.getSimpleName());
        assertNotNull(loadedClass.getMethod("getId"));
        assertNotNull(loadedClass.getMethod("setName", String.class));
    }
}
