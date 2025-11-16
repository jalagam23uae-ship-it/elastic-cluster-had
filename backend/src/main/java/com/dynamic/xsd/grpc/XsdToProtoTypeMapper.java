package com.dynamic.xsd.grpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps XSD types to Protocol Buffer types.
 * Handles both simple and complex type mappings.
 */
@Slf4j
@Component
public class XsdToProtoTypeMapper {

    private static final Map<String, String> SIMPLE_TYPE_MAP = new HashMap<>();

    static {
        // String types
        SIMPLE_TYPE_MAP.put("string", "string");
        SIMPLE_TYPE_MAP.put("normalizedString", "string");
        SIMPLE_TYPE_MAP.put("token", "string");
        SIMPLE_TYPE_MAP.put("language", "string");
        SIMPLE_TYPE_MAP.put("Name", "string");
        SIMPLE_TYPE_MAP.put("NCName", "string");
        SIMPLE_TYPE_MAP.put("ID", "string");
        SIMPLE_TYPE_MAP.put("IDREF", "string");
        SIMPLE_TYPE_MAP.put("ENTITY", "string");
        SIMPLE_TYPE_MAP.put("anyURI", "string");
        SIMPLE_TYPE_MAP.put("QName", "string");
        SIMPLE_TYPE_MAP.put("NOTATION", "string");

        // Numeric types
        SIMPLE_TYPE_MAP.put("int", "int32");
        SIMPLE_TYPE_MAP.put("integer", "int64");
        SIMPLE_TYPE_MAP.put("positiveInteger", "int64");
        SIMPLE_TYPE_MAP.put("negativeInteger", "int64");
        SIMPLE_TYPE_MAP.put("nonPositiveInteger", "int64");
        SIMPLE_TYPE_MAP.put("nonNegativeInteger", "int64");
        SIMPLE_TYPE_MAP.put("long", "int64");
        SIMPLE_TYPE_MAP.put("short", "int32");
        SIMPLE_TYPE_MAP.put("byte", "int32");
        SIMPLE_TYPE_MAP.put("unsignedLong", "uint64");
        SIMPLE_TYPE_MAP.put("unsignedInt", "uint32");
        SIMPLE_TYPE_MAP.put("unsignedShort", "uint32");
        SIMPLE_TYPE_MAP.put("unsignedByte", "uint32");

        // Floating point types
        SIMPLE_TYPE_MAP.put("float", "float");
        SIMPLE_TYPE_MAP.put("double", "double");
        SIMPLE_TYPE_MAP.put("decimal", "string"); // Use string to preserve precision

        // Boolean
        SIMPLE_TYPE_MAP.put("boolean", "bool");

        // Date/Time types - use string or google.protobuf.Timestamp
        SIMPLE_TYPE_MAP.put("dateTime", "google.protobuf.Timestamp");
        SIMPLE_TYPE_MAP.put("date", "string");
        SIMPLE_TYPE_MAP.put("time", "string");
        SIMPLE_TYPE_MAP.put("duration", "google.protobuf.Duration");
        SIMPLE_TYPE_MAP.put("gYear", "string");
        SIMPLE_TYPE_MAP.put("gYearMonth", "string");
        SIMPLE_TYPE_MAP.put("gMonth", "string");
        SIMPLE_TYPE_MAP.put("gMonthDay", "string");
        SIMPLE_TYPE_MAP.put("gDay", "string");

        // Binary types
        SIMPLE_TYPE_MAP.put("base64Binary", "bytes");
        SIMPLE_TYPE_MAP.put("hexBinary", "bytes");
    }

    /**
     * Maps XSD type to Proto type.
     */
    public String mapType(String xsdType) {
        if (xsdType == null || xsdType.isEmpty()) {
            return "string"; // Default to string
        }

        // Remove namespace prefix if present (e.g., "xs:string" -> "string")
        String cleanType = removeNamespacePrefix(xsdType);

        // Check if it's a simple type
        String protoType = SIMPLE_TYPE_MAP.get(cleanType);
        if (protoType != null) {
            return protoType;
        }

        // If not a simple type, assume it's a complex type (message)
        // Convert to PascalCase for Proto message names
        return toPascalCase(cleanType);
    }

    /**
     * Checks if the type is a well-known Proto type.
     */
    public boolean isWellKnownType(String protoType) {
        return protoType.startsWith("google.protobuf.");
    }

    /**
     * Gets the import statement for a well-known type.
     */
    public String getWellKnownTypeImport(String protoType) {
        if (protoType.equals("google.protobuf.Timestamp")) {
            return "import \"google/protobuf/timestamp.proto\";";
        } else if (protoType.equals("google.protobuf.Duration")) {
            return "import \"google/protobuf/duration.proto\";";
        } else if (protoType.equals("google.protobuf.Any")) {
            return "import \"google/protobuf/any.proto\";";
        } else if (protoType.equals("google.protobuf.Empty")) {
            return "import \"google/protobuf/empty.proto\";";
        }
        return "";
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
     * Converts a name to PascalCase for Proto message names.
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
     * Converts a name to snake_case for Proto field names.
     */
    public String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Convert camelCase or PascalCase to snake_case
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && Character.isLowerCase(name.charAt(i - 1))) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString().replaceAll("[^a-z0-9_]", "_");
    }

    /**
     * Gets the default value for a Proto type.
     */
    public String getDefaultValue(String protoType) {
        switch (protoType) {
            case "bool":
                return "false";
            case "int32":
            case "int64":
            case "uint32":
            case "uint64":
            case "sint32":
            case "sint64":
            case "fixed32":
            case "fixed64":
            case "sfixed32":
            case "sfixed64":
                return "0";
            case "float":
            case "double":
                return "0.0";
            case "string":
                return "\"\"";
            case "bytes":
                return "[]";
            default:
                return "null";
        }
    }

    /**
     * Checks if a type should be repeated (array).
     */
    public boolean shouldBeRepeated(String xsdType, int maxOccurs) {
        return maxOccurs > 1 || maxOccurs == -1; // -1 means unbounded
    }

    /**
     * Determines if a field should be optional.
     */
    public boolean shouldBeOptional(int minOccurs) {
        return minOccurs == 0;
    }

    /**
     * Gets the Proto syntax version.
     */
    public String getProtoSyntax() {
        return "proto3";
    }
}
