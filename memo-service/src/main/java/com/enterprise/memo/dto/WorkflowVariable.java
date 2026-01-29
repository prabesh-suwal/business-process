package com.enterprise.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a variable available for workflow conditions.
 * Variables can come from memo fields, form fields, or user context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowVariable {

    private String name; // e.g., "form.amount", "memo.priority", "initiator.role"
    private String label; // e.g., "Loan Amount", "Priority", "Initiator Role"
    private String type; // "number", "text", "enum", "date", "boolean"
    private String source; // "memo", "form", "initiator"
    private List<String> options; // For enum types, the available options

    /**
     * Get available operators for this variable type.
     */
    public List<String> getAvailableOperators() {
        return switch (type) {
            case "number" -> List.of("EQUALS", "NOT_EQUALS", "GREATER_THAN", "GREATER_THAN_OR_EQUALS",
                    "LESS_THAN", "LESS_THAN_OR_EQUALS");
            case "text" -> List.of("EQUALS", "NOT_EQUALS", "CONTAINS", "STARTS_WITH", "ENDS_WITH");
            case "enum" -> List.of("EQUALS", "NOT_EQUALS", "IN");
            case "boolean" -> List.of("EQUALS");
            case "date" -> List.of("EQUALS", "BEFORE", "AFTER");
            default -> List.of("EQUALS", "NOT_EQUALS");
        };
    }
}
