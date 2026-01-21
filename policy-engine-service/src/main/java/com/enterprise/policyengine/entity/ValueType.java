package com.enterprise.policyengine.entity;

/**
 * Value types for policy rule comparisons.
 */
public enum ValueType {
    STRING, // Static string value
    NUMBER, // Static number value
    BOOLEAN, // Static boolean value
    ARRAY, // Static array value (JSON)
    EXPRESSION // Dynamic expression like "subject.approvalLimit"
}
