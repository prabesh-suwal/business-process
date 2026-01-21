package com.cas.common.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for policy evaluation.
 * Sent to policy-engine-service /api/evaluate endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationRequest {

    /** Subject - who is making the request */
    private Subject subject;

    /** Action - what operation is being performed */
    private Action action;

    /** Resource - what is being accessed */
    private Resource resource;

    /** Product context - which product the request is for */
    private String product;

    /** Environment context (time, IP, etc.) */
    private Map<String, Object> environment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subject {
        private String userId;
        private String username;
        private String email;
        private java.util.List<String> roles;
        private java.util.List<String> permissions;
        private java.util.List<String> branchIds;
        private java.util.List<String> departmentIds;
        private java.util.List<String> regionIds;
        private Double approvalLimit;
        private Integer hierarchyLevel;
        private Map<String, Object> attributes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        private String name; // e.g., "READ", "CREATE", "UPDATE", "DELETE"
        private String httpMethod; // GET, POST, PUT, DELETE
        private String path; // /api/branches/123
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private String type; // e.g., "BRANCH", "LOAN", "USER"
        private String id; // specific resource ID if applicable
        private String branchId;
        private String regionId;
        private Double amount;
        private String ownerId;
        private String status;
        private Map<String, Object> attributes;
    }
}
