package com.enterprise.memo.controller;

import com.enterprise.memo.dto.GatewayConfigDTO;
import com.enterprise.memo.entity.WorkflowGatewayConfig;
import com.enterprise.memo.service.GatewayConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API for managing parallel/inclusive gateway configurations.
 * Used by the workflow designer to configure how parallel branches complete.
 */
@RestController
@RequestMapping("/api/topics/{topicId}/gateways")
@RequiredArgsConstructor
@Slf4j
public class GatewayConfigController {

    private final GatewayConfigService gatewayConfigService;

    /**
     * Get all gateway configs for a topic.
     */
    @GetMapping
    public ResponseEntity<List<GatewayConfigDTO>> getConfigs(@PathVariable UUID topicId) {
        List<GatewayConfigDTO> configs = gatewayConfigService.getConfigsForTopic(topicId)
                .stream()
                .map(GatewayConfigDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(configs);
    }

    /**
     * Get a specific gateway config.
     */
    @GetMapping("/{gatewayId}")
    public ResponseEntity<GatewayConfigDTO> getConfig(
            @PathVariable UUID topicId,
            @PathVariable String gatewayId) {
        return gatewayConfigService.getConfig(topicId, gatewayId)
                .map(GatewayConfigDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Save or update a gateway config.
     */
    @PutMapping("/{gatewayId}")
    public ResponseEntity<GatewayConfigDTO> saveConfig(
            @PathVariable UUID topicId,
            @PathVariable String gatewayId,
            @RequestBody GatewayConfigDTO dto) {

        // Ensure gatewayId matches path
        dto.setGatewayId(gatewayId);

        log.info("Saving gateway config for {} in topic {}: mode={}",
                gatewayId, topicId, dto.getCompletionMode());

        WorkflowGatewayConfig saved = gatewayConfigService.saveConfig(topicId, dto.toEntity());
        return ResponseEntity.ok(GatewayConfigDTO.fromEntity(saved));
    }

    /**
     * Bulk save gateway configs (used when saving whole workflow).
     */
    @PutMapping
    public ResponseEntity<List<GatewayConfigDTO>> saveConfigs(
            @PathVariable UUID topicId,
            @RequestBody List<GatewayConfigDTO> dtos) {

        log.info("Bulk saving {} gateway configs for topic {}", dtos.size(), topicId);

        List<WorkflowGatewayConfig> toSave = dtos.stream()
                .map(GatewayConfigDTO::toEntity)
                .collect(Collectors.toList());

        List<GatewayConfigDTO> saved = gatewayConfigService.saveConfigs(topicId, toSave)
                .stream()
                .map(GatewayConfigDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(saved);
    }

    /**
     * Delete a gateway config.
     */
    @DeleteMapping("/{gatewayId}")
    public ResponseEntity<Void> deleteConfig(
            @PathVariable UUID topicId,
            @PathVariable String gatewayId) {

        log.info("Deleting gateway config for {} in topic {}", gatewayId, topicId);
        gatewayConfigService.deleteConfig(topicId, gatewayId);
        return ResponseEntity.noContent().build();
    }
}
