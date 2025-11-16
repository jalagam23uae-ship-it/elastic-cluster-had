package com.dynamic.xsd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for the dynamic service generation platform.
 */
@Data
@Component
@ConfigurationProperties(prefix = "dynamic-service")
public class DynamicServiceProperties {

    private XsdConfig xsd = new XsdConfig();
    private CompilationConfig compilation = new CompilationConfig();
    private ClassLoaderConfig classloader = new ClassLoaderConfig();
    private RestConfig rest = new RestConfig();
    private SoapConfig soap = new SoapConfig();
    private StorageConfig storage = new StorageConfig();
    private SecurityConfig security = new SecurityConfig();
    private MonitoringConfig monitoring = new MonitoringConfig();

    @Data
    public static class XsdConfig {
        private String storagePath = "./xsd-storage";
        private Long maxFileSize = 5242880L;  // 5MB
        private Boolean validationEnabled = true;
        private Boolean cacheEnabled = true;
    }

    @Data
    public static class CompilationConfig {
        private String tempDirectory = "./temp/compile";
        private String outputDirectory = "./temp/classes";
        private Boolean keepSources = true;
        private String javaVersion = "21";
        private List<String> compilerOptions = List.of("-parameters", "-g");
    }

    @Data
    public static class ClassLoaderConfig {
        private Boolean isolationEnabled = true;
        private Boolean parentFirst = false;
        private Integer maxServices = 100;
    }

    @Data
    public static class RestConfig {
        private String basePath = "/api/dynamic";
        private Boolean versioningEnabled = true;
        private Integer defaultPageSize = 20;
        private Integer maxPageSize = 100;
    }

    @Data
    public static class SoapConfig {
        private String basePath = "/ws";
        private Boolean wsdlEnabled = true;
        private String soapVersion = "1.2";
    }

    @Data
    public static class StorageConfig {
        private String type = "in-memory";  // or 'database'
    }

    @Data
    public static class SecurityConfig {
        private JwtConfig jwt = new JwtConfig();
        private Boolean enabled = true;
        private Integer rateLimit = 100;

        @Data
        public static class JwtConfig {
            private String secret;
            private Long expiration = 86400000L;  // 24 hours
            private Long refreshExpiration = 604800000L;  // 7 days
        }
    }

    @Data
    public static class MonitoringConfig {
        private Boolean metricsEnabled = true;
        private Boolean auditEnabled = true;
    }
}
