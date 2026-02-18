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
         * 
         * @param cancelOthers If true, other parallel tasks will be cancelled (for
         *                     "first approval wins" mode)
         * @param gatewayId    Optional. If provided with cancelOthers=true, only
         *                     cancels tasks
         *                     within this gateway's scope (for nested gateway support)
         */
        public void completeTask(String taskId, String userId, String userName, Map<String, Object> variables,
                        boolean cancelOthers, String gatewayId) {
                log.info("Completing task {} with variables for user {}, cancelOthers={}, gatewayId={}",
                                taskId, userId, cancelOthers, gatewayId);

                // Build CompleteTaskRequest DTO structure expected by workflow-service
                Map<String, Object> requestBody = new java.util.HashMap<>();
                requestBody.put("variables", variables != null ? variables : Map.of());
                requestBody.put("comment", variables != null ? variables.get("comment") : null);
                // Derive approved flag from the decision/action variable
                String decision = variables != null ? String.valueOf(variables.getOrDefault("decision", "")) : "";
                requestBody.put("approved", "APPROVE".equalsIgnoreCase(decision));

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
         * Get task inbox for a user (assigned + claimable tasks).
         * Headers are automatically propagated by UserContextWebClientFilter.
         */
        public java.util.List<Map<String, Object>> getTaskInbox() {
                log.debug("Getting task inbox via auto-propagated UserContext headers");

                return webClientBuilder.build()
                                .get()
                                .uri(workflowServiceUrl + "/api/tasks/inbox")
                                .retrieve()
                                .bodyToMono(
                                                new org.springframework.core.ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {
                                                })
                                .block();
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
}
