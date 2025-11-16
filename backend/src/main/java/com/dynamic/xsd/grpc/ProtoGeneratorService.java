package com.dynamic.xsd.grpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;

/**
 * Generates Protocol Buffer .proto files from XSD schemas.
 * Handles complex type conversion, field mapping, and service definitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtoGeneratorService {

    private final XsdToProtoTypeMapper typeMapper;

    /**
     * Generates a .proto file from XSD content.
     */
    public String generateProtoFromXsd(String serviceName, String targetNamespace, String xsdContent) {
        log.info("Generating .proto file for service: {}", serviceName);

        try {
            // Parse XSD
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xsdDoc = builder.parse(new InputSource(new StringReader(xsdContent)));

            // Extract elements and complex types
            Map<String, ProtoMessage> messages = extractMessages(xsdDoc);

            // Generate .proto file content
            return buildProtoFile(serviceName, targetNamespace, messages);

        } catch (Exception e) {
            log.error("Failed to generate .proto file for service: {}", serviceName, e);
            throw new RuntimeException("Proto generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts messages from XSD document.
     */
    private Map<String, ProtoMessage> extractMessages(Document xsdDoc) {
        Map<String, ProtoMessage> messages = new LinkedHashMap<>();

        // Extract complex types
        NodeList complexTypes = xsdDoc.getElementsByTagNameNS("*", "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element complexType = (Element) complexTypes.item(i);
            String typeName = complexType.getAttribute("name");

            if (typeName != null && !typeName.isEmpty()) {
                ProtoMessage message = extractComplexType(complexType, typeName);
                messages.put(typeName, message);
            }
        }

        // Extract elements (potential root elements)
        NodeList elements = xsdDoc.getElementsByTagNameNS("*", "element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String elementName = element.getAttribute("name");
            String type = element.getAttribute("type");

            if (elementName != null && !elementName.isEmpty() && isGlobalElement(element)) {
                // Create a message for the element if it has inline complex type
                if (type == null || type.isEmpty()) {
                    NodeList children = element.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        if (children.item(j) instanceof Element) {
                            Element child = (Element) children.item(j);
                            if (child.getLocalName().equals("complexType")) {
                                ProtoMessage message = extractComplexType(child, elementName);
                                messages.put(elementName, message);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return messages;
    }

    /**
     * Extracts a complex type as a Proto message.
     */
    private ProtoMessage extractComplexType(Element complexType, String typeName) {
        ProtoMessage message = new ProtoMessage();
        message.name = typeMapper.toPascalCase(typeName);
        message.fields = new ArrayList<>();

        // Extract sequence elements
        NodeList sequences = complexType.getElementsByTagNameNS("*", "sequence");
        if (sequences.getLength() > 0) {
            Element sequence = (Element) sequences.item(0);
            NodeList elements = sequence.getElementsByTagNameNS("*", "element");

            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                ProtoField field = extractField(element, i + 1);
                message.fields.add(field);
            }
        }

        // Extract attributes
        NodeList attributes = complexType.getElementsByTagNameNS("*", "attribute");
        int fieldNumber = message.fields.size() + 1;
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attribute = (Element) attributes.item(i);
            ProtoField field = extractAttribute(attribute, fieldNumber++);
            message.fields.add(field);
        }

        return message;
    }

    /**
     * Extracts a field from an XSD element.
     */
    private ProtoField extractField(Element element, int fieldNumber) {
        ProtoField field = new ProtoField();
        field.number = fieldNumber;
        field.name = typeMapper.toSnakeCase(element.getAttribute("name"));

        String xsdType = element.getAttribute("type");
        field.type = typeMapper.mapType(xsdType);

        // Check for min/max occurs
        String minOccurs = element.getAttribute("minOccurs");
        String maxOccurs = element.getAttribute("maxOccurs");

        int min = minOccurs.isEmpty() ? 1 : Integer.parseInt(minOccurs);
        int max = maxOccurs.isEmpty() ? 1 : (maxOccurs.equals("unbounded") ? -1 : Integer.parseInt(maxOccurs));

        field.optional = typeMapper.shouldBeOptional(min);
        field.repeated = typeMapper.shouldBeRepeated(xsdType, max);

        return field;
    }

    /**
     * Extracts a field from an XSD attribute.
     */
    private ProtoField extractAttribute(Element attribute, int fieldNumber) {
        ProtoField field = new ProtoField();
        field.number = fieldNumber;
        field.name = typeMapper.toSnakeCase(attribute.getAttribute("name"));

        String xsdType = attribute.getAttribute("type");
        field.type = typeMapper.mapType(xsdType);

        String use = attribute.getAttribute("use");
        field.optional = !"required".equals(use);
        field.repeated = false;

        return field;
    }

    /**
     * Builds the complete .proto file content.
     */
    private String buildProtoFile(String serviceName, String targetNamespace, Map<String, ProtoMessage> messages) {
        StringBuilder proto = new StringBuilder();

        // Syntax
        proto.append("syntax = \"proto3\";\n\n");

        // Package
        String packageName = generatePackageName(targetNamespace, serviceName);
        proto.append("package ").append(packageName).append(";\n\n");

        // Java options
        proto.append("option java_multiple_files = true;\n");
        proto.append("option java_package = \"com.dynamic.xsd.generated.").append(serviceName.toLowerCase()).append("\";\n");
        proto.append("option java_outer_classname = \"").append(typeMapper.toPascalCase(serviceName)).append("Proto\";\n\n");

        // Imports for well-known types
        Set<String> imports = collectImports(messages);
        for (String importStmt : imports) {
            proto.append(importStmt).append("\n");
        }
        if (!imports.isEmpty()) {
            proto.append("\n");
        }

        // Messages
        for (ProtoMessage message : messages.values()) {
            proto.append(generateMessage(message));
            proto.append("\n");
        }

        // Service definition
        proto.append(generateService(serviceName, messages));

        return proto.toString();
    }

    /**
     * Generates a Proto message definition.
     */
    private String generateMessage(ProtoMessage message) {
        StringBuilder msg = new StringBuilder();

        msg.append("// ").append(message.name).append(" message\n");
        msg.append("message ").append(message.name).append(" {\n");

        for (ProtoField field : message.fields) {
            msg.append("  ");

            // Field modifier (repeated or optional)
            if (field.repeated) {
                msg.append("repeated ");
            } else if (field.optional) {
                msg.append("optional ");
            }

            // Field type and name
            msg.append(field.type).append(" ").append(field.name);
            msg.append(" = ").append(field.number).append(";\n");
        }

        msg.append("}\n");

        return msg.toString();
    }

    /**
     * Generates a gRPC service definition.
     */
    private String generateService(String serviceName, Map<String, ProtoMessage> messages) {
        StringBuilder service = new StringBuilder();

        String serviceNamePascal = typeMapper.toPascalCase(serviceName);
        service.append("// ").append(serviceNamePascal).append(" service\n");
        service.append("service ").append(serviceNamePascal).append("Service {\n");

        // Generate CRUD methods for each message
        for (ProtoMessage message : messages.values()) {
            // Create
            service.append("  rpc Create").append(message.name)
                    .append("(").append(message.name).append("Request) returns (").append(message.name).append("Response);\n");

            // Get
            service.append("  rpc Get").append(message.name)
                    .append("(GetRequest) returns (").append(message.name).append("Response);\n");

            // List
            service.append("  rpc List").append(message.name)
                    .append("(ListRequest) returns (").append(message.name).append("ListResponse);\n");

            // Update
            service.append("  rpc Update").append(message.name)
                    .append("(").append(message.name).append("Request) returns (").append(message.name).append("Response);\n");

            // Delete
            service.append("  rpc Delete").append(message.name)
                    .append("(DeleteRequest) returns (DeleteResponse);\n");

            service.append("\n");
        }

        service.append("}\n\n");

        // Add common request/response messages
        service.append(generateCommonMessages());

        return service.toString();
    }

    /**
     * Generates common request/response messages.
     */
    private String generateCommonMessages() {
        return """
                // Common request messages
                message GetRequest {
                  string id = 1;
                }

                message ListRequest {
                  optional int32 page_size = 1;
                  optional int32 page_number = 2;
                }

                message DeleteRequest {
                  string id = 1;
                }

                // Common response messages
                message DeleteResponse {
                  bool success = 1;
                  optional string message = 2;
                }
                """;
    }

    /**
     * Collects all necessary imports for well-known types.
     */
    private Set<String> collectImports(Map<String, ProtoMessage> messages) {
        Set<String> imports = new HashSet<>();

        for (ProtoMessage message : messages.values()) {
            for (ProtoField field : message.fields) {
                if (typeMapper.isWellKnownType(field.type)) {
                    String importStmt = typeMapper.getWellKnownTypeImport(field.type);
                    if (!importStmt.isEmpty()) {
                        imports.add(importStmt);
                    }
                }
            }
        }

        return imports;
    }

    /**
     * Generates package name from target namespace and service name.
     */
    private String generatePackageName(String targetNamespace, String serviceName) {
        // Extract domain from namespace (e.g., "http://example.com/schemas" -> "com.example")
        String domain = targetNamespace
                .replaceAll("https?://", "")
                .replaceAll("/.*", "")
                .replaceAll("[^a-zA-Z0-9.]", "");

        String[] parts = domain.split("\\.");
        StringBuilder packageName = new StringBuilder();

        // Reverse domain (com.example)
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty()) {
                if (packageName.length() > 0) {
                    packageName.append(".");
                }
                packageName.append(parts[i].toLowerCase());
            }
        }

        packageName.append(".").append(serviceName.toLowerCase());

        return packageName.toString();
    }

    /**
     * Checks if an element is a global element (direct child of schema).
     */
    private boolean isGlobalElement(Element element) {
        Node parent = element.getParentNode();
        return parent != null && "schema".equals(parent.getLocalName());
    }

    /**
     * Inner class representing a Proto message.
     */
    private static class ProtoMessage {
        String name;
        List<ProtoField> fields;
    }

    /**
     * Inner class representing a Proto field.
     */
    private static class ProtoField {
        int number;
        String name;
        String type;
        boolean optional;
        boolean repeated;
    }
}
