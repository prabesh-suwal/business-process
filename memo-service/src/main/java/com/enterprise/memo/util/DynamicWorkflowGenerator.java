package com.enterprise.memo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Generates dynamic BPMN workflows from workflow override configurations.
 * Used when a topic doesn't have a pre-configured workflow but the user
 * defines custom approval steps.
 */
@Slf4j
@Component
public class DynamicWorkflowGenerator {

    private static final String BPMN_HEADER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                         xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
                         xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
                         targetNamespace="http://www.flowable.org/processdef">
            """;

    private static final String BPMN_FOOTER = """
            </definitions>
            """;

    /**
     * Generate BPMN XML from workflow overrides.
     * 
     * @param workflowOverrides The override configuration from Memo
     * @param memoId            The memo ID (used as business key reference)
     * @return BPMN XML string
     */
    @SuppressWarnings("unchecked")
    public String generateBpmn(Map<String, Object> workflowOverrides, String memoId) {
        if (workflowOverrides == null) {
            throw new IllegalArgumentException("workflowOverrides cannot be null");
        }

        Boolean customWorkflow = (Boolean) workflowOverrides.get("customWorkflow");
        if (!Boolean.TRUE.equals(customWorkflow)) {
            throw new IllegalArgumentException("customWorkflow must be true");
        }

        List<Map<String, Object>> steps = (List<Map<String, Object>>) workflowOverrides.get("steps");
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("At least one step is required");
        }

        String processKey = "memo_workflow_" + memoId.replace("-", "_");
        String processName = "Memo Workflow (" + memoId.substring(0, 8) + ")";

        log.info("Generating dynamic BPMN with {} steps for memo {}", steps.size(), memoId);

        StringBuilder bpmn = new StringBuilder();
        bpmn.append(BPMN_HEADER);

        // Start process definition
        bpmn.append("  <process id=\"").append(processKey).append("\" name=\"").append(processName)
                .append("\" isExecutable=\"true\">\n");

        // Start event
        bpmn.append("    <startEvent id=\"start\" name=\"Start\"/>\n");

        // Generate sequence flows and user tasks
        String previousElement = "start";

        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String stepId = "task_" + (i + 1);
            String stepName = (String) step.getOrDefault("name", "Approval Step " + (i + 1));

            // Extract assignment rules for candidate groups
            String candidateGroups = extractCandidateGroups(step);

            // Add sequence flow from previous element
            bpmn.append("    <sequenceFlow id=\"flow_").append(i).append("\" ")
                    .append("sourceRef=\"").append(previousElement).append("\" ")
                    .append("targetRef=\"").append(stepId).append("\"/>\n");

            // Add user task with task listener for dynamic assignment
            bpmn.append("    <userTask id=\"").append(stepId).append("\" ")
                    .append("name=\"").append(escapeXml(stepName)).append("\"");

            if (candidateGroups != null && !candidateGroups.isEmpty()) {
                bpmn.append(" flowable:candidateGroups=\"").append(candidateGroups).append("\"");
            }

            bpmn.append(">\n");

            // Add extension elements with task listener for memo-service notification
            // Use assignmentTaskListener which directly webhooks to memo-service
            bpmn.append("      <extensionElements>\n");
            bpmn.append("        <flowable:taskListener event=\"create\" ")
                    .append("delegateExpression=\"${assignmentTaskListener}\"/>\n");
            bpmn.append("      </extensionElements>\n");

            bpmn.append("    </userTask>\n");

            previousElement = stepId;
        }

        // End event
        bpmn.append("    <sequenceFlow id=\"flow_end\" sourceRef=\"").append(previousElement)
                .append("\" targetRef=\"end\"/>\n");
        bpmn.append("    <endEvent id=\"end\" name=\"End\"/>\n");

        // Close process
        bpmn.append("  </process>\n");

        bpmn.append(BPMN_FOOTER);

        String result = bpmn.toString();
        log.debug("Generated BPMN:\n{}", result);
        return result;
    }

    /**
     * Generate a unique process key for the memo's dynamic workflow.
     */
    public String generateProcessKey(String memoId) {
        return "memo_workflow_" + memoId.replace("-", "_");
    }

    /**
     * Extract candidate groups (role IDs) from step assignment rules.
     */
    @SuppressWarnings("unchecked")
    private String extractCandidateGroups(Map<String, Object> step) {
        List<Map<String, Object>> rules = (List<Map<String, Object>>) step.get("rules");
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        StringBuilder groups = new StringBuilder();
        for (Map<String, Object> rule : rules) {
            Map<String, Object> criteria = (Map<String, Object>) rule.get("criteria");
            if (criteria == null)
                continue;

            // Extract roleIds from criteria
            List<String> roleIds = (List<String>) criteria.get("roleIds");
            if (roleIds != null) {
                for (String roleId : roleIds) {
                    if (groups.length() > 0) {
                        groups.append(",");
                    }
                    groups.append(roleId);
                }
            }

            // Also check for direct roles array
            List<String> roles = (List<String>) criteria.get("roles");
            if (roles != null) {
                for (String role : roles) {
                    if (groups.length() > 0) {
                        groups.append(",");
                    }
                    groups.append(role);
                }
            }
        }

        return groups.length() > 0 ? groups.toString() : null;
    }

    /**
     * Escape XML special characters.
     */
    private String escapeXml(String input) {
        if (input == null)
            return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
