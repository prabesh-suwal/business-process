package com.enterprise.policyengine.service;

import com.cas.common.policy.PolicyEvaluationRequest;
import com.cas.common.policy.PolicyEvaluationResponse;
import com.enterprise.policyengine.engine.PolicyEvaluator;
import com.enterprise.policyengine.entity.EvaluationAuditLog;
import com.enterprise.policyengine.repository.EvaluationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for evaluating authorization requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final PolicyEvaluator policyEvaluator;
    private final EvaluationAuditLogRepository auditLogRepository;

    @Value("${policy-engine.evaluation.audit-enabled:true}")
    private boolean auditEnabled;

    /**
     * Evaluate an authorization request.
     */
    @Transactional
    public PolicyEvaluationResponse evaluate(PolicyEvaluationRequest request) {
        log.debug("Evaluating authorization: subject={}, action={}, resource={}",
                request.getSubject().getUserId(),
                request.getAction().getName(),
                request.getResource().getType());

        // Perform evaluation
        PolicyEvaluationResponse response = policyEvaluator.evaluate(request);

        // Log the decision
        if (auditEnabled) {
            saveAuditLog(request, response);
        }

        log.info("Authorization decision: allowed={}, reason='{}', time={}ms",
                response.isAllowed(),
                response.getReason(),
                response.getEvaluationTimeMs());

        return response;
    }

    /**
     * Evaluate without audit logging (for testing/dry-run).
     */
    public PolicyEvaluationResponse evaluateDryRun(PolicyEvaluationRequest request) {
        log.debug("Dry-run evaluation: subject={}, action={}",
                request.getSubject().getUserId(), request.getAction().getName());
        return policyEvaluator.evaluate(request);
    }

    /**
     * Save an audit log entry.
     */
    private void saveAuditLog(PolicyEvaluationRequest request, PolicyEvaluationResponse response) {
        try {
            Map<String, Object> context = new HashMap<>();
            if (request.getEnvironment() != null) {
                context.putAll(request.getEnvironment());
            }
            context.put("resourceType", request.getResource().getType());
            context.put("action", request.getAction().getName());

            EvaluationAuditLog auditLog = EvaluationAuditLog.builder()
                    .policyId(response.getMatchedPolicyId())
                    .policyName(response.getMatchedPolicy())
                    .subjectId(request.getSubject().getUserId())
                    .resourceType(request.getResource().getType())
                    .resourceId(request.getResource().getId())
                    .action(request.getAction().getName())
                    .decision(response.isAllowed() ? "ALLOW" : "DENY")
                    .reason(response.getReason())
                    .evaluationTimeMs((int) response.getEvaluationTimeMs())
                    .requestContext(context)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
            // Don't fail the request if audit logging fails
        }
    }
}
