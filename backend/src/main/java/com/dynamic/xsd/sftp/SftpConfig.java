package com.dynamic.xsd.sftp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

/**
 * SFTP configuration for file-based service exposure.
 * Enables services to be accessed via SFTP protocol.
 */
@Slf4j
@Configuration
public class SftpConfig {

    @Value("${sftp.host:localhost}")
    private String host;

    @Value("${sftp.port:22}")
    private int port;

    @Value("${sftp.user:sftpuser}")
    private String user;

    @Value("${sftp.password:sftppass}")
    private String password;

    @Value("${sftp.private-key:#{null}}")
    private String privateKey;

    @Value("${sftp.root-directory:/sftp}")
    private String rootDirectory;

    /**
     * SFTP Session Factory for connections.
     */
    @Bean
    public DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUser(user);

        if (privateKey != null && !privateKey.isEmpty()) {
            factory.setPrivateKey(new FileSystemResource(privateKey));
        } else {
            factory.setPassword(password);
        }

        factory.setAllowUnknownKeys(true);

        log.info("SFTP Session Factory configured for {}:{}", host, port);

        return factory;
    }

    /**
     * Get root directory for SFTP operations.
     */
    public String getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Get directory for a specific service.
     */
    public String getServiceDirectory(String serviceName) {
        return rootDirectory + "/" + serviceName;
    }

    /**
     * Get request directory for a service.
     */
    public String getRequestDirectory(String serviceName) {
        return getServiceDirectory(serviceName) + "/requests";
    }

    /**
     * Get response directory for a service.
     */
    public String getResponseDirectory(String serviceName) {
        return getServiceDirectory(serviceName) + "/responses";
    }

    /**
     * Get error directory for a service.
     */
    public String getErrorDirectory(String serviceName) {
        return getServiceDirectory(serviceName) + "/errors";
    }

    /**
     * Get archive directory for a service.
     */
    public String getArchiveDirectory(String serviceName) {
        return getServiceDirectory(serviceName) + "/archive";
    }
}
