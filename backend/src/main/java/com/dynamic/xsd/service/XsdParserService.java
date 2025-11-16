package com.dynamic.xsd.service;

import com.dynamic.xsd.model.XsdAnalysisResult;
import com.dynamic.xsd.model.XsdFieldInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;

/**
 * Service for analyzing XSD schemas and extracting field information.
 * Provides detailed field metadata including validation rules and constraints.
 */
@Slf4j
@Service
public class XsdParserService {

    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    /**
     * Analyzes an XSD schema and extracts all field information.
     *
     * @param xsdContent The XSD schema content as string
     * @return XsdAnalysisResult containing all extracted field information
     */
    public XsdAnalysisResult analyzeXsd(String xsdContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xsdContent)));

            Element root = doc.getDocumentElement();
            String targetNamespace = root.getAttribute("targetNamespace");

            List<XsdFieldInfo> fields = new ArrayList<>();
            String rootElementName = null;

            // Find root element
            NodeList elements = root.getElementsByTagNameNS(XSD_NAMESPACE, "element");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                if (element.getParentNode().equals(root)) {
                    rootElementName = element.getAttribute("name");
                    // Parse this element's structure
                    parseElement(element, fields, doc);
                    break;
                }
            }

            return XsdAnalysisResult.builder()
                    .targetNamespace(targetNamespace)
                    .rootElement(rootElementName)
                    .fields(fields)
                    .totalFields(fields.size())
                    .success(true)
                    .message("XSD analyzed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing XSD: ", e);
            return XsdAnalysisResult.builder()
                    .success(false)
                    .message("Error analyzing XSD: " + e.getMessage())
                    .totalFields(0)
                    .build();
        }
    }

    private void parseElement(Element element, List<XsdFieldInfo> fields, Document doc) {
        String elementType = element.getAttribute("type");

        // Check for complex type definition
        NodeList complexTypes = element.getElementsByTagNameNS(XSD_NAMESPACE, "complexType");
        if (complexTypes.getLength() > 0) {
            Element complexType = (Element) complexTypes.item(0);
            parseComplexType(complexType, fields, doc, "");
        } else if (!elementType.isEmpty()) {
            // Check if the type is defined elsewhere in the schema
            Element typeDefinition = findTypeDefinition(elementType, doc);
            if (typeDefinition != null) {
                parseComplexType(typeDefinition, fields, doc, "");
            }
        }
    }

    private void parseComplexType(Element complexType, List<XsdFieldInfo> fields, Document doc, String prefix) {
        // Handle sequence
        NodeList sequences = complexType.getElementsByTagNameNS(XSD_NAMESPACE, "sequence");
        if (sequences.getLength() > 0) {
            Element sequence = (Element) sequences.item(0);
            if (isDirectChild(sequence, complexType)) {
                parseSequence(sequence, fields, doc, prefix);
            }
        }

        // Handle all
        NodeList alls = complexType.getElementsByTagNameNS(XSD_NAMESPACE, "all");
        if (alls.getLength() > 0) {
            Element all = (Element) alls.item(0);
            if (isDirectChild(all, complexType)) {
                parseSequence(all, fields, doc, prefix);
            }
        }

        // Handle choice
        NodeList choices = complexType.getElementsByTagNameNS(XSD_NAMESPACE, "choice");
        if (choices.getLength() > 0) {
            Element choice = (Element) choices.item(0);
            if (isDirectChild(choice, complexType)) {
                parseSequence(choice, fields, doc, prefix);
            }
        }
    }

    private void parseSequence(Element sequence, List<XsdFieldInfo> fields, Document doc, String prefix) {
        NodeList elements = sequence.getElementsByTagNameNS(XSD_NAMESPACE, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element elem = (Element) elements.item(i);
            // Only process direct children
            if (elem.getParentNode().equals(sequence)) {
                extractFieldInfo(elem, doc, fields, prefix);
            }
        }
    }

    private void extractFieldInfo(Element element, Document doc, List<XsdFieldInfo> fields, String prefix) {
        String name = element.getAttribute("name");
        String type = element.getAttribute("type");
        String minOccurs = element.getAttribute("minOccurs");
        String maxOccurs = element.getAttribute("maxOccurs");

        if (minOccurs.isEmpty()) {
            minOccurs = "1"; // default
        }
        if (maxOccurs.isEmpty()) {
            maxOccurs = "1"; // default
        }

        boolean required = !minOccurs.equals("0");

        // Full field name with path
        String fullName = prefix.isEmpty() ? name : prefix + "." + name;

        // Check for inline complex type
        NodeList inlineComplexTypes = element.getElementsByTagNameNS(XSD_NAMESPACE, "complexType");
        if (inlineComplexTypes.getLength() > 0) {
            for (int i = 0; i < inlineComplexTypes.getLength(); i++) {
                Element ct = (Element) inlineComplexTypes.item(i);
                if (ct.getParentNode().equals(element)) {
                    // Add parent field info first
                    fields.add(createComplexTypeFieldInfo(fullName, type, minOccurs, maxOccurs, required));
                    // Recursively parse nested complex type
                    parseComplexType(ct, fields, doc, fullName);
                    return;
                }
            }
        }

        // Check if type is a named complex type
        if (!type.isEmpty()) {
            Element typeDefinition = findTypeDefinition(type, doc);
            if (typeDefinition != null && typeDefinition.getLocalName().equals("complexType")) {
                // Add parent field info first
                fields.add(createComplexTypeFieldInfo(fullName, type, minOccurs, maxOccurs, required));
                // Recursively parse nested complex type
                parseComplexType(typeDefinition, fields, doc, fullName);
                return;
            }
        }

        // If not a complex type, extract as a simple field
        Map<String, Object> validations = new HashMap<>();
        List<String> enumValues = new ArrayList<>();
        String pattern = null;
        String minLength = null;
        String maxLength = null;
        String minInclusive = null;
        String maxInclusive = null;
        String documentation = null;

        // Check for inline simple type with restrictions
        NodeList simpleTypes = element.getElementsByTagNameNS(XSD_NAMESPACE, "simpleType");
        if (simpleTypes.getLength() > 0) {
            Element simpleType = (Element) simpleTypes.item(0);
            Map<String, Object> restrictions = parseRestrictions(simpleType);

            if (restrictions.containsKey("enumValues")) {
                @SuppressWarnings("unchecked")
                List<String> enums = (List<String>) restrictions.get("enumValues");
                enumValues = enums;
            }
            if (restrictions.containsKey("pattern")) {
                pattern = (String) restrictions.get("pattern");
            }
            if (restrictions.containsKey("minLength")) {
                minLength = (String) restrictions.get("minLength");
            }
            if (restrictions.containsKey("maxLength")) {
                maxLength = (String) restrictions.get("maxLength");
            }
            if (restrictions.containsKey("minInclusive")) {
                minInclusive = (String) restrictions.get("minInclusive");
            }
            if (restrictions.containsKey("maxInclusive")) {
                maxInclusive = (String) restrictions.get("maxInclusive");
            }
            validations.putAll(restrictions);
        } else if (!type.isEmpty()) {
            // Check if type is defined elsewhere
            Element typeDefinition = findTypeDefinition(type, doc);
            if (typeDefinition != null && typeDefinition.getLocalName().equals("simpleType")) {
                Map<String, Object> restrictions = parseRestrictions(typeDefinition);

                if (restrictions.containsKey("enumValues")) {
                    @SuppressWarnings("unchecked")
                    List<String> enums = (List<String>) restrictions.get("enumValues");
                    enumValues = enums;
                }
                if (restrictions.containsKey("pattern")) {
                    pattern = (String) restrictions.get("pattern");
                }
                if (restrictions.containsKey("minLength")) {
                    minLength = (String) restrictions.get("minLength");
                }
                if (restrictions.containsKey("maxLength")) {
                    maxLength = (String) restrictions.get("maxLength");
                }
                if (restrictions.containsKey("minInclusive")) {
                    minInclusive = (String) restrictions.get("minInclusive");
                }
                if (restrictions.containsKey("maxInclusive")) {
                    maxInclusive = (String) restrictions.get("maxInclusive");
                }
                validations.putAll(restrictions);
            }
        }

        // Extract documentation
        NodeList annotations = element.getElementsByTagNameNS(XSD_NAMESPACE, "annotation");
        if (annotations.getLength() > 0) {
            Element annotation = (Element) annotations.item(0);
            NodeList docs = annotation.getElementsByTagNameNS(XSD_NAMESPACE, "documentation");
            if (docs.getLength() > 0) {
                documentation = docs.item(0).getTextContent().trim();
            }
        }

        // Add the field info to the list
        fields.add(XsdFieldInfo.builder()
                .name(fullName)
                .type(type.isEmpty() ? "string" : type)
                .minOccurs(minOccurs)
                .maxOccurs(maxOccurs)
                .required(required)
                .validations(validations)
                .enumValues(enumValues)
                .pattern(pattern)
                .minLength(minLength)
                .maxLength(maxLength)
                .minInclusive(minInclusive)
                .maxInclusive(maxInclusive)
                .documentation(documentation)
                .build());
    }

    private XsdFieldInfo createComplexTypeFieldInfo(String name, String type, String minOccurs, String maxOccurs, boolean required) {
        return XsdFieldInfo.builder()
                .name(name)
                .type(type.isEmpty() ? "ComplexType" : type)
                .minOccurs(minOccurs)
                .maxOccurs(maxOccurs)
                .required(required)
                .validations(new HashMap<>())
                .enumValues(new ArrayList<>())
                .documentation("Complex type containing nested fields")
                .build();
    }

    private Map<String, Object> parseRestrictions(Element simpleType) {
        Map<String, Object> restrictions = new HashMap<>();
        List<String> enumValues = new ArrayList<>();

        NodeList restrictionNodes = simpleType.getElementsByTagNameNS(XSD_NAMESPACE, "restriction");
        if (restrictionNodes.getLength() > 0) {
            Element restriction = (Element) restrictionNodes.item(0);
            String base = restriction.getAttribute("base");
            restrictions.put("base", base);

            // Parse enumeration values
            NodeList enumerations = restriction.getElementsByTagNameNS(XSD_NAMESPACE, "enumeration");
            for (int i = 0; i < enumerations.getLength(); i++) {
                Element enumeration = (Element) enumerations.item(i);
                enumValues.add(enumeration.getAttribute("value"));
            }
            if (!enumValues.isEmpty()) {
                restrictions.put("enumValues", enumValues);
            }

            // Parse pattern
            NodeList patterns = restriction.getElementsByTagNameNS(XSD_NAMESPACE, "pattern");
            if (patterns.getLength() > 0) {
                Element pattern = (Element) patterns.item(0);
                restrictions.put("pattern", pattern.getAttribute("value"));
            }

            // Parse length restrictions
            NodeList minLengths = restriction.getElementsByTagNameNS(XSD_NAMESPACE, "minLength");
            if (minLengths.getLength() > 0) {
                Element minLength = (Element) minLengths.item(0);
                restrictions.put("minLength", minLength.getAttribute("value"));
            }

            NodeList maxLengths = restriction.getElementsByTagNameNS(XSD_NAMESPACE, "maxLength");
            if (maxLengths.getLength() > 0) {
                Element maxLength = (Element) maxLengths.item(0);
                restrictions.put("maxLength", maxLength.getAttribute("value"));
            }

            // Parse numeric restrictions
            NodeList minInclusives = restriction.getElementsByTagNameNS(XSD_NAMESPACE, "minInclusive");
            if (minInclusives.getLength() > 0) {
                Element minInclusive = (Element) minInclusives.item(0);
                restrictions.put("minInclusive", minInclusive.getAttribute("value"));
            }

            NodeList maxInclusives = restriction.getElementsByTagNameNS(XSD_NAMESPACE, "maxInclusive");
            if (maxInclusives.getLength() > 0) {
                Element maxInclusive = (Element) maxInclusives.item(0);
                restrictions.put("maxInclusive", maxInclusive.getAttribute("value"));
            }
        }

        return restrictions;
    }

    private Element findTypeDefinition(String typeName, Document doc) {
        // Remove namespace prefix if present
        String localTypeName = typeName.contains(":") ? typeName.split(":")[1] : typeName;

        // Search for simpleType
        NodeList simpleTypes = doc.getElementsByTagNameNS(XSD_NAMESPACE, "simpleType");
        for (int i = 0; i < simpleTypes.getLength(); i++) {
            Element simpleType = (Element) simpleTypes.item(i);
            if (localTypeName.equals(simpleType.getAttribute("name"))) {
                return simpleType;
            }
        }

        // Search for complexType
        NodeList complexTypes = doc.getElementsByTagNameNS(XSD_NAMESPACE, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element complexType = (Element) complexTypes.item(i);
            if (localTypeName.equals(complexType.getAttribute("name"))) {
                return complexType;
            }
        }

        return null;
    }

    private boolean isDirectChild(Element child, Element parent) {
        Node childParent = child.getParentNode();
        return childParent != null && childParent.equals(parent);
    }
}
