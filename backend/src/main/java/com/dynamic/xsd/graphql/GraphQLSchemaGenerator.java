package com.dynamic.xsd.graphql;

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
 * Generates GraphQL schemas from XSD schemas.
 * Supports queries, mutations, and subscriptions with type-safe schema definitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQLSchemaGenerator {

    private final XsdToGraphQLTypeMapper typeMapper;

    /**
     * Generates a GraphQL schema (.graphqls) from XSD content.
     */
    public String generateSchemaFromXsd(String serviceName, String targetNamespace, String xsdContent) {
        log.info("Generating GraphQL schema for service: {}", serviceName);

        try {
            // Parse XSD
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xsdDoc = builder.parse(new InputSource(new StringReader(xsdContent)));

            // Extract types and elements
            Map<String, GraphQLType> types = extractTypes(xsdDoc);

            // Generate GraphQL schema
            return buildGraphQLSchema(serviceName, types);

        } catch (Exception e) {
            log.error("Failed to generate GraphQL schema for service: {}", serviceName, e);
            throw new RuntimeException("GraphQL schema generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts GraphQL types from XSD document.
     */
    private Map<String, GraphQLType> extractTypes(Document xsdDoc) {
        Map<String, GraphQLType> types = new LinkedHashMap<>();

        // Extract complex types
        NodeList complexTypes = xsdDoc.getElementsByTagNameNS("*", "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element complexType = (Element) complexTypes.item(i);
            String typeName = complexType.getAttribute("name");

            if (typeName != null && !typeName.isEmpty()) {
                GraphQLType type = extractComplexType(complexType, typeName);
                types.put(typeName, type);
            }
        }

        // Extract elements (potential root types)
        NodeList elements = xsdDoc.getElementsByTagNameNS("*", "element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String elementName = element.getAttribute("name");

            if (elementName != null && !elementName.isEmpty() && isGlobalElement(element)) {
                // Create a type for inline complex types
                NodeList children = element.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j) instanceof Element) {
                        Element child = (Element) children.item(j);
                        if (child.getLocalName().equals("complexType")) {
                            GraphQLType type = extractComplexType(child, elementName);
                            types.put(elementName, type);
                            break;
                        }
                    }
                }
            }
        }

        return types;
    }

    /**
     * Extracts a complex type as a GraphQL type.
     */
    private GraphQLType extractComplexType(Element complexType, String typeName) {
        GraphQLType type = new GraphQLType();
        type.name = typeMapper.toPascalCase(typeName);
        type.fields = new ArrayList<>();

        // Extract sequence elements
        NodeList sequences = complexType.getElementsByTagNameNS("*", "sequence");
        if (sequences.getLength() > 0) {
            Element sequence = (Element) sequences.item(0);
            NodeList elements = sequence.getElementsByTagNameNS("*", "element");

            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                GraphQLField field = extractField(element);
                type.fields.add(field);
            }
        }

        // Extract attributes
        NodeList attributes = complexType.getElementsByTagNameNS("*", "attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attribute = (Element) attributes.item(i);
            GraphQLField field = extractAttribute(attribute);
            type.fields.add(field);
        }

        return type;
    }

    /**
     * Extracts a field from an XSD element.
     */
    private GraphQLField extractField(Element element) {
        GraphQLField field = new GraphQLField();
        field.name = typeMapper.toCamelCase(element.getAttribute("name"));

        String xsdType = element.getAttribute("type");
        field.type = typeMapper.mapType(xsdType);

        // Check for min/max occurs
        String minOccurs = element.getAttribute("minOccurs");
        String maxOccurs = element.getAttribute("maxOccurs");

        int min = minOccurs.isEmpty() ? 1 : Integer.parseInt(minOccurs);
        int max = maxOccurs.isEmpty() ? 1 : (maxOccurs.equals("unbounded") ? -1 : Integer.parseInt(maxOccurs));

        field.nullable = (min == 0);
        field.isList = (max > 1 || max == -1);

        return field;
    }

    /**
     * Extracts a field from an XSD attribute.
     */
    private GraphQLField extractAttribute(Element attribute) {
        GraphQLField field = new GraphQLField();
        field.name = typeMapper.toCamelCase(attribute.getAttribute("name"));

        String xsdType = attribute.getAttribute("type");
        field.type = typeMapper.mapType(xsdType);

        String use = attribute.getAttribute("use");
        field.nullable = !"required".equals(use);
        field.isList = false;

        return field;
    }

    /**
     * Builds the complete GraphQL schema.
     */
    private String buildGraphQLSchema(String serviceName, Map<String, GraphQLType> types) {
        StringBuilder schema = new StringBuilder();

        // Schema header
        schema.append("# GraphQL Schema for ").append(serviceName).append("\n");
        schema.append("# Auto-generated from XSD schema\n\n");

        // Custom scalars
        schema.append(generateCustomScalars()).append("\n");

        // Type definitions
        for (GraphQLType type : types.values()) {
            schema.append(generateTypeDefinition(type)).append("\n");
        }

        // Input types (for mutations)
        for (GraphQLType type : types.values()) {
            schema.append(generateInputType(type)).append("\n");
        }

        // Query root type
        schema.append(generateQueryType(serviceName, types)).append("\n");

        // Mutation root type
        schema.append(generateMutationType(serviceName, types)).append("\n");

        // Subscription root type
        schema.append(generateSubscriptionType(serviceName, types)).append("\n");

        // Common types
        schema.append(generateCommonTypes());

        return schema.toString();
    }

    /**
     * Generates custom scalar definitions.
     */
    private String generateCustomScalars() {
        return """
                # Custom Scalars
                scalar DateTime
                scalar JSON
                scalar Long
                """;
    }

    /**
     * Generates a GraphQL type definition.
     */
    private String generateTypeDefinition(GraphQLType type) {
        StringBuilder typeDef = new StringBuilder();

        typeDef.append("# ").append(type.name).append(" type\n");
        typeDef.append("type ").append(type.name).append(" {\n");
        typeDef.append("  id: ID!\n"); // Add ID field for all types

        for (GraphQLField field : type.fields) {
            typeDef.append("  ").append(field.name).append(": ");

            if (field.isList) {
                typeDef.append("[").append(field.type).append("!]");
            } else {
                typeDef.append(field.type);
            }

            if (!field.nullable) {
                typeDef.append("!");
            }

            typeDef.append("\n");
        }

        typeDef.append("}\n");

        return typeDef.toString();
    }

    /**
     * Generates an input type for mutations.
     */
    private String generateInputType(GraphQLType type) {
        StringBuilder inputDef = new StringBuilder();

        inputDef.append("# ").append(type.name).append(" input type\n");
        inputDef.append("input ").append(type.name).append("Input {\n");

        for (GraphQLField field : type.fields) {
            inputDef.append("  ").append(field.name).append(": ");

            if (field.isList) {
                inputDef.append("[").append(field.type).append("!]");
            } else {
                inputDef.append(field.type);
            }

            if (!field.nullable) {
                inputDef.append("!");
            }

            inputDef.append("\n");
        }

        inputDef.append("}\n");

        return inputDef.toString();
    }

    /**
     * Generates Query root type.
     */
    private String generateQueryType(String serviceName, Map<String, GraphQLType> types) {
        StringBuilder query = new StringBuilder();

        query.append("# Query root type\n");
        query.append("type Query {\n");

        for (GraphQLType type : types.values()) {
            String typeName = type.name;
            String typeNameLower = typeMapper.toCamelCase(typeName);

            // Get by ID
            query.append("  ").append(typeNameLower)
                    .append("(id: ID!): ").append(typeName).append("\n");

            // List all
            query.append("  ").append(typeNameLower).append("s")
                    .append("(page: Int, pageSize: Int): [").append(typeName).append("!]!\n");

            // Search/filter
            query.append("  search").append(typeName)
                    .append("(filter: JSON): [").append(typeName).append("!]!\n");
        }

        query.append("}\n");

        return query.toString();
    }

    /**
     * Generates Mutation root type.
     */
    private String generateMutationType(String serviceName, Map<String, GraphQLType> types) {
        StringBuilder mutation = new StringBuilder();

        mutation.append("# Mutation root type\n");
        mutation.append("type Mutation {\n");

        for (GraphQLType type : types.values()) {
            String typeName = type.name;
            String typeNameLower = typeMapper.toCamelCase(typeName);

            // Create
            mutation.append("  create").append(typeName)
                    .append("(input: ").append(typeName).append("Input!): ")
                    .append(typeName).append("!\n");

            // Update
            mutation.append("  update").append(typeName)
                    .append("(id: ID!, input: ").append(typeName).append("Input!): ")
                    .append(typeName).append("!\n");

            // Delete
            mutation.append("  delete").append(typeName)
                    .append("(id: ID!): DeleteResponse!\n");
        }

        mutation.append("}\n");

        return mutation.toString();
    }

    /**
     * Generates Subscription root type.
     */
    private String generateSubscriptionType(String serviceName, Map<String, GraphQLType> types) {
        StringBuilder subscription = new StringBuilder();

        subscription.append("# Subscription root type\n");
        subscription.append("type Subscription {\n");

        for (GraphQLType type : types.values()) {
            String typeName = type.name;
            String typeNameLower = typeMapper.toCamelCase(typeName);

            // Subscribe to entity changes
            subscription.append("  ").append(typeNameLower).append("Updated")
                    .append("(id: ID): ").append(typeName).append("!\n");

            // Subscribe to all changes
            subscription.append("  ").append(typeNameLower).append("Changes")
                    .append(": ChangeEvent!\n");
        }

        subscription.append("}\n");

        return subscription.toString();
    }

    /**
     * Generates common types used across schema.
     */
    private String generateCommonTypes() {
        return """
                # Common types
                type DeleteResponse {
                  success: Boolean!
                  message: String
                }

                type ChangeEvent {
                  eventType: EventType!
                  entityId: ID!
                  entity: JSON!
                  timestamp: DateTime!
                }

                enum EventType {
                  CREATED
                  UPDATED
                  DELETED
                }
                """;
    }

    /**
     * Checks if an element is a global element.
     */
    private boolean isGlobalElement(Element element) {
        Node parent = element.getParentNode();
        return parent != null && "schema".equals(parent.getLocalName());
    }

    /**
     * Inner class representing a GraphQL type.
     */
    private static class GraphQLType {
        String name;
        List<GraphQLField> fields;
    }

    /**
     * Inner class representing a GraphQL field.
     */
    private static class GraphQLField {
        String name;
        String type;
        boolean nullable;
        boolean isList;
    }
}
