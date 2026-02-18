package com.enterprise.workflow.listener;

import com.enterprise.workflow.service.AssignmentResolverService;
import com.enterprise.workflow.service.AssignmentResolverService.AssignmentResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.cas.common.webclient.InternalWebClient;

import java.util.*;

/**
 * Flowable Task Listener that:
 * 1. Resolves assignment using LOCAL AssignmentResolverService (centralized)
 * 2. Notifies product-service (memo-service, lms-service) via webhook for
 * business records
 * 
 * Assignment is now FULLY CENTRALIZED in workflow-service.
 * Product services only handle business records (MemoTask, etc.).
 */
@Component("assignmentTaskListener")
@Slf4j
public class AssignmentTaskListener implements TaskListener {

    private final WebClient.Builder webClientBuilder;
    private final AssignmentResolverService assignmentResolver;

    @Value("${memo.service.url:http://localhost:9008}")
    private String memoServiceUrl;

    public AssignmentTaskListener(@InternalWebClient WebClient.Builder webClientBuilder,
            AssignmentResolverService assignmentResolver) {
        this.webClientBuilder = webClientBuilder;
        this.assignmentResolver = assignmentResolver;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        String taskKey = delegateTask.getTaskDefinitionKey();
        String taskName = delegateTask.getName();
        String processInstanceId = delegateTask.getProcessInstanceId();
        Map<String, Object> variables = delegateTask.getVariables();

        log.info("Task created: {} ({}) in process {}", taskName, taskKey, processInstanceId);

        // Get processTemplateId from variables (set when process was started)
        UUID processTemplateId = getProcessTemplateId(variables);
        log.info("ProcessTemplateId from variables: {}", processTemplateId);

        // Step 1: Resolve assignment LOCALLY using centralized service
        AssignmentResult assignment = null;
        if (processTemplateId != null) {
            try {
                assignment = assignmentResolver.resolveAssignment(
                        processTemplateId, taskKey, variables);
                log.info("Assignment resolved for task {}: groups={}, users={}",
                        taskKey, assignment.getCandidateGroups(), assignment.getCandidateUsers());
            } catch (Exception e) {
                log.warn("Local assignment resolution failed for task {}: {}", taskKey, e.getMessage());
            }
        } else {
            log.warn("No processTemplateId found in variables, keys: {}", variables.keySet());
        }

        // Apply assignment to Flowable task
        if (assignment != null) {
            applyAssignment(delegateTask, assignment);
            log.info("Applied assignment for task {}: {}", taskKey, assignment.getDescription());
        }

        // Step 2: Notify product-service for business records ONLY
        // Assignment is already done - product service just creates MemoTask/LoanTask
        notifyProductService(delegateTask, assignment);
    }

    /**
     * Apply assignment from resolver result to Flowable task.
     */
    private void applyAssignment(DelegateTask task, AssignmentResult assignment) {
        if (assignment.getAssignee() != null && !assignment.getAssignee().isBlank()) {
            task.setAssignee(assignment.getAssignee());
        }

        if (assignment.getCandidateUsers() != null) {
            for (String user : assignment.getCandidateUsers()) {
                task.addCandidateUser(user);
            }
        }

        if (assignment.getCandidateGroups() != null) {
            for (String group : assignment.getCandidateGroups()) {
                task.addCandidateGroup(group);
            }
        }
    }

    /**
     * Notify product-service to create business records.
     * Assignment is already resolved - we pass it for logging/storage.
     */
    private void notifyProductService(DelegateTask delegateTask, AssignmentResult assignment) {
        Map<String, Object> variables = delegateTask.getVariables();
        Object memoIdObj = variables.get("memoId");
        Object loanIdObj = variables.get("loanId");

        if (memoIdObj != null) {
            notifyMemoService(delegateTask, assignment);
        } else if (loanIdObj != null) {
            log.info("LMS task created - lms-service webhook not yet implemented");
        } else {
            log.debug("No product ID (memoId/loanId) found in variables");
        }
    }

    /**
     * Notify memo-service to create MemoTask record.
     */
    private void notifyMemoService(DelegateTask delegateTask, AssignmentResult assignment) {
        String taskKey = delegateTask.getTaskDefinitionKey();

        try {
            UUID memoId = UUID.fromString(delegateTask.getVariable("memoId").toString());

            TaskCreatedEvent event = new TaskCreatedEvent();
            event.setTaskId(delegateTask.getId());
            event.setMemoId(memoId);
            event.setTaskDefinitionKey(taskKey);
            event.setTaskName(delegateTask.getName());
            event.setProcessInstanceId(delegateTask.getProcessInstanceId());

            // Pass resolved assignment to memo-service (for MemoTask record)
            // If assignment was resolved from config, use it
            // Otherwise, fall back to BPMN-defined candidateGroups
            if (assignment != null && assignment.getCandidateGroups() != null
                    && !assignment.getCandidateGroups().isEmpty()) {
                event.setCandidateGroups(assignment.getCandidateGroups());
                event.setCandidateUsers(assignment.getCandidateUsers());
            } else {
                // Fall back to BPMN-defined candidates (for dynamic workflows)
                Set<org.flowable.identitylink.api.IdentityLink> candidates = delegateTask.getCandidates();
                if (candidates != null && !candidates.isEmpty()) {
                    List<String> groups = candidates.stream()
                            .filter(c -> c.getGroupId() != null)
                            .map(org.flowable.identitylink.api.IdentityLink::getGroupId)
                            .toList();
                    List<String> users = candidates.stream()
                            .filter(c -> c.getUserId() != null)
                            .map(org.flowable.identitylink.api.IdentityLink::getUserId)
                            .toList();
                    event.setCandidateGroups(groups);
                    event.setCandidateUsers(users);
                    log.info("Using BPMN-defined candidates for task {}: groups={}, users={}", taskKey, groups, users);
                }
            }

            // Fire-and-forget notification (don't block on response)
            webClientBuilder.build()
                    .post()
                    .uri(memoServiceUrl + "/api/workflow-webhook/task-created")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                            result -> log.info("Memo-service notified for task {}", taskKey),
                            error -> log.error("Failed to notify memo-service: {}", error.getMessage()));

        } catch (Exception e) {
            log.error("Error preparing memo-service notification for task {}: {}", taskKey, e.getMessage());
        }
    }

    /**
     * Extract processTemplateId from workflow variables.
     */
    private UUID getProcessTemplateId(Map<String, Object> variables) {
        Object value = variables.get("processTemplateId");
        if (value == null)
            return null;

        try {
            if (value instanceof UUID) {
                return (UUID) value;
            }
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid processTemplateId format: {}", value);
            return null;
        }
    }

    @Data
    public static class TaskCreatedEvent {
        private String taskId;
        private UUID memoId;
        private String taskDefinitionKey;
        private String taskName;
        private String processInstanceId;
        private List<String> candidateGroups;
        private List<String> candidateUsers;
    }
}
