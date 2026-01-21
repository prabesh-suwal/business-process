package com.enterprise.workflow.dto;

import lombok.Data;
import java.util.Map;

/**
 * Represents a resolved assignment from rule evaluation.
 */
@Data
public class ResolvedAssignment {

    public enum AssignmentType {
        ROLE, // Assign to anyone with this role
        USER, // Assign to specific user
        DEPARTMENT, // Assign to users in department
        BRANCH, // Assign to users in branch
        POLICY // Use policy engine to determine
    }

    private AssignmentType type;
    private String value; // Role code, user ID, department ID, etc.
    private Map<String, Object> scope; // Additional scoping (branch, department filters)
    private String matchedCondition; // Which condition was matched (for debugging)

    public static ResolvedAssignment forRole(String roleCode) {
        ResolvedAssignment assignment = new ResolvedAssignment();
        assignment.setType(AssignmentType.ROLE);
        assignment.setValue(roleCode);
        return assignment;
    }

    public static ResolvedAssignment forUser(String userId) {
        ResolvedAssignment assignment = new ResolvedAssignment();
        assignment.setType(AssignmentType.USER);
        assignment.setValue(userId);
        return assignment;
    }
}
