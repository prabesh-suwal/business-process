package com.enterprise.memo.service;

import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.entity.WorkflowGatewayConfig;
import com.enterprise.memo.repository.MemoTopicRepository;
import com.enterprise.memo.repository.WorkflowGatewayConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing parallel/inclusive gateway configurations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GatewayConfigService {

    private final WorkflowGatewayConfigRepository gatewayConfigRepository;
    private final MemoTopicRepository topicRepository;

    /**
     * Get all gateway configs for a topic.
     */
    @Transactional(readOnly = true)
    public List<WorkflowGatewayConfig> getConfigsForTopic(UUID topicId) {
        return gatewayConfigRepository.findByTopicId(topicId);
    }

    /**
     * Get configs as a map for easy lookup by gateway ID.
     */
    @Transactional(readOnly = true)
    public Map<String, WorkflowGatewayConfig> getConfigMap(UUID topicId) {
        return gatewayConfigRepository.findByTopicId(topicId).stream()
                .collect(Collectors.toMap(
                        WorkflowGatewayConfig::getGatewayId,
                        config -> config,
                        (a, b) -> b // Keep latest if duplicate
                ));
    }

    /**
     * Get a specific gateway config.
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowGatewayConfig> getConfig(UUID topicId, String gatewayId) {
        return gatewayConfigRepository.findByTopicIdAndGatewayId(topicId, gatewayId);
    }

    /**
     * Save or update a gateway configuration.
     */
    public WorkflowGatewayConfig saveConfig(UUID topicId, WorkflowGatewayConfig config) {
        MemoTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

        // Check if config already exists
        Optional<WorkflowGatewayConfig> existing = gatewayConfigRepository
                .findByTopicIdAndGatewayId(topicId, config.getGatewayId());

        if (existing.isPresent()) {
            // Update existing
            WorkflowGatewayConfig toUpdate = existing.get();
            toUpdate.setGatewayName(config.getGatewayName());
            toUpdate.setGatewayType(config.getGatewayType());
            toUpdate.setCompletionMode(config.getCompletionMode());
            toUpdate.setMinimumRequired(config.getMinimumRequired());   
            toUpdate.setTotalIncomingFlows(config.getTotalIncomingFlows());
            toUpdate.setDescription(config.getDescription());
            toUpdate.setCancelRemaining(config.getCancelRemaining());
            log.info("Updated gateway config for {} in topic {}", config.getGatewayId(), topicId);
            return gatewayConfigRepository.save(toUpdate);
        } else {
            // Create new
            config.setTopic(topic);
            log.info("Created gateway config for {} in topic {}", config.getGatewayId(), topicId);
            return gatewayConfigRepository.save(config);
        }
    }

    /**
     * Bulk save gateway configurations (used by designer).
     */
    public List<WorkflowGatewayConfig> saveConfigs(UUID topicId, List<WorkflowGatewayConfig> configs) {
        return configs.stream()
                .map(config -> saveConfig(topicId, config))
                .collect(Collectors.toList());
    }

    /**
     * Delete a gateway config.
     */
    public void deleteConfig(UUID topicId, String gatewayId) {
        gatewayConfigRepository.findByTopicIdAndGatewayId(topicId, gatewayId)
                .ifPresent(config -> {
                    gatewayConfigRepository.delete(config);
                    log.info("Deleted gateway config for {} in topic {}", gatewayId, topicId);
                });
    }

    /**
     * Get all configs that need completion conditions injected.
     * (i.e., non-ALL completion modes)
     */
    @Transactional(readOnly = true)
    public List<WorkflowGatewayConfig> getConfigsRequiringCompletionCondition(UUID topicId) {
        return gatewayConfigRepository.findByTopicIdAndCompletionModeNot(
                topicId,
                WorkflowGatewayConfig.CompletionMode.ALL);
    }

    /**
     * Create default configs for gateways detected in BPMN
     * (when no config exists yet).
     */
    public WorkflowGatewayConfig createDefaultConfig(
            UUID topicId,
            String gatewayId,
            String gatewayName,
            WorkflowGatewayConfig.GatewayType type,
            int incomingFlows) {

        // Check if already exists
        Optional<WorkflowGatewayConfig> existing = getConfig(topicId, gatewayId);
        if (existing.isPresent()) {
            return existing.get();
        }

        MemoTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

        WorkflowGatewayConfig config = WorkflowGatewayConfig.builder()
                .topic(topic)
                .gatewayId(gatewayId)
                .gatewayName(gatewayName)
                .gatewayType(type)
                .completionMode(WorkflowGatewayConfig.CompletionMode.ALL)
                .minimumRequired(1)
                .totalIncomingFlows(incomingFlows)
                .cancelRemaining(true)
                .build();

        return gatewayConfigRepository.save(config);
    }
}
