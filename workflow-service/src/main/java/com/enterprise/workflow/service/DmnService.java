package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.CreateDecisionTableRequest;
import com.enterprise.workflow.dto.DecisionTableDTO;
import com.enterprise.workflow.dto.EvaluateDecisionResponse;
import com.enterprise.workflow.entity.DecisionTable;
import com.enterprise.workflow.entity.DecisionTable.DecisionTableStatus;
import com.enterprise.workflow.repository.DecisionTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnDecisionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Decision Table lifecycle:
 * create → edit → deploy → evaluate → deprecate → version.
 *
 * Mirrors the ProcessDesignService pattern for consistency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DmnService {

    private final DecisionTableRepository repository;
    private final DmnRepositoryService dmnRepositoryService;
    private final DmnDecisionService dmnDecisionService;

    /**
     * Default DMN XML template for new decision tables.
     */
    private static String generateDefaultDmnXml(String key, String name) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
                             xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/"
                             xmlns:dc="https://www.omg.org/spec/DMN/20191111/DC/"
                             id="Definitions_%s"
                             name="%s"
                             namespace="http://www.flowable.org/dmn">
                  <decision id="%s" name="%s">
                    <decisionTable id="DecisionTable_%s" hitPolicy="FIRST">
                      <input id="input_1" label="decision">
                        <inputExpression id="inputExpression_1" typeRef="string">
                          <text>decision</text>
                        </inputExpression>
                      </input>
                      <output id="output_1" label="routingTarget" name="routingTarget" typeRef="string" />
                      <rule id="rule_1">
                        <inputEntry id="inputEntry_1_1"><text>"APPROVED"</text></inputEntry>
                        <outputEntry id="outputEntry_1_1"><text>"NEXT_STAGE"</text></outputEntry>
                      </rule>
                    </decisionTable>
                  </decision>
                </definitions>
                """.formatted(key, name, key, name, key);
    }

    // === CRUD ===

    @Transactional
    public DecisionTableDTO create(CreateDecisionTableRequest request, UUID createdBy, String createdByName) {
        // Check for duplicate key
        if (repository.existsByKeyAndVersion(request.getKey(), 1)) {
            throw new IllegalArgumentException("Decision table with key '" + request.getKey() + "' already exists");
        }

        String dmnXml = request.getDmnXml() != null && !request.getDmnXml().isBlank()
                ? request.getDmnXml()
                : generateDefaultDmnXml(request.getKey(), request.getName());

        DecisionTable table = DecisionTable.builder()
                .name(request.getName())
                .key(request.getKey())
                .description(request.getDescription())
                .dmnXml(dmnXml)
                .version(1)
                .status(DecisionTableStatus.DRAFT)
                .productId(request.getProductId())
                .productCode(request.getProductCode())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .build();

        table = repository.save(table);
        log.info("Created decision table '{}' (key={}, id={})", table.getName(), table.getKey(), table.getId());
        return toDTO(table);
    }

    @Transactional
    public DecisionTableDTO update(UUID id, String dmnXml, String name, String description) {
        DecisionTable table = findOrThrow(id);
        if (table.getStatus() != DecisionTableStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT decision tables can be updated. Current status: " + table.getStatus());
        }

        if (dmnXml != null)
            table.setDmnXml(dmnXml);
        if (name != null)
            table.setName(name);
        if (description != null)
            table.setDescription(description);

        table = repository.save(table);
        log.info("Updated decision table '{}' (id={})", table.getName(), table.getId());
        return toDTO(table);
    }

    public DecisionTableDTO get(UUID id) {
        return toDTO(findOrThrow(id));
    }

    public DecisionTableDTO getByKey(String key) {
        DecisionTable table = repository.findByKeyAndStatus(key, DecisionTableStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No ACTIVE decision table with key: " + key));
        return toDTO(table);
    }

    public List<DecisionTableDTO> list(UUID productId) {
        List<DecisionTable> tables = productId != null
                ? repository.findByProductId(productId)
                : repository.findAll();
        return tables.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<DecisionTableDTO> listActive(UUID productId) {
        List<DecisionTable> tables = productId != null
                ? repository.findByProductIdAndStatus(productId, DecisionTableStatus.ACTIVE)
                : repository.findByStatus(DecisionTableStatus.ACTIVE);
        return tables.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID id) {
        DecisionTable table = findOrThrow(id);
        if (table.getStatus() != DecisionTableStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT decision tables can be deleted");
        }
        repository.delete(table);
        log.info("Deleted decision table '{}' (id={})", table.getName(), table.getId());
    }

    // === DEPLOYMENT ===

    @Transactional
    public DecisionTableDTO deploy(UUID id) {
        DecisionTable table = findOrThrow(id);

        if (table.getDmnXml() == null || table.getDmnXml().isBlank()) {
            throw new IllegalStateException("Cannot deploy decision table without DMN XML");
        }

        // Deploy to Flowable DMN engine
        DmnDeployment deployment = dmnRepositoryService.createDeployment()
                .name(table.getName() + " v" + table.getVersion())
                .addInputStream(
                        table.getKey() + ".dmn",
                        new ByteArrayInputStream(table.getDmnXml().getBytes(StandardCharsets.UTF_8)))
                .deploy();

        // Get the deployed decision definition
        DmnDecision decisionDef = dmnRepositoryService.createDecisionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        // Update entity
        table.setFlowableDeploymentId(deployment.getId());
        table.setFlowableDecisionKey(decisionDef != null ? decisionDef.getKey() : table.getKey());
        table.setStatus(DecisionTableStatus.ACTIVE);
        table.setEffectiveFrom(LocalDateTime.now());

        table = repository.save(table);
        log.info("Deployed decision table '{}' to Flowable DMN engine. Deployment ID: {}, Decision Key: {}",
                table.getName(), deployment.getId(), table.getFlowableDecisionKey());

        return toDTO(table);
    }

    // === EVALUATION ===

    /**
     * Test-evaluate a deployed decision table with given variables.
     * Uses Flowable's DmnRuleService to execute the decision.
     */
    public EvaluateDecisionResponse evaluate(String decisionKey, Map<String, Object> variables) {
        log.info("Evaluating decision table '{}' with variables: {}", decisionKey, variables);

        List<Map<String, Object>> results = dmnDecisionService
                .createExecuteDecisionBuilder()
                .decisionKey(decisionKey)
                .variables(variables)
                .execute();

        String hitPolicy = "FIRST"; // default hit policy

        log.info("Decision '{}' evaluated: {} result(s), hitPolicy={}", decisionKey, results.size(), hitPolicy);

        return EvaluateDecisionResponse.builder()
                .decisionKey(decisionKey)
                .results(results)
                .hitPolicy(hitPolicy)
                .build();
    }

    // === VERSIONING ===

    @Transactional
    public DecisionTableDTO deprecate(UUID id) {
        DecisionTable table = findOrThrow(id);
        if (table.getStatus() != DecisionTableStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE decision tables can be deprecated");
        }
        table.setStatus(DecisionTableStatus.DEPRECATED);
        table.setEffectiveTo(LocalDateTime.now());
        table = repository.save(table);
        log.info("Deprecated decision table '{}' (id={})", table.getName(), table.getId());
        return toDTO(table);
    }

    @Transactional
    public DecisionTableDTO createNewVersion(UUID sourceId, UUID createdBy, String createdByName) {
        DecisionTable source = findOrThrow(sourceId);

        // Find latest version to determine next version number
        DecisionTable latest = repository.findFirstByKeyOrderByVersionDesc(source.getKey())
                .orElse(source);
        int nextVersion = latest.getVersion() + 1;

        DecisionTable newTable = DecisionTable.builder()
                .name(source.getName())
                .key(source.getKey())
                .description(source.getDescription())
                .dmnXml(source.getDmnXml())
                .version(nextVersion)
                .status(DecisionTableStatus.DRAFT)
                .productId(source.getProductId())
                .productCode(source.getProductCode())
                .previousVersionId(source.getId())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .build();

        newTable = repository.save(newTable);
        log.info("Created new version {} of decision table '{}' (id={})",
                nextVersion, newTable.getName(), newTable.getId());
        return toDTO(newTable);
    }

    // === HELPERS ===

    private DecisionTable findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Decision table not found: " + id));
    }

    private DecisionTableDTO toDTO(DecisionTable table) {
        return DecisionTableDTO.builder()
                .id(table.getId())
                .name(table.getName())
                .key(table.getKey())
                .description(table.getDescription())
                .dmnXml(table.getDmnXml())
                .version(table.getVersion())
                .status(table.getStatus().name())
                .productId(table.getProductId())
                .productCode(table.getProductCode())
                .flowableDeploymentId(table.getFlowableDeploymentId())
                .flowableDecisionKey(table.getFlowableDecisionKey())
                .previousVersionId(table.getPreviousVersionId())
                .effectiveFrom(table.getEffectiveFrom())
                .effectiveTo(table.getEffectiveTo())
                .createdBy(table.getCreatedBy())
                .createdByName(table.getCreatedByName())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .build();
    }
}
