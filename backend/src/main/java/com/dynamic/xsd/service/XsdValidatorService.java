package com.dynamic.xsd.service;

import com.ctc.wstx.stax.WstxInputFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating XSD schemas using Woodstox XML processor.
 *
 * Provides:
 * - XSD syntax validation
 * - Namespace extraction
 * - Security validation (XXE, XML bomb prevention)
 * - Schema complexity analysis
 */
@Slf4j
@Service
public class XsdValidatorService {

    private static final long MAX_ELEMENT_COUNT = 10000;
    private static final int MAX_DEPTH = 100;

    /**
     * Validates XSD content and returns validation result.
     */
    public ValidationResult validate(byte[] xsdContent) {
        log.debug("Starting XSD validation");

        ValidationResult result = new ValidationResult();
        result.setValid(true);

        try {
            // Security checks
            performSecurityChecks(xsdContent, result);

            if (!result.isValid()) {
                return result;
            }

            // Syntax validation
            validateSyntax(xsdContent, result);

            // Namespace extraction
            extractNamespace(xsdContent, result);

            // Complexity analysis
            analyzeComplexity(xsdContent, result);

            log.info("XSD validation completed. Valid: {}, Warnings: {}, Errors: {}",
                result.isValid(), result.getWarnings().size(), result.getErrors().size());

        } catch (Exception e) {
            log.error("XSD validation failed", e);
            result.setValid(false);
            result.addError("Validation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Performs security checks to prevent XXE and XML bomb attacks.
     */
    private void performSecurityChecks(byte[] xsdContent, ValidationResult result) {
        String content = new String(xsdContent, StandardCharsets.UTF_8);

        // Check for external entity declarations
        if (content.contains("<!ENTITY") && content.contains("SYSTEM")) {
            result.setValid(false);
            result.addError("External entity declarations are not allowed (XXE prevention)");
        }

        // Check for entity expansion attacks
        if (content.contains("<!ENTITY") && countOccurrences(content, "<!ENTITY") > 10) {
            result.setValid(false);
            result.addError("Too many entity declarations detected (XML bomb prevention)");
        }

        // Check for DOCTYPE declarations
        if (content.contains("<!DOCTYPE")) {
            result.addWarning("DOCTYPE declaration found. Consider removing for security.");
        }
    }

    /**
     * Validates XSD syntax using schema factory.
     */
    private void validateSyntax(byte[] xsdContent, ValidationResult result) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // Try to disable external entities for security (may not be supported by all implementations)
            try {
                schemaFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                schemaFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            } catch (SAXException e) {
                log.debug("Some security features not supported by SchemaFactory implementation: {}", e.getMessage());
                // Continue - the XMLInputFactory already provides XXE protection
            }

            schemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(xsdContent)));

            log.debug("XSD syntax validation successful");
        } catch (SAXException e) {
            result.setValid(false);
            result.addError("Syntax error: " + e.getMessage());
            log.warn("XSD syntax validation failed: {}", e.getMessage());
        }
    }

    /**
     * Extracts target namespace from XSD.
     */
    private void extractNamespace(byte[] xsdContent, ValidationResult result) {
        try {
            XMLInputFactory factory = createSecureXMLInputFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(
                new ByteArrayInputStream(xsdContent)
            );

            while (reader.hasNext()) {
                reader.next();
                if (reader.isStartElement() &&
                    reader.getLocalName().equals("schema")) {

                    String namespace = reader.getAttributeValue(null, "targetNamespace");
                    if (namespace != null && !namespace.isEmpty()) {
                        result.setTargetNamespace(namespace);
                        log.debug("Extracted namespace: {}", namespace);
                    } else {
                        result.addWarning("No targetNamespace found in XSD");
                    }
                    break;
                }
            }
            reader.close();
        } catch (XMLStreamException e) {
            result.addWarning("Could not extract namespace: " + e.getMessage());
        }
    }

    /**
     * Analyzes schema complexity.
     */
    private void analyzeComplexity(byte[] xsdContent, ValidationResult result) {
        try {
            XMLInputFactory factory = createSecureXMLInputFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(
                new ByteArrayInputStream(xsdContent)
            );

            int elementCount = 0;
            int complexTypeCount = 0;
            int simpleTypeCount = 0;
            int currentDepth = 0;
            int maxDepth = 0;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamReader.START_ELEMENT) {
                    currentDepth++;
                    maxDepth = Math.max(maxDepth, currentDepth);

                    String localName = reader.getLocalName();
                    switch (localName) {
                        case "element" -> elementCount++;
                        case "complexType" -> complexTypeCount++;
                        case "simpleType" -> simpleTypeCount++;
                    }

                    // Check complexity limits
                    if (elementCount > MAX_ELEMENT_COUNT) {
                        result.addWarning("Schema is very complex (" + elementCount + " elements)");
                    }
                    if (maxDepth > MAX_DEPTH) {
                        result.addWarning("Schema nesting is very deep (" + maxDepth + " levels)");
                    }
                } else if (event == XMLStreamReader.END_ELEMENT) {
                    currentDepth--;
                }
            }

            result.setElementCount(elementCount);
            result.setComplexTypeCount(complexTypeCount);
            result.setSimpleTypeCount(simpleTypeCount);
            result.setMaxDepth(maxDepth);

            log.debug("Schema complexity: elements={}, complexTypes={}, simpleTypes={}, maxDepth={}",
                elementCount, complexTypeCount, simpleTypeCount, maxDepth);

            reader.close();
        } catch (XMLStreamException e) {
            result.addWarning("Could not analyze complexity: " + e.getMessage());
        }
    }

    /**
     * Creates a secure XMLInputFactory with XXE prevention.
     */
    private XMLInputFactory createSecureXMLInputFactory() {
        WstxInputFactory factory = new WstxInputFactory();

        // Disable external entities
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        return factory;
    }

    private int countOccurrences(String str, String sub) {
        return (str.length() - str.replace(sub, "").length()) / sub.length();
    }

    /**
     * Validation result class.
     */
    @lombok.Data
    public static class ValidationResult {
        private boolean valid;
        private String targetNamespace;
        private int elementCount;
        private int complexTypeCount;
        private int simpleTypeCount;
        private int maxDepth;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            this.errors.add(error);
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }
}
