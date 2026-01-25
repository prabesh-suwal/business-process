package com.enterprise.memo.client;

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

        private final WebClient.Builder webClientBuilder;

        @Value("${workflow.service.url:http://localhost:9002}")
        private String workflowServiceUrl;

        /**
         * Start a workflow process.
         */
        public String startProcess(StartProcessRequest request, UUID userId) {
                log.info("Starting workflow process for businessKey: {}", request.getBusinessKey());

                return webClientBuilder.build()
                                .post()
                                .uri(workflowServiceUrl + "/api/process-instances")
                                .header("X-User-Id", userId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();
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
         */
        public void completeTask(String taskId, String userId, String userName, Map<String, Object> variables) {
                log.info("Completing task {} with variables for user {}", taskId, userId);

                // Build CompleteTaskRequest DTO structure expected by workflow-service
                Map<String, Object> requestBody = new java.util.HashMap<>();
                requestBody.put("variables", variables != null ? variables : Map.of());
                requestBody.put("comment", variables != null ? variables.get("comment") : null);
                requestBody.put("approved", true); // Default to true for now

                WebClient.RequestHeadersSpec<?> spec = webClientBuilder.build()
                                .post()
                                .uri(workflowServiceUrl + "/api/tasks/" + taskId + "/complete")
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
        public BpmnDeployResult deployBpmn(String processKey, String processName, String bpmnXml) {
                log.info("Deploying BPMN for process: {} ({})", processName, processKey);

                Map<String, Object> request = Map.of(
                                "processKey", processKey,
                                "processName", processName,
                                "bpmnXml", bpmnXml);

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
         * Get task inbox for a user (assigned + claimable tasks).
         * Forwards user roles header to workflow-service.
         */
        public java.util.List<Map<String, Object>> getTaskInbox(String userId, String roles) {
                log.debug("Getting task inbox for user: {} with roles: {}", userId, roles);

                WebClient.RequestHeadersSpec<?> spec = webClientBuilder.build()
                                .get()
                                .uri(workflowServiceUrl + "/api/tasks/inbox")
                                .header("X-User-Id", userId);

                if (roles != null && !roles.isBlank()) {
                        spec = spec.header("X-User-Roles", roles);
                }

                return spec.retrieve()
                                .bodyToMono(
                                                new org.springframework.core.ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {
                                                })
                                .block();
        }
}
