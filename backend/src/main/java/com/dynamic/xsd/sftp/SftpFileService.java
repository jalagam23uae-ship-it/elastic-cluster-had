package com.dynamic.xsd.sftp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Service for handling SFTP file operations.
 * Manages file-based service interactions using JSON files.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SftpFileService {

    private final ObjectMapper objectMapper;
    private final SftpConfig sftpConfig;

    /**
     * Write request file to SFTP directory.
     */
    public String writeRequestFile(String serviceName, String operation, Map<String, Object> data) {
        try {
            String requestDir = sftpConfig.getRequestDirectory(serviceName);
            ensureDirectoryExists(requestDir);

            String filename = generateFilename(operation);
            Path filePath = Paths.get(requestDir, filename);

            Map<String, Object> request = new HashMap<>();
            request.put("service", serviceName);
            request.put("operation", operation);
            request.put("data", data);
            request.put("timestamp", System.currentTimeMillis());
            request.put("requestId", UUID.randomUUID().toString());

            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);

            Files.writeString(filePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("SFTP: Wrote request file {} for service {}", filename, serviceName);
            return filename;

        } catch (IOException e) {
            log.error("Failed to write SFTP request file for service {}: {}", serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to write SFTP request file: " + e.getMessage(), e);
        }
    }

    /**
     * Write response file to SFTP directory.
     */
    public void writeResponseFile(String serviceName, String requestId, Map<String, Object> responseData) {
        try {
            String responseDir = sftpConfig.getResponseDirectory(serviceName);
            ensureDirectoryExists(responseDir);

            String filename = requestId + "_response.json";
            Path filePath = Paths.get(responseDir, filename);

            Map<String, Object> response = new HashMap<>();
            response.put("requestId", requestId);
            response.put("service", serviceName);
            response.put("data", responseData);
            response.put("timestamp", System.currentTimeMillis());
            response.put("success", true);

            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(response);

            Files.writeString(filePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("SFTP: Wrote response file {} for service {}", filename, serviceName);

        } catch (IOException e) {
            log.error("Failed to write SFTP response file for service {}: {}", serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to write SFTP response file: " + e.getMessage(), e);
        }
    }

    /**
     * Write error file to SFTP directory.
     */
    public void writeErrorFile(String serviceName, String requestId, String error) {
        try {
            String errorDir = sftpConfig.getErrorDirectory(serviceName);
            ensureDirectoryExists(errorDir);

            String filename = requestId + "_error.json";
            Path filePath = Paths.get(errorDir, filename);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("requestId", requestId);
            errorResponse.put("service", serviceName);
            errorResponse.put("error", error);
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("success", false);

            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(errorResponse);

            Files.writeString(filePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.warn("SFTP: Wrote error file {} for service {}", filename, serviceName);

        } catch (IOException e) {
            log.error("Failed to write SFTP error file for service {}: {}", serviceName, e.getMessage(), e);
        }
    }

    /**
     * Read and process request files from SFTP directory.
     */
    public List<Map<String, Object>> readRequestFiles(String serviceName) {
        try {
            String requestDir = sftpConfig.getRequestDirectory(serviceName);
            ensureDirectoryExists(requestDir);

            File dir = new File(requestDir);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

            if (files == null || files.length == 0) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> requests = new ArrayList<>();
            for (File file : files) {
                try {
                    String content = Files.readString(file.toPath());
                    @SuppressWarnings("unchecked")
                    Map<String, Object> request = objectMapper.readValue(content, Map.class);
                    requests.add(request);

                    // Archive the file after reading
                    archiveFile(serviceName, file);

                } catch (IOException e) {
                    log.error("Failed to read request file {}: {}", file.getName(), e.getMessage());
                }
            }

            log.info("SFTP: Read {} request files from service {}", requests.size(), serviceName);
            return requests;

        } catch (IOException e) {
            log.error("Failed to read SFTP request files for service {}: {}", serviceName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Archive a processed file.
     */
    private void archiveFile(String serviceName, File file) {
        try {
            String archiveDir = sftpConfig.getArchiveDirectory(serviceName);
            ensureDirectoryExists(archiveDir);

            String timestamp = String.valueOf(System.currentTimeMillis());
            String archivedName = timestamp + "_" + file.getName();
            Path archivePath = Paths.get(archiveDir, archivedName);

            Files.move(file.toPath(), archivePath);

            log.debug("SFTP: Archived file {} to {}", file.getName(), archivePath);

        } catch (IOException e) {
            log.error("Failed to archive file {}: {}", file.getName(), e.getMessage());
        }
    }

    /**
     * Ensure directory exists, create if necessary.
     */
    private void ensureDirectoryExists(String directory) throws IOException {
        Path path = Paths.get(directory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("SFTP: Created directory {}", directory);
        }
    }

    /**
     * Generate filename for operation.
     */
    private String generateFilename(String operation) {
        return String.format("%d_%s_request.json",
                System.currentTimeMillis(),
                operation.toLowerCase());
    }

    /**
     * Initialize service directories.
     */
    public void initializeServiceDirectories(String serviceName) {
        try {
            ensureDirectoryExists(sftpConfig.getServiceDirectory(serviceName));
            ensureDirectoryExists(sftpConfig.getRequestDirectory(serviceName));
            ensureDirectoryExists(sftpConfig.getResponseDirectory(serviceName));
            ensureDirectoryExists(sftpConfig.getErrorDirectory(serviceName));
            ensureDirectoryExists(sftpConfig.getArchiveDirectory(serviceName));

            log.info("SFTP: Initialized directories for service {}", serviceName);

        } catch (IOException e) {
            log.error("Failed to initialize SFTP directories for service {}: {}", serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize SFTP directories: " + e.getMessage(), e);
        }
    }

    /**
     * Get service statistics.
     */
    public Map<String, Object> getServiceStats(String serviceName) {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("requestsPending", countFiles(sftpConfig.getRequestDirectory(serviceName)));
            stats.put("responsesGenerated", countFiles(sftpConfig.getResponseDirectory(serviceName)));
            stats.put("errors", countFiles(sftpConfig.getErrorDirectory(serviceName)));
            stats.put("archived", countFiles(sftpConfig.getArchiveDirectory(serviceName)));

        } catch (Exception e) {
            log.error("Failed to get SFTP stats for service {}: {}", serviceName, e.getMessage());
        }

        return stats;
    }

    /**
     * Count files in directory.
     */
    private int countFiles(String directory) {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        return files != null ? files.length : 0;
    }
}
