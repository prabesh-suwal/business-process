package com.enterprise.policyengine.service;

import com.enterprise.policyengine.dto.PolicyRequest;
import com.enterprise.policyengine.dto.PolicyResponse;
import com.enterprise.policyengine.entity.*;
import com.enterprise.policyengine.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing policies (CRUD operations).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public List<PolicyResponse> findAll() {
        return policyRepository.findAllActiveWithRules().stream()
                .map(PolicyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PolicyResponse findById(UUID id) {
        Policy policy = policyRepository.findByIdWithRules(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + id));
        return PolicyResponse.fromEntity(policy);
    }

    @Transactional(readOnly = true)
    public PolicyResponse findByName(String name) {
        Policy policy = policyRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + name));
        return PolicyResponse.fromEntity(policy);
    }

    @Transactional
    public PolicyResponse create(PolicyRequest request, UUID createdBy) {
        if (policyRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Policy with name already exists: " + request.getName());
        }

        Policy policy = Policy.builder()
                .name(request.getName())
                .description(request.getDescription())
                .resourceType(request.getResourceType())
                .action(request.getAction())
                .effect(request.getEffect())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .products(request.getProducts() != null ? request.getProducts() : new HashSet<>())
                .isActive(true)
                .version(1)
                .createdBy(createdBy)
                .build();

        // Add rules
        if (request.getRules() != null) {
            for (PolicyRequest.RuleRequest ruleReq : request.getRules()) {
                PolicyRule rule = PolicyRule.builder()
                        .ruleGroup(ruleReq.getRuleGroup() != null ? ruleReq.getRuleGroup() : "default")
                        .attribute(ruleReq.getAttribute())
                        .operator(ruleReq.getOperator())
                        .valueType(ruleReq.getValueType())
                        .value(ruleReq.getValue() != null ? ruleReq.getValue() : "")
                        .description(ruleReq.getDescription())
                        .sortOrder(ruleReq.getSortOrder() != null ? ruleReq.getSortOrder() : 0)
                        .temporalCondition(ruleReq.getTemporalCondition() != null ? ruleReq.getTemporalCondition()
                                : com.enterprise.policyengine.entity.TemporalCondition.NONE)
                        .timeFrom(ruleReq.getTimeFrom())
                        .timeTo(ruleReq.getTimeTo())
                        .validFrom(ruleReq.getValidFrom())
                        .validUntil(ruleReq.getValidUntil())
                        .timezone(ruleReq.getTimezone())
                        .build();
                policy.addRule(rule);
            }
        }

        // Add rule groups
        if (request.getRuleGroups() != null) {
            for (PolicyRequest.RuleGroupRequest groupReq : request.getRuleGroups()) {
                PolicyRuleGroup group = PolicyRuleGroup.builder()
                        .name(groupReq.getName())
                        .logicOperator(groupReq.getLogicOperator() != null
                                ? LogicOperator.valueOf(groupReq.getLogicOperator().toUpperCase())
                                : LogicOperator.AND)
                        .sortOrder(groupReq.getSortOrder() != null ? groupReq.getSortOrder() : 0)
                        .build();
                policy.addRuleGroup(group);
            }
        }

        Policy saved = policyRepository.save(policy);
        log.info("Created policy: {} (id={})", saved.getName(), saved.getId());

        return PolicyResponse.fromEntity(saved);
    }

    @Transactional
    public PolicyResponse update(UUID id, PolicyRequest request, UUID updatedBy) {
        Policy policy = policyRepository.findByIdWithRules(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + id));

        // Check if name is being changed to an existing name
        if (!policy.getName().equals(request.getName())
                && policyRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Policy with name already exists: " + request.getName());
        }

        // Update basic fields
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setResourceType(request.getResourceType());
        policy.setAction(request.getAction());
        policy.setEffect(request.getEffect() != null ? request.getEffect() : PolicyEffect.ALLOW);
        policy.setPriority(request.getPriority() != null ? request.getPriority() : 0);

        if (request.getProducts() != null) {
            policy.setProducts(request.getProducts());
        } else {
            policy.getProducts().clear();
        }

        // Update Rules
        policy.getRules().clear();
        if (request.getRules() != null) {
            for (PolicyRequest.RuleRequest ruleReq : request.getRules()) {
                PolicyRule rule = PolicyRule.builder()
                        .ruleGroup(ruleReq.getRuleGroup() != null ? ruleReq.getRuleGroup() : "default")
                        .attribute(ruleReq.getAttribute())
                        .operator(ruleReq.getOperator())
                        .valueType(ruleReq.getValueType())
                        .value(ruleReq.getValue() != null ? ruleReq.getValue() : "")
                        .description(ruleReq.getDescription())
                        .sortOrder(ruleReq.getSortOrder() != null ? ruleReq.getSortOrder() : 0)
                        .temporalCondition(ruleReq.getTemporalCondition() != null ? ruleReq.getTemporalCondition()
                                : com.enterprise.policyengine.entity.TemporalCondition.NONE)
                        .timeFrom(ruleReq.getTimeFrom())
                        .timeTo(ruleReq.getTimeTo())
                        .validFrom(ruleReq.getValidFrom())
                        .validUntil(ruleReq.getValidUntil())
                        .timezone(ruleReq.getTimezone())
                        .build();
                policy.addRule(rule);
            }
        }

        // Replace rule groups
        policy.getRuleGroups().clear();
        if (request.getRuleGroups() != null) {
            for (PolicyRequest.RuleGroupRequest groupReq : request.getRuleGroups()) {
                PolicyRuleGroup group = PolicyRuleGroup.builder()
                        .name(groupReq.getName())
                        .logicOperator(groupReq.getLogicOperator() != null
                                ? LogicOperator.valueOf(groupReq.getLogicOperator().toUpperCase())
                                : LogicOperator.AND)
                        .sortOrder(groupReq.getSortOrder() != null ? groupReq.getSortOrder() : 0)
                        .build();
                policy.addRuleGroup(group);
            }
        }

        Policy saved = policyRepository.save(policy);
        log.info("Updated policy: {} (version={})", saved.getName(), saved.getVersion());

        return PolicyResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + id));

        policyRepository.delete(policy);
        log.info("Deleted policy: {} (id={})", policy.getName(), id);
    }

    @Transactional
    public PolicyResponse activate(UUID id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + id));

        policy.setActive(true);
        Policy saved = policyRepository.save(policy);
        log.info("Activated policy: {}", saved.getName());

        return PolicyResponse.fromEntity(saved);
    }

    @Transactional
    public PolicyResponse deactivate(UUID id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + id));

        policy.setActive(false);
        Policy saved = policyRepository.save(policy);
        log.info("Deactivated policy: {}", saved.getName());

        return PolicyResponse.fromEntity(saved);
    }
}
