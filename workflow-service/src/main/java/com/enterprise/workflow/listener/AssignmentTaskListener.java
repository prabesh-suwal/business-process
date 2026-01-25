package com.enterprise.workflow.listener;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Flowable Task Listener that notifies memo-service when a task is created.
 * 
 * Workflow-service does NOT decide assignments - it delegates to memo-service
 * via webhook, keeping this service as a pure Flowable engine.
 */
@Component("assignmentTaskListener")
@Slf4j
public class AssignmentTaskListener implements TaskListener {

    private final WebClient.Builder webClientBuilder;

    @Value("${memo.service.url:http://localhost:9008}")
    private String memoServiceUrl;

    public AssignmentTaskListener(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        String taskKey = delegateTask.getTaskDefinitionKey();
        String taskName = delegateTask.getName();
        String taskId = delegateTask.getId();

        log.info("Task created: {} ({}) - notifying memo-service", taskName, taskKey);

        try {
            // Get memoId from process variables
            Object memoIdObj = delegateTask.getVariable("memoId");
            if (memoIdObj == null) {
                log.warn("No memoId found in process variables, skipping webhook");
                return;
            }

            UUID memoId = UUID.fromString(memoIdObj.toString());

            // Build webhook request
            TaskCreatedEvent event = new TaskCreatedEvent();
            event.setTaskId(taskId);
            event.setMemoId(memoId);
            event.setTaskDefinitionKey(taskKey);
            event.setTaskName(taskName);
            event.setProcessInstanceId(delegateTask.getProcessInstanceId());
            event.setProcessVariables(delegateTask.getVariables());

            // Call memo-service webhook
            TaskCreatedResponse response = webClientBuilder.build()
                    .post()
                    .uri(memoServiceUrl + "/api/workflow-webhook/task-created")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(TaskCreatedResponse.class)
                    .block();

            if (response != null) {
                // Apply assignment from memo-service
                applyAssignment(delegateTask, response);
                log.info("Applied assignment from memo-service for task {}", taskKey);
            }

        } catch (Exception e) {
            log.error("Error calling memo-service webhook for task {}: {}", taskKey, e.getMessage());
            // Don't throw - let the task be created without assignment
        }
    }

    private void applyAssignment(DelegateTask task, TaskCreatedResponse response) {
        if (response.getAssignee() != null && !response.getAssignee().isBlank()) {
            task.setAssignee(response.getAssignee());
        }

        if (response.getCandidateUsers() != null) {
            for (String user : response.getCandidateUsers()) {
                task.addCandidateUser(user);
            }
        }

        if (response.getCandidateGroups() != null) {
            for (String group : response.getCandidateGroups()) {
                task.addCandidateGroup(group);
            }
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
        private Map<String, Object> processVariables;
    }

    @Data
    public static class TaskCreatedResponse {
        private UUID memoTaskId;
        private String assignee;
        private List<String> candidateGroups;
        private List<String> candidateUsers;
    }
}
