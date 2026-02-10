package com.enterprise.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an integrity check on audit logs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrityCheckResult {
    private boolean verified;
    private int checkedCount;
    private int violationsCount;
    private Long startSequence;
    private Long endSequence;
    private String message;
    private String details;
}
