package com.enterprise.policyengine.controller;

import com.cas.common.policy.PolicyEvaluationRequest;
import com.cas.common.policy.PolicyEvaluationResponse;
import com.enterprise.policyengine.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for authorization evaluation.
 * This is the main endpoint called by other services.
 */
@Slf4j
@RestController
@RequestMapping("/evaluate")
@RequiredArgsConstructor
@Tag(name = "Evaluation", description = "Authorization evaluation endpoints")
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * Evaluate an authorization request.
     * 
     * @param request The evaluation request
     * @return Evaluation result with allowed/denied and reason
     */
    @PostMapping
    @Operation(summary = "Evaluate authorization", description = "Evaluate if a subject can perform an action on a resource")
    public ResponseEntity<PolicyEvaluationResponse> evaluate(@Valid @RequestBody PolicyEvaluationRequest request) {
        PolicyEvaluationResponse response = evaluationService.evaluate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Dry-run evaluation (for testing, no audit log).
     */
    @PostMapping("/dry-run")
    @Operation(summary = "Dry-run evaluation", description = "Evaluate without creating audit log entry")
    public ResponseEntity<PolicyEvaluationResponse> evaluateDryRun(
            @Valid @RequestBody PolicyEvaluationRequest request) {
        PolicyEvaluationResponse response = evaluationService.evaluateDryRun(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Batch evaluation for multiple requests.
     */
    @PostMapping("/batch")
    @Operation(summary = "Batch evaluation", description = "Evaluate multiple authorization requests at once")
    public ResponseEntity<List<PolicyEvaluationResponse>> evaluateBatch(
            @Valid @RequestBody List<PolicyEvaluationRequest> requests) {
        List<PolicyEvaluationResponse> responses = requests.stream()
                .map(evaluationService::evaluate)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the evaluation service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
