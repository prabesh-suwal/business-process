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

    // Additional config
    private Map<String, Object> config;
}
