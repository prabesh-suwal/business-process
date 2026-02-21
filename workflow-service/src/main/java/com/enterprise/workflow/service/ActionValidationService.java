package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.TaskConfigurationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates submitted workflow actions against configured outcome options.
 *
 * This is the backend enforcement layer that ensures only admin-configured
 * actions can be executed at each workflow step. The configured options live
 * in TaskConfiguration.config.outcomeConfig.options[].
 *
 * Each option has the structure:
 * {
 * "label": "Approve", // action identifier (matched case-insensitively)
 * "style": "success", // UI hint: success | danger | warning | info
 * "icon": "check-circle", // lucide icon name
 * "requiresComment": false, // if true, comment is mandatory
 * "confirmationMessage": "...",// custom dialog text
 * "sets": { "nextStage": "APPROVED" } // variables injected into process
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionValidationService {

    private final TaskConfigurationService taskConfigService;

    /**
     * Validate that the submitted action is one of the configured outcome options.
     * Matches by actionType (stable identifier), not label (user-editable).
     *
     * @param processTemplateId the process template UUID
     * @param taskKey           the BPMN task definition key
     * @param actionTypeStr     the actionType submitted by the frontend (e.g.,
     *                          "REJECT")
     * @throws IllegalArgumentException if the action is not in the configured
     *                                  options
     */
    public void validateAction(UUID processTemplateId, String taskKey, String actionTypeStr) {
        List<Map<String, Object>> options = getConfiguredOptions(processTemplateId, taskKey);

        if (options == null || options.isEmpty()) {
            // No outcome config → allow any action (backward compatibility)
            log.debug("No outcome options configured for template={} task={}, allowing action '{}'",
                    processTemplateId, taskKey, actionTypeStr);
            return;
        }

        boolean valid = options.stream()
                .anyMatch(opt -> {
                    String optType = (String) opt.get("actionType");
                    return optType != null && optType.equalsIgnoreCase(actionTypeStr);
                });

        if (!valid) {
            List<String> validTypes = options.stream()
                    .map(opt -> (String) opt.get("actionType"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.warn("Invalid actionType '{}' for template={} task={}. Valid types: {}",
                    actionTypeStr, processTemplateId, taskKey, validTypes);

            throw new IllegalArgumentException(
                    "Invalid action type '" + actionTypeStr + "'. Allowed types: " + validTypes);
        }

        log.debug("Action '{}' validated for template={} task={}", actionTypeStr, processTemplateId, taskKey);
    }

    /**
     * Validate that a comment is provided when the action requires it.
     * Matches by actionType (stable identifier).
     */
    public void validateComment(UUID processTemplateId, String taskKey, String actionTypeStr, String comment) {
        List<Map<String, Object>> options = getConfiguredOptions(processTemplateId, taskKey);
        if (options == null || options.isEmpty())
            return;

        options.stream()
                .filter(opt -> actionTypeStr.equalsIgnoreCase((String) opt.get("actionType")))
                .findFirst()
                .ifPresent(opt -> {
                    Boolean requiresComment = (Boolean) opt.get("requiresComment");
                    if (Boolean.TRUE.equals(requiresComment)
                            && (comment == null || comment.isBlank())) {
                        String label = (String) opt.get("label");
                        throw new IllegalArgumentException(
                                "Action '" + (label != null ? label : actionTypeStr) + "' requires a comment.");
                    }
                });
    }

    /**
     * Get the configured option map for a specific action.
     * Matches by actionType (stable identifier).
     *
     * @return the option map, or null if not found
     */
    public Map<String, Object> getActionOption(UUID processTemplateId, String taskKey, String actionTypeStr) {
        List<Map<String, Object>> options = getConfiguredOptions(processTemplateId, taskKey);
        if (options == null)
            return null;

        return options.stream()
                .filter(opt -> actionTypeStr.equalsIgnoreCase((String) opt.get("actionType")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the list of configured action types for a task.
     */
    public List<String> getConfiguredActionLabels(UUID processTemplateId, String taskKey) {
        List<Map<String, Object>> options = getConfiguredOptions(processTemplateId, taskKey);
        if (options == null)
            return Collections.emptyList();

        return options.stream()
                .map(opt -> (String) opt.get("actionType"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Extract configured options from TaskConfiguration.
     * Reads from dedicated outcomeConfig field first, then falls back to
     * legacy config.outcomeConfig path for backward compatibility.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getConfiguredOptions(UUID processTemplateId, String taskKey) {
        try {
            TaskConfigurationDTO taskConfig = taskConfigService.getTaskConfig(processTemplateId, taskKey);

            // 1. Try dedicated outcomeConfig field (new)
            Map<String, Object> outcomeConfig = taskConfig.getOutcomeConfig();

            // 2. Fallback: legacy config.outcomeConfig path
            if (outcomeConfig == null) {
                Map<String, Object> config = taskConfig.getConfig();
                if (config != null) {
                    outcomeConfig = (Map<String, Object>) config.get("outcomeConfig");
                }
            }

            if (outcomeConfig == null)
                return null;

            return (List<Map<String, Object>>) outcomeConfig.get("options");
        } catch (IllegalArgumentException e) {
            log.debug("No task config found for template={} task={}: {}", processTemplateId, taskKey, e.getMessage());
            return null;
        }
    }
}
