package com.enterprise.memo.service;

import com.enterprise.memo.entity.GatewayDecisionRule;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.entity.WorkflowStepConfig;
import com.enterprise.memo.repository.GatewayDecisionRuleRepository;
import com.enterprise.memo.repository.MemoTopicRepository;
import com.enterprise.memo.repository.WorkflowStepConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing workflow step configuration.
 * Stores assignment rules, form config, SLA, escalation per workflow step.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowConfigService {

    private final WorkflowStepConfigRepository stepConfigRepository;
    private final GatewayDecisionRuleRepository gatewayRuleRepository;
    private final MemoTopicRepository topicRepository;

    // ==================== Step Config ====================

    public List<WorkflowStepConfig> getStepConfigs(UUID topicId) {
        return stepConfigRepository.findByMemoTopicIdOrderByStepOrder(topicId);
    }

    public Optional<WorkflowStepConfig> getStepConfig(UUID topicId, String taskKey) {
        return stepConfigRepository.findByMemoTopicIdAndTaskKey(topicId, taskKey);
    }

    public WorkflowStepConfig saveStepConfig(UUID topicId, String taskKey,
            String taskName,
            Map<String, Object> assignmentConfig,
            Map<String, Object> formConfig,
            Map<String, Object> slaConfig,
            Map<String, Object> escalationConfig,
            Map<String, Object> viewerConfig,
            Map<String, Object> conditionConfig,
            Map<String, Object> outcomeConfig) {
        MemoTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

        // Reject edit if workflow is already deployed (locked)
        if (topic.isWorkflowDeployed()) {
            throw new IllegalStateException(
                    "Cannot modify deployed workflow. Use 'Copy to New Version' to create an editable copy.");
        }

        WorkflowStepConfig config = stepConfigRepository.findByMemoTopicIdAndTaskKey(topicId, taskKey)
                .orElse(WorkflowStepConfig.builder()
                        .memoTopic(topic)
                        .taskKey(taskKey)
                        .build());

        config.setTaskName(taskName);
        config.setAssignmentConfig(assignmentConfig);
        config.setFormConfig(formConfig);
        config.setSlaConfig(slaConfig);
        config.setEscalationConfig(escalationConfig);
        config.setViewerConfig(viewerConfig);
        config.setConditionConfig(conditionConfig);
        config.setOutcomeConfig(outcomeConfig);

        log.info("Saving step config for topic {} task {}", topicId, taskKey);
        return stepConfigRepository.save(config);
    }

    // ==================== Gateway Rules ====================

    public List<GatewayDecisionRule> getGatewayRules(UUID topicId) {
        return gatewayRuleRepository.findByMemoTopicIdOrderByGatewayKey(topicId);
    }

    public Optional<GatewayDecisionRule> getActiveGatewayRule(UUID topicId, String gatewayKey) {
        return gatewayRuleRepository.findByMemoTopicIdAndGatewayKeyAndActiveTrue(topicId, gatewayKey);
    }

    public GatewayDecisionRule saveGatewayRule(UUID topicId, String gatewayKey,
            String gatewayName,
            List<Map<String, Object>> rules,
            String defaultFlow,
            boolean activate) {
        MemoTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

        // Reject edit if workflow is already deployed (locked)
        if (topic.isWorkflowDeployed()) {
            throw new IllegalStateException(
                    "Cannot modify deployed workflow. Use 'Copy to New Version' to create an editable copy.");
        }

        // Get current max version
        List<GatewayDecisionRule> existing = gatewayRuleRepository
                .findByMemoTopicIdAndGatewayKeyOrderByVersionDesc(topicId, gatewayKey);

        int newVersion = existing.isEmpty() ? 1 : existing.get(0).getVersion() + 1;

        // Deactivate previous version if activating new one
        if (activate && !existing.isEmpty()) {
            existing.forEach(r -> r.setActive(false));
            gatewayRuleRepository.saveAll(existing);
        }

        GatewayDecisionRule rule = GatewayDecisionRule.builder()
                .memoTopic(topic)
                .gatewayKey(gatewayKey)
                .gatewayName(gatewayName)
                .rules(rules)
                .defaultFlow(defaultFlow)
                .version(newVersion)
                .active(activate)
                .activatedAt(activate ? LocalDateTime.now() : null)
                .build();

        log.info("Saving gateway rule for topic {} gateway {} version {}", topicId, gatewayKey, newVersion);
        return gatewayRuleRepository.save(rule);
    }

    public void activateGatewayRule(UUID ruleId, UUID activatedBy) {
        GatewayDecisionRule rule = gatewayRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));

        // Deactivate other versions
        List<GatewayDecisionRule> otherVersions = gatewayRuleRepository
                .findByMemoTopicIdAndGatewayKeyOrderByVersionDesc(
                        rule.getMemoTopic().getId(), rule.getGatewayKey());
        otherVersions.forEach(r -> r.setActive(false));
        gatewayRuleRepository.saveAll(otherVersions);

        // Activate this one
        rule.setActive(true);
        rule.setActivatedAt(LocalDateTime.now());
        rule.setActivatedBy(activatedBy);
        gatewayRuleRepository.save(rule);

        log.info("Activated gateway rule {} version {}", rule.getGatewayKey(), rule.getVersion());
    }

    // ==================== Runtime Resolution ====================

    /**
     * Evaluate gateway rules and return the target flow name.
     */
    public String evaluateGateway(UUID topicId, String gatewayKey, Map<String, Object> memoData) {
        Optional<GatewayDecisionRule> ruleOpt = getActiveGatewayRule(topicId, gatewayKey);

        if (ruleOpt.isEmpty()) {
            log.warn("No active rule for gateway {}, using default", gatewayKey);
            return null;
        }

        GatewayDecisionRule rule = ruleOpt.get();

        for (Map<String, Object> ruleItem : rule.getRules()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) ruleItem.get("conditions");
            String goTo = (String) ruleItem.get("goTo");

            if (evaluateConditions(conditions, memoData)) {
                log.info("Gateway {} matched rule: {}", gatewayKey, goTo);
                return goTo;
            }
        }

        log.info("Gateway {} using default: {}", gatewayKey, rule.getDefaultFlow());
        return rule.getDefaultFlow();
    }

    private boolean evaluateConditions(List<Map<String, Object>> conditions, Map<String, Object> data) {
        for (Map<String, Object> condition : conditions) {
            String field = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object expectedValue = condition.get("value");

            Object actualValue = data.get(field);

            if (!evaluateCondition(actualValue, operator, expectedValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateCondition(Object actual, String operator, Object expected) {
        if (actual == null)
            return false;

        return switch (operator) {
            case "==" -> actual.toString().equals(expected.toString());
            case "!=" -> !actual.toString().equals(expected.toString());
            case ">" -> compareNumbers(actual, expected) > 0;
            case ">=" -> compareNumbers(actual, expected) >= 0;
            case "<" -> compareNumbers(actual, expected) < 0;
            case "<=" -> compareNumbers(actual, expected) <= 0;
            case "contains" -> actual.toString().contains(expected.toString());
            case "startsWith" -> actual.toString().startsWith(expected.toString());
            default -> false;
        };
    }

    private int compareNumbers(Object a, Object b) {
        double d1 = Double.parseDouble(a.toString().replaceAll("[^0-9.-]", ""));
        double d2 = Double.parseDouble(b.toString().replaceAll("[^0-9.-]", ""));
        return Double.compare(d1, d2);
    }
}
