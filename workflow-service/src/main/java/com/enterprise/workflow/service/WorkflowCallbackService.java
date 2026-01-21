package com.enterprise.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending workflow status callbacks to product services (e.g.,
 * LMS).
 * Called when task status changes in Flowable.
 */
@Slf4j
@Service
public class WorkflowCallbackService {

    private final RestTemplate restTemplate;

    @Autowired
    public WorkflowCallbackService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Notify the product service when a task is created/assigned.
     */
    @Async
    public void notifyTaskCreated(String callbackUrl, TaskStatusEvent event) {
        sendCallback(callbackUrl, "TASK_CREATED", event);
    }

    /**
     * Notify the product service when a task is completed.
     */
    @Async
    public void notifyTaskCompleted(String callbackUrl, TaskStatusEvent event) {
        sendCallback(callbackUrl, "TASK_COMPLETED", event);
    }

    /**
     * Notify the product service when a task is claimed.
     */
    @Async
    public void notifyTaskClaimed(String callbackUrl, TaskStatusEvent event) {
        sendCallback(callbackUrl, "TASK_CLAIMED", event);
    }

    /**
     * Notify the product service when a process is completed.
     */
    @Async
    public void notifyProcessCompleted(String callbackUrl, ProcessStatusEvent event) {
        sendProcessCallback(callbackUrl, "PROCESS_COMPLETED", event);
    }

    /**
     * Notify the product service when SLA is breached.
     */
    @Async
    public void notifySlaBreached(String callbackUrl, TaskStatusEvent event) {
        sendCallback(callbackUrl, "SLA_BREACHED", event);
    }

    private void sendCallback(String callbackUrl, String eventType, TaskStatusEvent event) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            log.debug("No callback URL configured, skipping {} notification", eventType);
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", eventType);
            payload.put("timestamp", LocalDateTime.now().toString());
            payload.put("processInstanceId", event.getProcessInstanceId());
            payload.put("businessKey", event.getBusinessKey() != null ? event.getBusinessKey() : "");
            payload.put("taskId", event.getTaskId() != null ? event.getTaskId() : "");
            payload.put("taskKey", event.getTaskKey() != null ? event.getTaskKey() : "");
            payload.put("taskName", event.getTaskName() != null ? event.getTaskName() : "");
            payload.put("assignee", event.getAssignee() != null ? event.getAssignee() : "");
            payload.put("outcome", event.getOutcome() != null ? event.getOutcome() : "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(callbackUrl, request, Void.class);
            log.info("Callback {} sent successfully to {}", eventType, callbackUrl);
        } catch (Exception e) {
            log.error("Failed to send callback {} to {}: {}", eventType, callbackUrl, e.getMessage());
        }
    }

    private void sendProcessCallback(String callbackUrl, String eventType, ProcessStatusEvent event) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            log.debug("No callback URL configured, skipping {} notification", eventType);
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", eventType);
            payload.put("timestamp", LocalDateTime.now().toString());
            payload.put("processInstanceId", event.getProcessInstanceId());
            payload.put("businessKey", event.getBusinessKey() != null ? event.getBusinessKey() : "");
            payload.put("processDefinitionKey",
                    event.getProcessDefinitionKey() != null ? event.getProcessDefinitionKey() : "");
            payload.put("endReason", event.getEndReason() != null ? event.getEndReason() : "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(callbackUrl, request, Void.class);
            log.info("Callback {} sent successfully to {}", eventType, callbackUrl);
        } catch (Exception e) {
            log.error("Failed to send callback {} to {}: {}", eventType, callbackUrl, e.getMessage());
        }
    }

    // === Event DTOs ===

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaskStatusEvent {
        private String processInstanceId;
        private String businessKey;
        private String taskId;
        private String taskKey;
        private String taskName;
        private String assignee;
        private String outcome;
        private Map<String, Object> variables;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProcessStatusEvent {
        private String processInstanceId;
        private String businessKey;
        private String processDefinitionKey;
        private String endReason;
    }
}
