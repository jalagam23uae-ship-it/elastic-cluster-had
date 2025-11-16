package com.dynamic.xsd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model representing the complete analysis result of an XSD schema.
 * Contains schema metadata, extracted fields, and generated sample XML.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XsdAnalysisResult {
    private String targetNamespace;
    private String rootElement;
    private List<XsdFieldInfo> fields;
    private String sampleXml;
    private boolean success;
    private String message;
    private Integer totalFields;
}
