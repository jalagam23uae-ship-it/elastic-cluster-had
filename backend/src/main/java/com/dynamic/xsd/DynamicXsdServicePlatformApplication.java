package com.dynamic.xsd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for Dynamic XSD Service Generation Platform.
 *
 * This platform allows users to:
 * - Upload XSD schemas
 * - Automatically generate Java POJOs from XSD
 * - Dynamically compile and load generated classes
 * - Auto-generate REST APIs (JSON/XML support)
 * - Auto-generate SOAP Web Services with WSDL
 * - Manage service lifecycle dynamically
 *
 * @author Dynamic XSD Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class DynamicXsdServicePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicXsdServicePlatformApplication.class, args);
    }

}
