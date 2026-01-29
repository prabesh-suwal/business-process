package com.enterprise.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfigurationDTO {
    private UUID id;
    private UUID processTemplateId;
    private String taskKey;
    private String taskName;
    private String description;
    private Integer taskOrder;

    // Form mapping
    private UUID formId;
    private Integer formVersion;

    // Maker-Checker
    private Boolean requiresMakerChecker;
    private List<String> checkerRoles;

    // SLA
    private Integer slaHours;
    private Integer warningHours;
    private String escalationRole;

    // Return paths
    private List<String> canReturnTo;

    // Notifications
    private String assignmentNotificationCode;
    private String completionNotificationCode;
    private String slaWarningNotificationCode;
    private String slaBreachNotificationCode;

    // Assignment override
    private Map<String, Object> assignmentConfig;

    // Viewer config (who can view tasks at this step)
    private Map<String, Object> viewerConfig;

    // Form config (form behavior at this step)
    private Map<String, Object> formConfig;

    // Escalation config (multi-level escalation rules)
    private Map<String, Object> escalationConfig;

    // Condition config (branching/routing conditions)
    private Map<String, Object> conditionConfig;

    // Step order for display
    private Integer stepOrder;

    // Active flag
    private Boolean active;

    // Additional config
    private Map<String, Object> config;
}
