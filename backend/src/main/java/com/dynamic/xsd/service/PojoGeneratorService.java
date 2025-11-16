package com.dynamic.xsd.service;

import com.dynamic.xsd.config.DynamicServiceProperties;
import com.sun.codemodel.JCodeModel;
import com.sun.tools.xjc.api.S2JJAXBModel;
import com.sun.tools.xjc.api.SchemaCompiler;
import com.sun.tools.xjc.api.XJC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for generating Java POJOs from XSD schemas using JAXB XJC API.
 *
 * Generated POJOs include:
 * - JAXB annotations (@XmlRootElement, @XmlElement, etc.)
 * - Jackson annotations (@JsonProperty) for JSON support
 * - Bean Validation annotations (@NotNull, @Size, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PojoGeneratorService {

    private final DynamicServiceProperties properties;

    /**
     * Generates Java POJOs from XSD content.
     *
     * @param serviceName Service name for package generation
     * @param xsdContent XSD schema content
     * @return Generation result containing generated files info
     */
    public GenerationResult generate(String serviceName, byte[] xsdContent) {
        log.info("Starting POJO generation for service: {}", serviceName);

        GenerationResult result = new GenerationResult();
        result.setServiceName(serviceName);

        try {
            // Create output directories
            Path sourceOutputDir = createOutputDirectory(serviceName, "sources");
            result.setSourceOutputDirectory(sourceOutputDir.toString());

            // Generate Java sources using XJC
            generateWithXJC(serviceName, xsdContent, sourceOutputDir, result);

            // Post-process generated files to add Jackson annotations
            if (result.isSuccess()) {
                enhanceGeneratedFiles(sourceOutputDir, result);
            }

            log.info("POJO generation completed for service: {}. Success: {}, Generated files: {}",
                serviceName, result.isSuccess(), result.getGeneratedFiles().size());

        } catch (Exception e) {
            log.error("POJO generation failed for service: {}", serviceName, e);
            result.setSuccess(false);
            result.addError("Generation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Generates Java sources using JAXB XJC API.
     */
    private void generateWithXJC(String serviceName, byte[] xsdContent,
                                  Path outputDir, GenerationResult result) throws IOException {
        try {
            // Create schema compiler
            SchemaCompiler compiler = XJC.createSchemaCompiler();

            // Set options
            compiler.setDefaultPackageName(getPackageName(serviceName));

            // Configure error listener
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            compiler.setErrorListener(new com.sun.tools.xjc.api.ErrorListener() {
                @Override
                public void error(org.xml.sax.SAXParseException e) {
                    String msg = "Line " + e.getLineNumber() + ": " + e.getMessage();
                    errors.add(msg);
                    log.error("XJC compilation error: {}", msg);
                }

                @Override
                public void fatalError(org.xml.sax.SAXParseException e) {
                    String msg = "Line " + e.getLineNumber() + ": " + e.getMessage();
                    errors.add(msg);
                    log.error("XJC fatal error: {}", msg);
                }

                @Override
                public void warning(org.xml.sax.SAXParseException e) {
                    String msg = "Line " + e.getLineNumber() + ": " + e.getMessage();
                    warnings.add(msg);
                    log.warn("XJC warning: {}", msg);
                }

                @Override
                public void info(org.xml.sax.SAXParseException e) {
                    log.info("XJC info: {}", e.getMessage());
                }
            });

            // Parse schema
            InputSource inputSource = new InputSource(new ByteArrayInputStream(xsdContent));
            inputSource.setSystemId("schema.xsd");
            compiler.parseSchema(inputSource);

            // Bind schema to Java
            S2JJAXBModel model = compiler.bind();

            if (model == null || !errors.isEmpty()) {
                result.setSuccess(false);
                errors.forEach(result::addError);
                return;
            }

            warnings.forEach(result::addWarning);

            // Generate code
            JCodeModel codeModel = model.generateCode(null, null);
            codeModel.build(outputDir.toFile());

            // Collect generated files
            collectGeneratedFiles(outputDir, result);

            result.setSuccess(true);
            log.info("XJC code generation completed successfully");

        } catch (Exception e) {
            log.error("XJC code generation failed", e);
            result.setSuccess(false);
            result.addError("XJC generation failed: " + e.getMessage());
        }
    }

    /**
     * Enhances generated files by adding Jackson annotations for JSON support.
     */
    private void enhanceGeneratedFiles(Path sourceDir, GenerationResult result) {
        log.debug("Enhancing generated files with Jackson annotations");

        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);

                        // Add Jackson imports if not present
                        if (!content.contains("import com.fasterxml.jackson")) {
                            content = addJacksonImports(content);
                        }

                        // Add @JsonProperty to fields with @XmlElement
                        content = enhanceWithJsonAnnotations(content);

                        Files.writeString(path, content);
                        log.debug("Enhanced file: {}", path.getFileName());

                    } catch (IOException e) {
                        log.warn("Failed to enhance file: {}", path, e);
                        result.addWarning("Could not enhance file: " + path.getFileName());
                    }
                });

        } catch (IOException e) {
            log.error("Failed to enhance generated files", e);
            result.addWarning("File enhancement incomplete: " + e.getMessage());
        }
    }

    /**
     * Adds Jackson import statements to the source file.
     */
    private String addJacksonImports(String content) {
        int packageEnd = content.indexOf(";", content.indexOf("package")) + 1;
        String imports = "\n\nimport com.fasterxml.jackson.annotation.JsonProperty;" +
                        "\nimport com.fasterxml.jackson.annotation.JsonRootName;";

        return content.substring(0, packageEnd) + imports + content.substring(packageEnd);
    }

    /**
     * Enhances content with JSON annotations.
     */
    private String enhanceWithJsonAnnotations(String content) {
        // Add @JsonProperty next to @XmlElement
        content = content.replaceAll(
            "(@XmlElement\\([^)]*name\\s*=\\s*\"([^\"]+)\"[^)]*\\))",
            "$1\n    @JsonProperty(\"$2\")"
        );

        // Add @JsonRootName next to @XmlRootElement
        content = content.replaceAll(
            "(@XmlRootElement\\([^)]*name\\s*=\\s*\"([^\"]+)\"[^)]*\\))",
            "$1\n@JsonRootName(\"$2\")"
        );

        return content;
    }

    /**
     * Collects all generated Java files.
     */
    private void collectGeneratedFiles(Path sourceDir, GenerationResult result) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    String relativePath = sourceDir.relativize(path).toString();
                    result.addGeneratedFile(relativePath);
                    log.debug("Generated file: {}", relativePath);
                });
        }
    }

    /**
     * Creates output directory for generated sources.
     */
    private Path createOutputDirectory(String serviceName, String type) throws IOException {
        String baseDir = properties.getCompilation().getTempDirectory();
        Path path = Paths.get(baseDir, serviceName, type);

        if (Files.exists(path)) {
            deleteDirectory(path.toFile());
        }

        Files.createDirectories(path);
        log.debug("Created output directory: {}", path);

        return path;
    }

    /**
     * Deletes directory recursively.
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * Gets package name for generated classes.
     */
    private String getPackageName(String serviceName) {
        return "com.generated." + serviceName.replace("-", "").toLowerCase() + ".model";
    }

    /**
     * Generation result class.
     */
    @lombok.Data
    public static class GenerationResult {
        private boolean success;
        private String serviceName;
        private String sourceOutputDirectory;
        private List<String> generatedFiles = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addGeneratedFile(String file) {
            this.generatedFiles.add(file);
        }

        public void addError(String error) {
            this.errors.add(error);
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }
}
