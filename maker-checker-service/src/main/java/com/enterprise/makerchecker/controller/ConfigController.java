package com.enterprise.makerchecker.controller;

import com.cas.common.dto.RawResponse;
import com.cas.common.security.UserContextHolder;
import com.enterprise.makerchecker.dto.ConfigRequest;
import com.enterprise.makerchecker.dto.ConfigResponse;
import com.enterprise.makerchecker.dto.CreateApprovalRequest;
import com.enterprise.makerchecker.dto.ApprovalRequestResponse;
import com.enterprise.makerchecker.entity.MakerCheckerConfig;
import com.enterprise.makerchecker.service.ApprovalService;
import com.enterprise.makerchecker.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/maker-checker/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;
    private final ApprovalService approvalService;

    @GetMapping
    public List<ConfigResponse> getAllConfigs(
            @RequestParam(required = false) UUID productId) {
        if (productId != null) {
            return configService.getConfigsByProduct(productId);
        }
        return configService.getAllConfigs();
    }

    @GetMapping("/{id}")
    public ConfigResponse getConfig(@PathVariable UUID id) {
        return configService.getConfig(id);
    }

    @PostMapping
    public ResponseEntity<ConfigResponse> createConfig(@Valid @RequestBody ConfigRequest request) {
        String createdBy = UserContextHolder.getUserId();
        ConfigResponse response = configService.createConfig(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ConfigResponse updateConfig(@PathVariable UUID id, @Valid @RequestBody ConfigRequest request) {
        return configService.updateConfig(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable UUID id) {
        configService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if an endpoint requires maker-checker approval.
     * Called by the gateway filter.
     */
    @RawResponse
    @GetMapping("/check")
    public ResponseEntity<?> checkConfig(
            @RequestParam String httpMethod,
            @RequestParam String requestPath) {
        MakerCheckerConfig config = configService.findMatchingConfig(httpMethod, requestPath);
        if (config == null) {
            return ResponseEntity.ok().body(java.util.Map.of("required", false));
        }
        return ResponseEntity.ok().body(java.util.Map.of(
                "required", true,
                "configId", config.getId(),
                "sameMakerCanCheck", config.isSameMakerCanCheck()));
    }

    /**
     * Create an approval request (called by the gateway internally).
     */
    @RawResponse
    @PostMapping("/approval-requests")
    public ResponseEntity<ApprovalRequestResponse> createApproval(
            @Valid @RequestBody CreateApprovalRequest request) {
        ApprovalRequestResponse response = approvalService.createApproval(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
