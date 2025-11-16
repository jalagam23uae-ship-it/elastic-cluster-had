package com.dynamic.xsd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Generates WSDL documents from XSD schemas and POJO classes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsdlGenerator {

    private static final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    /**
     * Generates a WSDL document for a service.
     */
    public String generateWsdl(String serviceName, String targetNamespace,
                               Set<Class<?>> rootClasses, String xsdContent) {
        try {
            log.info("Generating WSDL for service: {}", serviceName);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            // Create definitions root element
            Element definitions = createDefinitions(doc, serviceName, targetNamespace);
            doc.appendChild(definitions);

            // Add types section with embedded XSD
            Element types = createTypes(doc, xsdContent.getBytes(), targetNamespace);
            definitions.appendChild(types);

            // Generate messages, portType, binding, and service for each root class
            for (Class<?> rootClass : rootClasses) {
                String operationName = getOperationName(rootClass);
                String messageName = rootClass.getSimpleName();

                // Create messages
                Element inputMessage = createMessage(doc, messageName + "Request", messageName, targetNamespace);
                Element outputMessage = createMessage(doc, messageName + "Response", messageName, targetNamespace);
                definitions.appendChild(inputMessage);
                definitions.appendChild(outputMessage);

                // Create portType operation
                Element portType = getOrCreatePortType(doc, definitions, serviceName + "PortType");
                Element operation = createOperation(doc, operationName, messageName);
                portType.appendChild(operation);

                // Create binding operation
                Element binding = getOrCreateBinding(doc, definitions, serviceName + "Binding",
                    serviceName + "PortType", targetNamespace);
                Element bindingOperation = createBindingOperation(doc, operationName);
                binding.appendChild(bindingOperation);
            }

            // Create service element
            Element service = createService(doc, serviceName, serviceName + "Binding", targetNamespace);
            definitions.appendChild(service);

            // Convert to string
            return documentToString(doc);

        } catch (Exception e) {
            log.error("Failed to generate WSDL for service: {}", serviceName, e);
            throw new RuntimeException("WSDL generation failed", e);
        }
    }

    /**
     * Creates the definitions root element.
     */
    private Element createDefinitions(Document doc, String serviceName, String targetNamespace) {
        Element definitions = doc.createElementNS(WSDL_NS, "wsdl:definitions");
        definitions.setAttribute("name", serviceName);
        definitions.setAttribute("targetNamespace", targetNamespace);
        definitions.setAttribute("xmlns:tns", targetNamespace);
        definitions.setAttribute("xmlns:wsdl", WSDL_NS);
        definitions.setAttribute("xmlns:soap", SOAP_NS);
        definitions.setAttribute("xmlns:xsd", XSD_NS);
        return definitions;
    }

    /**
     * Creates the types section with embedded XSD.
     */
    private Element createTypes(Document doc, byte[] xsdContent, String targetNamespace) throws Exception {
        Element types = doc.createElementNS(WSDL_NS, "wsdl:types");

        // Parse and embed the XSD content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document xsdDoc = builder.parse(new java.io.ByteArrayInputStream(xsdContent));
        Element schemaElement = xsdDoc.getDocumentElement();

        // Import the schema element into the WSDL document
        Element importedSchema = (Element) doc.importNode(schemaElement, true);
        types.appendChild(importedSchema);

        return types;
    }

    /**
     * Creates a message element.
     */
    private Element createMessage(Document doc, String messageName, String elementName, String targetNamespace) {
        Element message = doc.createElementNS(WSDL_NS, "wsdl:message");
        message.setAttribute("name", messageName);

        Element part = doc.createElementNS(WSDL_NS, "wsdl:part");
        part.setAttribute("name", "parameters");
        part.setAttribute("element", "tns:" + elementName);

        message.appendChild(part);
        return message;
    }

    /**
     * Creates or gets existing portType element.
     */
    private Element getOrCreatePortType(Document doc, Element definitions, String portTypeName) {
        // Check if portType already exists
        org.w3c.dom.NodeList portTypes = definitions.getElementsByTagNameNS(WSDL_NS, "portType");
        for (int i = 0; i < portTypes.getLength(); i++) {
            Element pt = (Element) portTypes.item(i);
            if (pt.getAttribute("name").equals(portTypeName)) {
                return pt;
            }
        }

        // Create new portType
        Element portType = doc.createElementNS(WSDL_NS, "wsdl:portType");
        portType.setAttribute("name", portTypeName);
        definitions.appendChild(portType);
        return portType;
    }

    /**
     * Creates an operation element for portType.
     */
    private Element createOperation(Document doc, String operationName, String messageName) {
        Element operation = doc.createElementNS(WSDL_NS, "wsdl:operation");
        operation.setAttribute("name", operationName);

        Element input = doc.createElementNS(WSDL_NS, "wsdl:input");
        input.setAttribute("message", "tns:" + messageName + "Request");
        operation.appendChild(input);

        Element output = doc.createElementNS(WSDL_NS, "wsdl:output");
        output.setAttribute("message", "tns:" + messageName + "Response");
        operation.appendChild(output);

        return operation;
    }

    /**
     * Creates or gets existing binding element.
     */
    private Element getOrCreateBinding(Document doc, Element definitions, String bindingName,
                                       String portTypeName, String targetNamespace) {
        // Check if binding already exists
        org.w3c.dom.NodeList bindings = definitions.getElementsByTagNameNS(WSDL_NS, "binding");
        for (int i = 0; i < bindings.getLength(); i++) {
            Element b = (Element) bindings.item(i);
            if (b.getAttribute("name").equals(bindingName)) {
                return b;
            }
        }

        // Create new binding
        Element binding = doc.createElementNS(WSDL_NS, "wsdl:binding");
        binding.setAttribute("name", bindingName);
        binding.setAttribute("type", "tns:" + portTypeName);

        // Add SOAP binding
        Element soapBinding = doc.createElementNS(SOAP_NS, "soap:binding");
        soapBinding.setAttribute("style", "document");
        soapBinding.setAttribute("transport", "http://schemas.xmlsoap.org/soap/http");
        binding.appendChild(soapBinding);

        definitions.appendChild(binding);
        return binding;
    }

    /**
     * Creates a binding operation element.
     */
    private Element createBindingOperation(Document doc, String operationName) {
        Element operation = doc.createElementNS(WSDL_NS, "wsdl:operation");
        operation.setAttribute("name", operationName);

        // SOAP operation
        Element soapOperation = doc.createElementNS(SOAP_NS, "soap:operation");
        soapOperation.setAttribute("soapAction", operationName);
        operation.appendChild(soapOperation);

        // Input
        Element input = doc.createElementNS(WSDL_NS, "wsdl:input");
        Element soapBodyInput = doc.createElementNS(SOAP_NS, "soap:body");
        soapBodyInput.setAttribute("use", "literal");
        input.appendChild(soapBodyInput);
        operation.appendChild(input);

        // Output
        Element output = doc.createElementNS(WSDL_NS, "wsdl:output");
        Element soapBodyOutput = doc.createElementNS(SOAP_NS, "soap:body");
        soapBodyOutput.setAttribute("use", "literal");
        output.appendChild(soapBodyOutput);
        operation.appendChild(output);

        return operation;
    }

    /**
     * Creates the service element.
     */
    private Element createService(Document doc, String serviceName, String bindingName, String targetNamespace) {
        Element service = doc.createElementNS(WSDL_NS, "wsdl:service");
        service.setAttribute("name", serviceName + "Service");

        Element port = doc.createElementNS(WSDL_NS, "wsdl:port");
        port.setAttribute("name", serviceName + "Port");
        port.setAttribute("binding", "tns:" + bindingName);

        Element soapAddress = doc.createElementNS(SOAP_NS, "soap:address");
        soapAddress.setAttribute("location", "http://localhost:8080/ws/" + serviceName);
        port.appendChild(soapAddress);

        service.appendChild(port);
        return service;
    }

    /**
     * Gets operation name from root class.
     */
    private String getOperationName(Class<?> rootClass) {
        // Use class name as operation name
        String className = rootClass.getSimpleName();

        // Common patterns: create, get, update, delete
        return "process" + className;
    }

    /**
     * Converts DOM document to string.
     */
    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
