package com.enterprise.memo.controller;

import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;
import com.enterprise.memo.dto.AssignmentRulesDTO;
import com.enterprise.memo.entity.GatewayDecisionRule;
import com.enterprise.memo.entity.WorkflowStepConfig;
import com.enterprise.memo.service.WorkflowConfigService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API for workflow step configuration.
 * Used by admin UI to configure assignment, form, SLA, escalation per step.
 */
@RestController
@RequestMapping("/api/topics/{topicId}/workflow-config")
@RequiredArgsConstructor
@Slf4j
public class WorkflowConfigController {

    private final WorkflowConfigService configService;

    // ==================== Step Config ====================

    @GetMapping("/steps")
    public ResponseEntity<List<StepConfigDTO>> getStepConfigs(@PathVariable UUID topicId) {
        List<WorkflowStepConfig> configs = configService.getStepConfigs(topicId);
        return ResponseEntity.ok(configs.stream()
                .map(this::toStepDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/steps/{taskKey}")
    public ResponseEntity<StepConfigDTO> getStepConfig(
            @PathVariable UUID topicId,
            @PathVariable String taskKey) {
        return configService.getStepConfig(topicId, taskKey)
                .map(this::toStepDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/steps/{taskKey}")
    public ResponseEntity<StepConfigDTO> saveStepConfig(
            @PathVariable UUID topicId,
            @PathVariable String taskKey,
            @RequestBody SaveStepConfigRequest request) {
        WorkflowStepConfig config = configService.saveStepConfig(
                topicId, taskKey,
                request.getTaskName(),
                request.getAssignmentConfig(),
                request.getFormConfig(),
                request.getSlaConfig(),
                request.getEscalationConfig(),
                request.getViewerConfig(),
                request.getConditionConfig());
        return ResponseEntity.ok(toStepDto(config));
    }

    // ==================== Gateway Rules ====================

    @GetMapping("/gateways")
    public ResponseEntity<List<GatewayRuleDTO>> getGatewayRules(@PathVariable UUID topicId) {
        List<GatewayDecisionRule> rules = configService.getGatewayRules(topicId);
        return ResponseEntity.ok(rules.stream()
                .map(this::toGatewayDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/gateways/{gatewayKey}")
    public ResponseEntity<GatewayRuleDTO> getGatewayRule(
            @PathVariable UUID topicId,
            @PathVariable String gatewayKey) {
        return configService.getActiveGatewayRule(topicId, gatewayKey)
                .map(this::toGatewayDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/gateways/{gatewayKey}")
    public ResponseEntity<GatewayRuleDTO> saveGatewayRule(
            @PathVariable UUID topicId,
            @PathVariable String gatewayKey,
            @RequestBody SaveGatewayRuleRequest request) {
        GatewayDecisionRule rule = configService.saveGatewayRule(
                topicId, gatewayKey,
                request.getGatewayName(),
                request.getRules(),
                request.getDefaultFlow(),
                request.isActivate());
        return ResponseEntity.ok(toGatewayDto(rule));
    }

    @PostMapping("/gateways/{ruleId}/activate")
    public ResponseEntity<Void> activateGatewayRule(
            @PathVariable UUID topicId,
            @PathVariable UUID ruleId) {
        UserContext user = UserContextHolder.getContext();
        UUID activatedBy = user != null && user.getUserId() != null ? UUID.fromString(user.getUserId()) : null;
        configService.activateGatewayRule(ruleId, activatedBy);
        return ResponseEntity.ok().build();
    }

    // ==================== Runtime Evaluation ====================

    @PostMapping("/gateways/{gatewayKey}/evaluate")
    public ResponseEntity<EvaluationResult> evaluateGateway(
            @PathVariable UUID topicId,
            @PathVariable String gatewayKey,
            @RequestBody Map<String, Object> memoData) {
        String result = configService.evaluateGateway(topicId, gatewayKey, memoData);
        return ResponseEntity.ok(new EvaluationResult(gatewayKey, result));
    }

    // ==================== Assignment Preview ====================

    /**
     * Preview which users would be assigned based on the given assignment rules.
     * This helps admins validate their assignment configuration before saving.
     */
    @PostMapping("/assignments/preview")
    public ResponseEntity<AssignmentPreviewResult> previewAssignment(
            @PathVariable UUID topicId,
            @RequestBody AssignmentRulesDTO rules) {
        log.info("Previewing assignment for topic {} with {} rules", topicId,
                rules.getRules() != null ? rules.getRules().size() : 0);

        // TODO: Call CAS/Organization service to resolve users matching criteria
        // For now, return a placeholder response showing the resolved candidate groups
        List<String> candidateGroups = new ArrayList<>();
        List<UserPreviewDTO> matchedUsers = new ArrayList<>();

        if (rules.getRules() != null) {
            for (AssignmentRulesDTO.AssignmentRule rule : rules.getRules()) {
                var criteria = rule.getCriteria();
                if (criteria != null) {
                    // Add role-based candidate groups
                    if (criteria.getRoleIds() != null) {
                        for (UUID roleId : criteria.getRoleIds()) {
                            candidateGroups.add("ROLE_" + roleId.toString());
                        }
                    }
                    // Add department-based candidate groups
                    if (criteria.getDepartmentIds() != null) {
                        for (UUID deptId : criteria.getDepartmentIds()) {
                            candidateGroups.add("DEPT_" + deptId.toString());
                        }
                    }
                    // Add branch-based candidate groups
                    if (criteria.getBranchIds() != null) {
                        for (UUID branchId : criteria.getBranchIds()) {
                            candidateGroups.add("BRANCH_" + branchId.toString());
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok(AssignmentPreviewResult.builder()
                .topicId(topicId)
                .candidateGroups(candidateGroups)
                .matchedUsers(matchedUsers)
                .message("Preview generated. Connect to CAS for actual user resolution.")
                .build());
    }

    // ==================== DTOs ====================

    private StepConfigDTO toStepDto(WorkflowStepConfig config) {
        StepConfigDTO dto = new StepConfigDTO();
        dto.setId(config.getId());
        dto.setTaskKey(config.getTaskKey());
        dto.setTaskName(config.getTaskName());
        dto.setStepOrder(config.getStepOrder());
        dto.setAssignmentConfig(config.getAssignmentConfig());
        dto.setFormConfig(config.getFormConfig());
        dto.setSlaConfig(config.getSlaConfig());
        dto.setEscalationConfig(config.getEscalationConfig());
        dto.setViewerConfig(config.getViewerConfig());
        dto.setConditionConfig(config.getConditionConfig());
        dto.setActive(config.getActive());
        return dto;
    }

    private GatewayRuleDTO toGatewayDto(GatewayDecisionRule rule) {
        GatewayRuleDTO dto = new GatewayRuleDTO();
        dto.setId(rule.getId());
        dto.setGatewayKey(rule.getGatewayKey());
        dto.setGatewayName(rule.getGatewayName());
        dto.setRules(rule.getRules());
        dto.setDefaultFlow(rule.getDefaultFlow());
        dto.setVersion(rule.getVersion());
        dto.setActive(rule.getActive());
        return dto;
    }

    @Data
    public static class StepConfigDTO {
        private UUID id;
        private String taskKey;
        private String taskName;
        private Integer stepOrder;
        private Map<String, Object> assignmentConfig;
        private Map<String, Object> formConfig;
        private Map<String, Object> slaConfig;
        private Map<String, Object> escalationConfig;
        private Map<String, Object> viewerConfig;
        private Map<String, Object> conditionConfig;
        private Boolean active;
    }

    @Data
    public static class SaveStepConfigRequest {
        private String taskName;
        private Map<String, Object> assignmentConfig;
        private Map<String, Object> formConfig;
        private Map<String, Object> slaConfig;
        private Map<String, Object> escalationConfig;
        private Map<String, Object> viewerConfig;
        private Map<String, Object> conditionConfig;
    }

    @Data
    public static class GatewayRuleDTO {
        private UUID id;
        private String gatewayKey;
        private String gatewayName;
        private List<Map<String, Object>> rules;
        private String defaultFlow;
        private Integer version;
        private Boolean active;
    }

    @Data
    public static class SaveGatewayRuleRequest {
        private String gatewayName;
        private List<Map<String, Object>> rules;
        private String defaultFlow;
        private boolean activate;
    }

    @Data
    public static class EvaluationResult {
        private final String gatewayKey;
        private final String targetFlow;

        public EvaluationResult(String gatewayKey, String targetFlow) {
            this.gatewayKey = gatewayKey;
            this.targetFlow = targetFlow;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentPreviewResult {
        private UUID topicId;
        private List<String> candidateGroups;
        private List<UserPreviewDTO> matchedUsers;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPreviewDTO {
        private UUID userId;
        private String username;
        private String fullName;
        private String email;
        private String branch;
        private String department;
        private List<String> roles;
        private String matchedRule; // Which rule matched this user
    }
}
