package com.cas.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for user role assignments with constraints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleDto {
    private UUID id;
    private UUID roleId;
    private String roleCode;
    private String roleName;
    private String productCode;
    private String productName;
    private Map<String, Object> constraints;
    private Instant assignedAt;
    private List<String> permissions;

    /**
     * Request to assign a role to a user with constraints.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignRoleRequest {
        private UUID roleId;
        private Map<String, Object> constraints;
    }

    /**
     * Request to update role constraints.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateConstraintsRequest {
        private Map<String, Object> constraints;
    }

    /**
     * Constraints configuration for UI.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstraintsConfig {
        private List<String> branchIds;
        private List<String> regionIds;
        private Number maxApprovalAmount;
    }
}
