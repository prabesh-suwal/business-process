package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.TaskConfigurationDTO;
import com.enterprise.workflow.entity.ProcessTemplate;
import com.enterprise.workflow.entity.TaskConfiguration;
import com.enterprise.workflow.repository.ProcessTemplateRepository;
import com.enterprise.workflow.repository.TaskConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing task configurations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskConfigurationService {

    private final TaskConfigurationRepository taskConfigRepository;
    private final ProcessTemplateRepository processTemplateRepository;

    /**
     * Create or update task configuration.
     */
    public TaskConfigurationDTO saveTaskConfig(UUID processTemplateId, TaskConfigurationDTO dto) {
        ProcessTemplate template = processTemplateRepository.findById(processTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + processTemplateId));

        TaskConfiguration config = taskConfigRepository
                .findByProcessTemplateIdAndTaskKey(processTemplateId, dto.getTaskKey())
                .orElse(TaskConfiguration.builder()
                        .processTemplate(template)
                        .taskKey(dto.getTaskKey())
                        .build());

        // Update fields
        config.setTaskName(dto.getTaskName());
        config.setDescription(dto.getDescription());
        config.setTaskOrder(dto.getTaskOrder());
        config.setFormId(dto.getFormId());
        config.setFormVersion(dto.getFormVersion());
        config.setRequiresMakerChecker(dto.getRequiresMakerChecker());
        config.setCheckerRoles(dto.getCheckerRoles());
        config.setSlaHours(dto.getSlaHours());
        config.setWarningHours(dto.getWarningHours());
        config.setEscalationRole(dto.getEscalationRole());
        config.setCanReturnTo(dto.getCanReturnTo());
        config.setAssignmentNotificationCode(dto.getAssignmentNotificationCode());
        config.setCompletionNotificationCode(dto.getCompletionNotificationCode());
        config.setSlaWarningNotificationCode(dto.getSlaWarningNotificationCode());
        config.setSlaBreachNotificationCode(dto.getSlaBreachNotificationCode());
        config.setAssignmentConfig(dto.getAssignmentConfig());
        config.setViewerConfig(dto.getViewerConfig());
        config.setFormConfig(dto.getFormConfig());
        config.setEscalationConfig(dto.getEscalationConfig());
        config.setConditionConfig(dto.getConditionConfig());

        // Inject default outcome buttons if admin didn't configure any
        Map<String, Object> outcomeConfig = dto.getOutcomeConfig();
        if (isOutcomeConfigEmpty(outcomeConfig)) {
            int stepOrder = resolveStepOrder(dto);
            outcomeConfig = buildDefaultOutcomeConfig(stepOrder);
            log.info("No outcome config for step '{}' (order={}), injecting defaults: {}",
                    dto.getTaskKey(), stepOrder,
                    ((List<?>) outcomeConfig.get("options")).stream()
                            .map(o -> ((Map<?, ?>) o).get("actionType")).toList());
        }
        config.setOutcomeConfig(outcomeConfig);

        config.setStepOrder(dto.getStepOrder());
        config.setActive(dto.getActive() != null ? dto.getActive() : true);
        config.setConfig(dto.getConfig());

        config = taskConfigRepository.save(config);
        log.info("Saved task configuration: {} for template {}", dto.getTaskKey(), processTemplateId);

        return toDTO(config);
    }

    /**
     * Check if the outcomeConfig is effectively empty (null, empty map, or has no
     * options).
     */
    private boolean isOutcomeConfigEmpty(Map<String, Object> outcomeConfig) {
        if (outcomeConfig == null || outcomeConfig.isEmpty())
            return true;
        Object options = outcomeConfig.get("options");
        if (options == null)
            return true;
        if (options instanceof List<?> list)
            return list.isEmpty();
        return false;
    }

    /**
     * Resolve the step order from the DTO, using stepOrder, taskOrder, or
     * defaulting to 1.
     */
    private int resolveStepOrder(TaskConfigurationDTO dto) {
        if (dto.getStepOrder() != null && dto.getStepOrder() > 0)
            return dto.getStepOrder();
        if (dto.getTaskOrder() != null && dto.getTaskOrder() > 0)
            return dto.getTaskOrder();
        return 1;
    }

    /**
     * Build default outcome config with action buttons based on step position.
     * - All steps: APPROVE, REJECT, SEND_BACK
     * - Steps 3+: Also BACK_TO_INITIATOR
     */
    private Map<String, Object> buildDefaultOutcomeConfig(int stepOrder) {
        List<Map<String, Object>> options = new ArrayList<>();

        // APPROVE — always present
        options.add(Map.of(
                "actionType", "APPROVE",
                "label", "Approve",
                "style", "success",
                "sets", Map.of("decision", "APPROVED"),
                "requiresComment", false));

        // REJECT — always present
        options.add(Map.of(
                "actionType", "REJECT",
                "label", "Reject",
                "style", "danger",
                "sets", Map.of("decision", "REJECTED"),
                "requiresComment", true));

        // SEND_BACK — always present
        options.add(Map.of(
                "actionType", "SEND_BACK",
                "label", "Send Back",
                "style", "warning",
                "sets", Map.of("decision", "SENT_BACK"),
                "requiresComment", true));

        // BACK_TO_INITIATOR — only for step 3 and beyond
        if (stepOrder >= 3) {
            options.add(Map.of(
                    "actionType", "BACK_TO_INITIATOR",
                    "label", "Return to Initiator",
                    "style", "warning",
                    "sets", Map.of("decision", "RETURNED_TO_INITIATOR"),
                    "requiresComment", true));
        }

        Map<String, Object> defaultConfig = new LinkedHashMap<>();
        defaultConfig.put("variableName", "nextStage");
        defaultConfig.put("options", options);
        defaultConfig.put("autoGenerated", true);
        return defaultConfig;
    }

    /**
     * Get all task configurations for a template.
     */
    @Transactional(readOnly = true)
    public List<TaskConfigurationDTO> getTaskConfigs(UUID processTemplateId) {
        return taskConfigRepository.findByProcessTemplateIdOrderByTaskOrderAsc(processTemplateId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get task configuration by template and task key.
     */
    @Transactional(readOnly = true)
    public TaskConfigurationDTO getTaskConfig(UUID processTemplateId, String taskKey) {
        return taskConfigRepository.findByProcessTemplateIdAndTaskKey(processTemplateId, taskKey)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Task config not found: " + taskKey + " in template " + processTemplateId));
    }

    /**
     * Delete task configuration.
     */
    public void deleteTaskConfig(UUID configId) {
        taskConfigRepository.deleteById(configId);
        log.info("Deleted task configuration: {}", configId);
    }

    /**
     * Bulk save task configurations for a template.
     */
    public List<TaskConfigurationDTO> saveAllTaskConfigs(UUID processTemplateId, List<TaskConfigurationDTO> configs) {
        // Delete existing
        taskConfigRepository.deleteByProcessTemplateId(processTemplateId);

        // Save new
        return configs.stream()
                .map(dto -> saveTaskConfig(processTemplateId, dto))
                .collect(Collectors.toList());
    }

    private TaskConfigurationDTO toDTO(TaskConfiguration config) {
        return TaskConfigurationDTO.builder()
                .id(config.getId())
                .processTemplateId(config.getProcessTemplate().getId())
                .taskKey(config.getTaskKey())
                .taskName(config.getTaskName())
                .description(config.getDescription())
                .taskOrder(config.getTaskOrder())
                .formId(config.getFormId())
                .formVersion(config.getFormVersion())
                .requiresMakerChecker(config.getRequiresMakerChecker())
                .checkerRoles(config.getCheckerRoles())
                .slaHours(config.getSlaHours())
                .warningHours(config.getWarningHours())
                .escalationRole(config.getEscalationRole())
                .canReturnTo(config.getCanReturnTo())
                .assignmentNotificationCode(config.getAssignmentNotificationCode())
                .completionNotificationCode(config.getCompletionNotificationCode())
                .slaWarningNotificationCode(config.getSlaWarningNotificationCode())
                .slaBreachNotificationCode(config.getSlaBreachNotificationCode())
                .assignmentConfig(config.getAssignmentConfig())
                .viewerConfig(config.getViewerConfig())
                .formConfig(config.getFormConfig())
                .escalationConfig(config.getEscalationConfig())
                .conditionConfig(config.getConditionConfig())
                .outcomeConfig(config.getOutcomeConfig())
                .stepOrder(config.getStepOrder())
                .active(config.getActive())
                .config(config.getConfig())
                .build();
    }
}
