package com.dynamic.xsd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Model representing detailed field information extracted from XSD schema.
 * Contains field metadata including validation rules, constraints, and documentation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XsdFieldInfo {
    private String name;
    private String type;
    private String minOccurs;
    private String maxOccurs;
    private boolean required;
    private Map<String, Object> validations;
    private List<String> enumValues;
    private String pattern;
    private String minLength;
    private String maxLength;
    private String minInclusive;
    private String maxInclusive;
    private String documentation;
}
