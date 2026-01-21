package com.enterprise.form.service;

import com.enterprise.form.dto.ValidationResultDTO;
import com.enterprise.form.entity.FieldDefinition;
import com.enterprise.form.entity.FormDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for validating form data against schema and rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    /**
     * Validate form data against the form definition.
     */
    public ValidationResultDTO validate(FormDefinition form, Map<String, Object> data) {
        List<ValidationResultDTO.ValidationError> errors = new ArrayList<>();

        // Validate each field
        for (FieldDefinition field : form.getFields()) {
            Object value = data.get(field.getFieldKey());

            // Check required fields
            if (Boolean.TRUE.equals(field.getRequired())) {
                if (value == null || (value instanceof String && ((String) value).isBlank())) {
                    errors.add(ValidationResultDTO.ValidationError.builder()
                            .fieldKey(field.getFieldKey())
                            .message(field.getLabel() + " is required")
                            .rule("required")
                            .value(value)
                            .build());
                    continue;
                }
            }

            // Skip further validation if value is null/empty for non-required fields
            if (value == null || (value instanceof String && ((String) value).isBlank())) {
                continue;
            }

            // Apply validation rules
            if (field.getValidationRules() != null) {
                validateFieldRules(field, value, errors);
            }
        }

        return ValidationResultDTO.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private void validateFieldRules(FieldDefinition field, Object value,
            List<ValidationResultDTO.ValidationError> errors) {
        Map<String, Object> rules = field.getValidationRules();

        // Min length
        if (rules.containsKey("minLength") && value instanceof String) {
            int minLength = ((Number) rules.get("minLength")).intValue();
            if (((String) value).length() < minLength) {
                errors.add(ValidationResultDTO.ValidationError.builder()
                        .fieldKey(field.getFieldKey())
                        .message(field.getLabel() + " must be at least " + minLength + " characters")
                        .rule("minLength")
                        .value(value)
                        .build());
            }
        }

        // Max length
        if (rules.containsKey("maxLength") && value instanceof String) {
            int maxLength = ((Number) rules.get("maxLength")).intValue();
            if (((String) value).length() > maxLength) {
                errors.add(ValidationResultDTO.ValidationError.builder()
                        .fieldKey(field.getFieldKey())
                        .message(field.getLabel() + " must not exceed " + maxLength + " characters")
                        .rule("maxLength")
                        .value(value)
                        .build());
            }
        }

        // Min value (for numbers)
        if (rules.containsKey("min") && value instanceof Number) {
            double min = ((Number) rules.get("min")).doubleValue();
            if (((Number) value).doubleValue() < min) {
                errors.add(ValidationResultDTO.ValidationError.builder()
                        .fieldKey(field.getFieldKey())
                        .message(field.getLabel() + " must be at least " + min)
                        .rule("min")
                        .value(value)
                        .build());
            }
        }

        // Max value (for numbers)
        if (rules.containsKey("max") && value instanceof Number) {
            double max = ((Number) rules.get("max")).doubleValue();
            if (((Number) value).doubleValue() > max) {
                errors.add(ValidationResultDTO.ValidationError.builder()
                        .fieldKey(field.getFieldKey())
                        .message(field.getLabel() + " must not exceed " + max)
                        .rule("max")
                        .value(value)
                        .build());
            }
        }

        // Pattern (regex)
        if (rules.containsKey("pattern") && value instanceof String) {
            String pattern = (String) rules.get("pattern");
            if (!((String) value).matches(pattern)) {
                String message = rules.containsKey("patternMessage")
                        ? (String) rules.get("patternMessage")
                        : field.getLabel() + " has invalid format";
                errors.add(ValidationResultDTO.ValidationError.builder()
                        .fieldKey(field.getFieldKey())
                        .message(message)
                        .rule("pattern")
                        .value(value)
                        .build());
            }
        }

        // Email validation
        if (rules.containsKey("email") && Boolean.TRUE.equals(rules.get("email")) && value instanceof String) {
            String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
            if (!((String) value).matches(emailPattern)) {
                errors.add(ValidationResultDTO.ValidationError.builder()
                        .fieldKey(field.getFieldKey())
                        .message(field.getLabel() + " must be a valid email address")
                        .rule("email")
                        .value(value)
                        .build());
            }
        }
    }

    /**
     * Validate data against JSON Schema (more comprehensive validation).
     */
    public ValidationResultDTO validateAgainstSchema(Map<String, Object> schema, Map<String, Object> data) {
        // For comprehensive JSON Schema validation, we can use networknt
        // json-schema-validator
        // For now, returning a simple pass
        return ValidationResultDTO.builder()
                .valid(true)
                .errors(List.of())
                .build();
    }
}
