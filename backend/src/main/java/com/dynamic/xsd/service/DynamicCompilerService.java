package com.dynamic.xsd.service;

import com.dynamic.xsd.config.DynamicServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Service for dynamically compiling Java source files using Java Compiler API.
 *
 * Features:
 * - Runtime compilation of generated POJOs
 * - Diagnostic collection for error reporting
 * - Classpath management
 * - Compilation caching
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCompilerService {

    private final DynamicServiceProperties properties;

    /**
     * Compiles Java source files from the source directory.
     *
     * @param serviceName Service name
     * @param sourceDirectory Directory containing Java source files
     * @return Compilation result
     */
    public CompilationResult compile(String serviceName, String sourceDirectory) {
        log.info("Starting compilation for service: {}", serviceName);

        CompilationResult result = new CompilationResult();
        result.setServiceName(serviceName);
        result.setSourceDirectory(sourceDirectory);

        try {
            // Create output directory for compiled classes
            Path classOutputDir = createOutputDirectory(serviceName);
            result.setClassOutputDirectory(classOutputDir.toString());

            // Collect all Java source files
            List<File> sourceFiles = collectSourceFiles(sourceDirectory);
            if (sourceFiles.isEmpty()) {
                result.setSuccess(false);
                result.addError("No Java source files found in: " + sourceDirectory);
                return result;
            }

            log.debug("Found {} source files to compile", sourceFiles.size());

            // Perform compilation
            boolean compilationSuccess = performCompilation(sourceFiles, classOutputDir, result);
            result.setSuccess(compilationSuccess);

            if (compilationSuccess) {
                // Collect compiled class files
                collectCompiledClasses(classOutputDir, result);
                log.info("Compilation successful for service: {}. Compiled {} classes",
                    serviceName, result.getCompiledClasses().size());
            } else {
                log.error("Compilation failed for service: {}. Errors: {}",
                    serviceName, result.getErrors().size());
            }

        } catch (Exception e) {
            log.error("Compilation failed for service: {}", serviceName, e);
            result.setSuccess(false);
            result.addError("Compilation exception: " + e.getMessage());
        }

        return result;
    }

    /**
     * Performs the actual compilation using JavaCompiler.
     */
    private boolean performCompilation(List<File> sourceFiles, Path classOutputDir,
                                       CompilationResult result) {
        // Get Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            result.addError("Java Compiler not available. Ensure JDK (not JRE) is being used.");
            return false;
        }

        // Get diagnostic collector for error/warning collection
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // Get standard file manager
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnostics, Locale.getDefault(), null
        );

        try {
            // Set output directory
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
                List.of(classOutputDir.toFile()));

            // Get compilation units
            Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(sourceFiles);

            // Prepare compiler options
            List<String> options = prepareCompilerOptions(classOutputDir);

            // Create compilation task
            JavaCompiler.CompilationTask task = compiler.getTask(
                null,                    // Writer for additional output
                fileManager,             // File manager
                diagnostics,             // Diagnostic listener
                options,                 // Compiler options
                null,                    // Annotation processor classes
                compilationUnits         // Compilation units
            );

            // Execute compilation
            boolean success = task.call();

            // Process diagnostics
            processDiagnostics(diagnostics, result);

            fileManager.close();

            return success;

        } catch (IOException e) {
            log.error("Compilation I/O error", e);
            result.addError("I/O error during compilation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prepares compiler options.
     */
    private List<String> prepareCompilerOptions(Path classOutputDir) {
        List<String> options = new ArrayList<>();

        // Add custom compiler options from configuration
        if (properties.getCompilation().getCompilerOptions() != null) {
            options.addAll(properties.getCompilation().getCompilerOptions());
        }

        // Add classpath
        String classpath = buildClasspath(classOutputDir);
        options.add("-classpath");
        options.add(classpath);

        // Verbose output for debugging (optional)
        // options.add("-verbose");

        log.debug("Compiler options: {}", options);

        return options;
    }

    /**
     * Builds classpath for compilation.
     * Includes all runtime dependencies needed by generated classes.
     * For Spring Boot, extracts JARs from BOOT-INF/lib temporarily.
     */
    private String buildClasspath(Path outputDir) {
        List<String> classpathEntries = new ArrayList<>();

        // Add output directory
        classpathEntries.add(outputDir.toString());

        // For Spring Boot fat JARs, we need to extract dependencies from BOOT-INF/lib
        // because Java compiler cannot read from nested JARs
        try {
            // Get the application's JAR file
            String mainJar = System.getProperty("java.class.path");
            if (mainJar != null && mainJar.endsWith(".jar")) {
                Path jarPath = Paths.get(mainJar);
                if (Files.exists(jarPath)) {
                    log.debug("Checking Spring Boot JAR: {}", jarPath);

                    // Extract required JARs from BOOT-INF/lib/
                    Path tempLibDir = Paths.get(System.getProperty("java.io.tmpdir"), "xsd-platform-libs");
                    Files.createDirectories(tempLibDir);

                    String[] requiredDependencies = {
                        "jackson-annotations",
                        "jackson-databind",
                        "jackson-core",
                        "jakarta.xml.bind-api",
                        "jakarta.activation-api",
                        "jaxb-runtime",
                        "jaxb-core",
                        "txw2",
                        "istack-commons-runtime",
                        "lombok"
                    };

                    // Extract JARs from Spring Boot fat JAR
                    extractBootInfLibs(jarPath, tempLibDir, requiredDependencies, classpathEntries);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract BOOT-INF/lib JARs: {}", e.getMessage());
        }

        // Log classpath for debugging
        log.info("Compilation classpath entries: {}", classpathEntries.size());
        classpathEntries.forEach(entry -> log.info("Classpath entry: {}", entry));

        return String.join(File.pathSeparator, classpathEntries);
    }

    /**
     * Extracts required JARs from Spring Boot fat JAR's BOOT-INF/lib directory.
     */
    private void extractBootInfLibs(Path jarPath, Path tempLibDir, String[] requiredDeps, List<String> classpathEntries) {
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(jarPath.toFile())) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Check if this entry is a JAR in BOOT-INF/lib/
                if (entryName.startsWith("BOOT-INF/lib/") && entryName.endsWith(".jar")) {
                    String jarName = entryName.substring(13); // Remove "BOOT-INF/lib/" prefix

                    // Check if this is one of our required dependencies
                    for (String requiredDep : requiredDeps) {
                        if (jarName.startsWith(requiredDep)) {
                            Path extractedJar = tempLibDir.resolve(jarName);

                            // Extract only if not already extracted
                            if (!Files.exists(extractedJar)) {
                                try (java.io.InputStream is = zipFile.getInputStream(entry);
                                     java.io.FileOutputStream fos = new java.io.FileOutputStream(extractedJar.toFile())) {
                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    while ((bytesRead = is.read(buffer)) != -1) {
                                        fos.write(buffer, 0, bytesRead);
                                    }
                                    log.debug("Extracted JAR: {}", jarName);
                                }
                            }

                            classpathEntries.add(extractedJar.toString());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract JARs from BOOT-INF/lib", e);
        }
    }

    /**
     * Processes compilation diagnostics.
     */
    private void processDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics,
                                    CompilationResult result) {
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            String message = formatDiagnostic(diagnostic);

            switch (diagnostic.getKind()) {
                case ERROR:
                    result.addError(message);
                    log.error("Compilation error: {}", message);
                    break;
                case WARNING:
                case MANDATORY_WARNING:
                    result.addWarning(message);
                    log.warn("Compilation warning: {}", message);
                    break;
                case NOTE:
                    log.debug("Compilation note: {}", message);
                    break;
                default:
                    log.trace("Compilation diagnostic: {}", message);
            }
        }
    }

    /**
     * Formats diagnostic message.
     */
    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        StringBuilder sb = new StringBuilder();

        if (diagnostic.getSource() != null) {
            sb.append(diagnostic.getSource().getName());
        }

        if (diagnostic.getLineNumber() != Diagnostic.NOPOS) {
            sb.append(":").append(diagnostic.getLineNumber());
        }

        if (diagnostic.getColumnNumber() != Diagnostic.NOPOS) {
            sb.append(":").append(diagnostic.getColumnNumber());
        }

        sb.append(" - ").append(diagnostic.getMessage(Locale.getDefault()));

        return sb.toString();
    }

    /**
     * Collects all Java source files from directory.
     */
    private List<File> collectSourceFiles(String sourceDirectory) throws IOException {
        List<File> sourceFiles = new ArrayList<>();
        Path sourcePath = Paths.get(sourceDirectory);

        if (!Files.exists(sourcePath)) {
            log.warn("Source directory does not exist: {}", sourceDirectory);
            return sourceFiles;
        }

        try (Stream<Path> paths = Files.walk(sourcePath)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sourceFiles.add(p.toFile()));
        }

        return sourceFiles;
    }

    /**
     * Collects compiled class files.
     */
    private void collectCompiledClasses(Path classOutputDir, CompilationResult result) throws IOException {
        try (Stream<Path> paths = Files.walk(classOutputDir)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(path -> {
                    String relativePath = classOutputDir.relativize(path).toString();
                    // Convert file path to class name
                    String className = relativePath
                        .replace(File.separator, ".")
                        .replace(".class", "");
                    result.addCompiledClass(className);
                    log.debug("Compiled class: {}", className);
                });
        }
    }

    /**
     * Creates output directory for compiled classes.
     */
    private Path createOutputDirectory(String serviceName) throws IOException {
        String baseDir = properties.getCompilation().getOutputDirectory();
        Path path = Paths.get(baseDir, serviceName);

        if (Files.exists(path)) {
            // Clean existing directory
            deleteDirectory(path.toFile());
        }

        Files.createDirectories(path);
        log.debug("Created class output directory: {}", path);

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
     * Compilation result class.
     */
    @lombok.Data
    public static class CompilationResult {
        private boolean success;
        private String serviceName;
        private String sourceDirectory;
        private String classOutputDirectory;
        private List<String> compiledClasses = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addCompiledClass(String className) {
            this.compiledClasses.add(className);
        }

        public void addError(String error) {
            this.errors.add(error);
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }
}
