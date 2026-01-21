package com.enterprise.policyengine.entity;

/**
 * Comparison operators for policy rules.
 */
public enum Operator {
    // Equality
    EQUALS,
    NOT_EQUALS,

    // Collection
    IN,
    NOT_IN,
    CONTAINS,
    CONTAINS_ANY,

    // Comparison (for numbers)
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,

    // String
    STARTS_WITH,
    ENDS_WITH,
    MATCHES_REGEX,

    // Null checks
    IS_NULL,
    IS_NOT_NULL,

    // Boolean
    IS_TRUE,
    IS_FALSE
}
