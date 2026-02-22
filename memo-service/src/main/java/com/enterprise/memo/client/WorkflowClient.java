package com.enterprise.memo.client;

import com.cas.common.webclient.InternalWebClient;
import com.enterprise.memo.dto.StartProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

/**
 * Client for calling workflow-service.
 * Workflow-service should only handle Flowable operations, not business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowClient {

        @InternalWebClient
        private final WebClient.Builder webClientBuilder;

        @Value("${services.workflow-service.url:http://localhost:9002}")
        private String workflowServiceUrl;

        /**
         * Start a workflow process. Returns the Flowable process instance ID.
         */
        @SuppressWarnings("unchecked")
        public String startProcess(StartProcessRequest request, UUID userId) {
                log.info("Starting workflow process for businessKey: {}", request.getBusinessKey());

                Map<String, Object> response = webClientBuilder.build()
                                .post()
                                .uri(workflowServiceUrl + "/api/process-instances")
                                .header("X-User-Id", userId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();

                if (response != null && response.get("flowableProcessInstanceId") != null) {
                        String processInstanceId = response.get("flowableProcessInstanceId").toString();
                        log.info("Process started, flowableProcessInstanceId: {}", processInstanceId);
                        return processInstanceId;
                }
                log.warn("Process started but no flowableProcessInstanceId in response: {}", response);
                return null;
        }

        /**
         * Get execution timeline for a process instance from workflow-service.
         */
        @SuppressWarnings("unchecked")
        public java.util.List<Map<String, Object>> getTimeline(String processInstanceId) {
                log.debug("Getting timeline for processInstanceId: {}", processInstanceId);
                try {
                        return webClientBuilder.build()
                                        .get()
                                        .uri(workflowServiceUrl + "/api/history/timeline/" + processInstanceId)
                                        .retrieve()
                                        .bodyToMono(java.util.List.class)
                                        .block();
                } catch (Exception e) {
                        log.error("Failed to get timeline for {}: {}", processInstanceId, e.getMessage());
                        return java.util.Collections.emptyList();
                }
        }

        /**
         * Claim a task.
         */
        public void claimTask(String taskId, String userId, String userName) {
                log.info("Claiming task {} for user {}", taskId, userId);

                WebClient.RequestHeadersSpec<?> spec = webClientBuilder.build()
                                .post()
                                .uri(workflowServiceUrl + "/api/tasks/" + taskId + "/claim")
                                .header("X-User-Id", userId);

                if (userName != null && !userName.isBlank()) {
                        spec = spec.header("X-User-Name", userName);
                }

                spec.retrieve()
                                .bodyToMono(Void.class)
                                .block();
        }

        /**
         * Complete a task with variables.
         * 
         * @param actionLabel  The original button label (e.g. "Reject"), used by
         *                     workflow-service for action validation
         * @param cancelOthers If true, other parallel tasks will be cancelled (for
         *                     "first approval wins" mode)
         * @param gatewayId    Optional. If provided with cancelOthers=true, only
         *                     cancels tasks
         *                     within this gateway's scope (for nested gateway support)
         */
        public void completeTask(String taskId, String userId, String userName, Map<String, Object> variables,
                        String actionLabel, boolean cancelOthers, String gatewayId) {
                log.info("Completing task {} with variables for user {}, action={}, cancelOthers={}, gatewayId={}",
                                taskId, userId, actionLabel, cancelOthers, gatewayId);

                // Build CompleteTaskRequest DTO structure expected by workflow-service
                Map<String, Object> requestBody = new java.util.HashMap<>();
                requestBody.put("variables", variables != null ? variables : Map.of());
                requestBody.put("comment", variables != null ? variables.get("comment") : null);
                // Derive approved flag from the decision/action variable
                String decision = variables != null ? String.valueOf(variables.getOrDefault("decision", "")) : "";
                requestBody.put("approved", "APPROVE".equalsIgnoreCase(decision)
                                || "APPROVED".equalsIgnoreCase(decision));
                // Pass the original button label for backend validation & action type
                // resolution
                requestBody.put("action", actionLabel != null ? actionLabel : decision);

                StringBuilder urlBuilder = new StringBuilder(workflowServiceUrl + "/api/tasks/" + taskId + "/complete");

                if (cancelOthers) {
                        urlBuilder.append("?cancelOthers=true");
                        if (gatewayId != null && !gatewayId.isEmpty()) {
                                urlBuilder.append("&gatewayId=").append(gatewayId);
                        }
                }

                String url = urlBuilder.toString();

                WebClient.RequestHeadersSpec<?> spec = webClientBuilder.build()
                                .post()
                                .uri(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-User-Id", userId)
                                .bodyValue(requestBody);

                if (userName != null && !userName.isBlank()) {
                        spec = spec.header("X-User-Name", userName);
                }

                spec.retrieve()
                                .bodyToMono(Void.class)
                                .block();
        }

        /**
         * Get task details from workflow service.
         */
        public Map<String, Object> getTask(String taskId) {
                return webClientBuilder.build()
                                .get()
                                .uri(workflowServiceUrl + "/api/tasks/" + taskId)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();
        }

        /**
         * Assign task to user/groups.
         */
        public void assignTask(String taskId, String assignee, String candidateGroups) {
                log.info("Assigning task {} to {} (groups: {})", taskId, assignee, candidateGroups);

                Map<String, Object> body = Map.of(
                                "assignee", assignee != null ? assignee : "",
                                "candidateGroups", candidateGroups != null ? candidateGroups : "");

                webClientBuilder.build()
                                .post()
                                .uri(workflowServiceUrl + "/api/tasks/" + taskId + "/assign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(body)
                                .retrieve()
                                .bodyToMono(Void.class)
                                .block();
        }

        /**
         * Deploy BPMN XML to Flowable engine.
         * Returns both the Flowable process definition ID and ProcessTemplate UUID.
         */
        public BpmnDeployResult deployBpmn(String processKey, String processName, String bpmnXml, String productId) {
                log.info("Deploying BPMN for process: {} ({}) with productId: {}", processName, processKey, productId);

                Map<String, Object> request = Map.of(
                                "processKey", processKey,
                                "processName", processName,
                                "bpmnXml", bpmnXml,
                                "productId", productId);

                Map<String, Object> response = webClientBuilder.build()
                                .post()
                                .uri(workflowServiceUrl + "/api/process-templates/deploy-bpmn")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();

                if (response != null && response.get("processDefinitionId") != null) {
                        return new BpmnDeployResult(
                                        response.get("processDefinitionId").toString(),
                                        response.get("processTemplateId") != null
                                                        ? response.get("processTemplateId").toString()
                                                        : null);
                }
                throw new RuntimeException("Failed to deploy BPMN - no process definition ID returned");
        }

        /**
         * Result of BPMN deployment.
         */
        public record BpmnDeployResult(
                        String processDefinitionId, // Flowable ID for starting instances
                        String processTemplateId // Our UUID for config lookup
        ) {
        }

        /**
         * Get task inbox for a user (assigned + claimable tasks) with pagination.
         * Headers are automatically propagated by UserContextWebClientFilter.
         */
        public Map<String, Object> getTaskInbox(int page, int size, String sortBy, String sortDir,
                        String priority, String search) {
                log.debug("Getting task inbox via auto-propagated UserContext headers (page={}, size={})", page, size);

                return webClientBuilder.build()
                                .get()
                                .uri(workflowServiceUrl + "/api/tasks/inbox", uriBuilder -> uriBuilder
                                                .queryParam("page", page)
                                                .queryParam("size", size)
                                                .queryParam("sortBy", sortBy)
                                                .queryParam("sortDir", sortDir)
                                                .queryParamIfPresent("priority",
                                                                java.util.Optional.ofNullable(priority)
                                                                                .filter(s -> !s.isBlank()))
                                                .queryParamIfPresent("search",
                                                                java.util.Optional.ofNullable(search)
                                                                                .filter(s -> !s.isBlank()))
                                                .build())
                                .retrieve()
                                .bodyToMono(
                                                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                                                })
                                .block();
        }

        /**
         * Get task inbox (no pagination — backward compatible).
         */
        public java.util.List<Map<String, Object>> getTaskInboxAll() {
                log.debug("Getting full task inbox via auto-propagated UserContext headers");

                return webClientBuilder.build()
                                .get()
                                .uri(workflowServiceUrl + "/api/tasks/inbox?size=1000")
                                .retrieve()
                                .bodyToMono(
                                                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                                                })
                                .map(response -> {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> data = (Map<String, Object>) response.get("data");
                                        if (data != null && data.get("content") != null) {
                                                @SuppressWarnings("unchecked")
                                                java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) data
                                                                .get("content");
                                                return content;
                                        }
                                        return java.util.Collections.<Map<String, Object>>emptyList();
                                })
                                .block();
        }

        /**
         * Get all task configurations for a process template.
         * Used during version snapshotting to capture outcome configs alongside step
         * configs.
         */
        @SuppressWarnings("unchecked")
        public java.util.List<Map<String, Object>> getTaskConfigsForTemplate(String processTemplateId) {
                log.debug("Getting task configs for process template: {}", processTemplateId);
                try {
                        return webClientBuilder.build()
                                        .get()
                                        .uri(workflowServiceUrl + "/api/process-templates/" + processTemplateId
                                                        + "/task-configs")
                                        .retrieve()
                                        .bodyToMono(java.util.List.class)
                                        .block();
                } catch (Exception e) {
                        log.debug("No task configs for template {}: {}", processTemplateId, e.getMessage());
                        return java.util.Collections.emptyList();
                }
        }

        /**
         * Get outcome configuration for a task.
         * Returns the configured variable name and options for task completion.
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getOutcomeConfig(String taskId) {
                log.debug("Getting outcome config for task: {}", taskId);

                try {
                        return webClientBuilder.build()
                                        .get()
                                        .uri(workflowServiceUrl + "/api/tasks/" + taskId + "/outcome-config")
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .block();
                } catch (Exception e) {
                        log.debug("No outcome config for task {}: {}", taskId, e.getMessage());
                        return null;
                }
        }

        /**
         * Get movement history for a task.
         * Returns the ordered history of completed steps and valid return points,
         * built from ActionTimeline events instead of static BPMN paths.
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getMovementHistory(String taskId) {
                log.debug("Getting movement history for task: {}", taskId);

                try {
                        return webClientBuilder.build()
                                        .get()
                                        .uri(workflowServiceUrl + "/api/tasks/" + taskId + "/movement-history")
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .block();
                } catch (Exception e) {
                        log.debug("No movement history for task {}: {}", taskId, e.getMessage());
                        return null;
                }
        }

        /**
         * Get return points for a task (valid send-back targets based on movement
         * history).
         */
        @SuppressWarnings("unchecked")
        public java.util.List<Map<String, Object>> getReturnPoints(String taskId) {
                log.debug("Getting return points for task: {}", taskId);
                try {
                        return webClientBuilder.build()
                                        .get()
                                        .uri(workflowServiceUrl + "/api/tasks/" + taskId + "/return-points")
                                        .retrieve()
                                        .bodyToMono(java.util.List.class)
                                        .block();
                } catch (Exception e) {
                        log.debug("No return points for task {}: {}", taskId, e.getMessage());
                        return java.util.Collections.emptyList();
                }
        }

        /**
         * Send back a task to a previous step.
         */
        public void sendBackTask(String taskId, String targetActivityId, String reason) {
                log.debug("Sending back task {} to {}", taskId, targetActivityId);
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("targetActivityId", targetActivityId);
                body.put("reason", reason);

                webClientBuilder.build()
                                .post()
                                .uri(workflowServiceUrl + "/api/tasks/" + taskId + "/send-back")
                                .bodyValue(body)
                                .retrieve()
                                .bodyToMono(Void.class)
                                .block();
        }

        /**
         * Get delegate candidates for a task.
         * Returns candidate groups and candidate users from the Flowable task.
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getDelegateCandidates(String taskId) {
                log.debug("Getting delegate candidates for task: {}", taskId);
                try {
                        return webClientBuilder.build()
                                        .get()
                                        .uri(workflowServiceUrl + "/api/tasks/" + taskId + "/delegate-candidates")
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .block();
                } catch (Exception e) {
                        log.debug("No delegate candidates for task {}: {}", taskId, e.getMessage());
                        return Map.of("candidateGroups", java.util.Collections.emptyList(),
                                        "candidateUsers", java.util.Collections.emptyList());
                }
        }
}
