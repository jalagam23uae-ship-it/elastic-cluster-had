package com.dynamic.xsd.graphql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps XSD types to GraphQL types.
 * Handles both simple and complex type mappings.
 */
@Slf4j
@Component
public class XsdToGraphQLTypeMapper {

    private static final Map<String, String> SIMPLE_TYPE_MAP = new HashMap<>();

    static {
        // String types -> String
        SIMPLE_TYPE_MAP.put("string", "String");
        SIMPLE_TYPE_MAP.put("normalizedString", "String");
        SIMPLE_TYPE_MAP.put("token", "String");
        SIMPLE_TYPE_MAP.put("language", "String");
        SIMPLE_TYPE_MAP.put("Name", "String");
        SIMPLE_TYPE_MAP.put("NCName", "String");
        SIMPLE_TYPE_MAP.put("ID", "ID");
        SIMPLE_TYPE_MAP.put("IDREF", "ID");
        SIMPLE_TYPE_MAP.put("ENTITY", "String");
        SIMPLE_TYPE_MAP.put("anyURI", "String");
        SIMPLE_TYPE_MAP.put("QName", "String");
        SIMPLE_TYPE_MAP.put("NOTATION", "String");

        // Numeric types
        SIMPLE_TYPE_MAP.put("int", "Int");
        SIMPLE_TYPE_MAP.put("integer", "Long");
        SIMPLE_TYPE_MAP.put("positiveInteger", "Long");
        SIMPLE_TYPE_MAP.put("negativeInteger", "Long");
        SIMPLE_TYPE_MAP.put("nonPositiveInteger", "Long");
        SIMPLE_TYPE_MAP.put("nonNegativeInteger", "Long");
        SIMPLE_TYPE_MAP.put("long", "Long");
        SIMPLE_TYPE_MAP.put("short", "Int");
        SIMPLE_TYPE_MAP.put("byte", "Int");
        SIMPLE_TYPE_MAP.put("unsignedLong", "Long");
        SIMPLE_TYPE_MAP.put("unsignedInt", "Int");
        SIMPLE_TYPE_MAP.put("unsignedShort", "Int");
        SIMPLE_TYPE_MAP.put("unsignedByte", "Int");

        // Floating point types
        SIMPLE_TYPE_MAP.put("float", "Float");
        SIMPLE_TYPE_MAP.put("double", "Float");
        SIMPLE_TYPE_MAP.put("decimal", "Float");

        // Boolean
        SIMPLE_TYPE_MAP.put("boolean", "Boolean");

        // Date/Time types - use custom scalar
        SIMPLE_TYPE_MAP.put("dateTime", "DateTime");
        SIMPLE_TYPE_MAP.put("date", "String");
        SIMPLE_TYPE_MAP.put("time", "String");
        SIMPLE_TYPE_MAP.put("duration", "String");
        SIMPLE_TYPE_MAP.put("gYear", "String");
        SIMPLE_TYPE_MAP.put("gYearMonth", "String");
        SIMPLE_TYPE_MAP.put("gMonth", "String");
        SIMPLE_TYPE_MAP.put("gMonthDay", "String");
        SIMPLE_TYPE_MAP.put("gDay", "String");

        // Binary types - use String for base64 encoding
        SIMPLE_TYPE_MAP.put("base64Binary", "String");
        SIMPLE_TYPE_MAP.put("hexBinary", "String");
    }

    /**
     * Maps XSD type to GraphQL type.
     */
    public String mapType(String xsdType) {
        if (xsdType == null || xsdType.isEmpty()) {
            return "String"; // Default to String
        }

        // Remove namespace prefix if present (e.g., "xs:string" -> "string")
        String cleanType = removeNamespacePrefix(xsdType);

        // Check if it's a simple type
        String graphqlType = SIMPLE_TYPE_MAP.get(cleanType);
        if (graphqlType != null) {
            return graphqlType;
        }

        // If not a simple type, assume it's a complex type
        // Convert to PascalCase for GraphQL type names
        return toPascalCase(cleanType);
    }

    /**
     * Removes namespace prefix from XSD type.
     */
    private String removeNamespacePrefix(String xsdType) {
        int colonIndex = xsdType.indexOf(':');
        if (colonIndex > 0) {
            return xsdType.substring(colonIndex + 1);
        }
        return xsdType;
    }

    /**
     * Converts a name to PascalCase for GraphQL type names.
     */
    public String toPascalCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Remove any non-alphanumeric characters and split by them
        String[] parts = name.split("[^a-zA-Z0-9]+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                result.append(part.substring(1).toLowerCase());
            }
        }

        String pascalCase = result.toString();

        // Ensure first character is uppercase
        if (!pascalCase.isEmpty() && !Character.isUpperCase(pascalCase.charAt(0))) {
            pascalCase = Character.toUpperCase(pascalCase.charAt(0)) + pascalCase.substring(1);
        }

        return pascalCase;
    }

    /**
     * Converts a name to camelCase for GraphQL field names.
     */
    public String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // First convert to PascalCase
        String pascalCase = toPascalCase(name);

        // Then lowercase the first character
        if (!pascalCase.isEmpty()) {
            return Character.toLowerCase(pascalCase.charAt(0)) + pascalCase.substring(1);
        }

        return pascalCase;
    }

    /**
     * Gets the default value for a GraphQL type.
     */
    public String getDefaultValue(String graphqlType) {
        switch (graphqlType) {
            case "Boolean":
                return "false";
            case "Int":
            case "Long":
                return "0";
            case "Float":
                return "0.0";
            case "String":
            case "ID":
                return "\"\"";
            default:
                return "null";
        }
    }

    /**
     * Checks if a field should be a list based on maxOccurs.
     */
    public boolean shouldBeList(int maxOccurs) {
        return maxOccurs > 1 || maxOccurs == -1; // -1 means unbounded
    }

    /**
     * Determines if a field should be nullable.
     */
    public boolean shouldBeNullable(int minOccurs) {
        return minOccurs == 0;
    }

    /**
     * Gets the GraphQL SDL syntax version.
     */
    public String getGraphQLVersion() {
        return "SDL 2023";
    }

    /**
     * Checks if a GraphQL type is a custom scalar.
     */
    public boolean isCustomScalar(String graphqlType) {
        return graphqlType.equals("DateTime") ||
               graphqlType.equals("JSON") ||
               graphqlType.equals("Long");
    }
}
