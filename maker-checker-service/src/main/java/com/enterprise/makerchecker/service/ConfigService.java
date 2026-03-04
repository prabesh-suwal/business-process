package com.enterprise.makerchecker.service;

import com.enterprise.makerchecker.dto.ConfigRequest;
import com.enterprise.makerchecker.dto.ConfigResponse;
import com.enterprise.makerchecker.entity.ConfigChecker;
import com.enterprise.makerchecker.entity.MakerCheckerConfig;
import com.enterprise.makerchecker.entity.SlaEscalationConfig;
import com.enterprise.makerchecker.repository.ConfigCheckerRepository;
import com.enterprise.makerchecker.repository.MakerCheckerConfigRepository;
import com.enterprise.makerchecker.repository.SlaEscalationConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final MakerCheckerConfigRepository configRepository;
    private final SlaEscalationConfigRepository slaRepository;
    private final ConfigCheckerRepository checkerRepository;

    public List<ConfigResponse> getAllConfigs() {
        return configRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ConfigResponse> getConfigsByProduct(UUID productId) {
        return configRepository.findByProductId(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ConfigResponse getConfig(UUID id) {
        return toResponse(findConfigOrThrow(id));
    }

    @Transactional
    public ConfigResponse createConfig(ConfigRequest request, String createdBy) {
        if (configRepository.existsByServiceNameAndEndpointPatternAndHttpMethod(
                request.getServiceName(), request.getEndpointPattern(), request.getHttpMethod())) {
            throw new IllegalArgumentException("Config already exists for " +
                    request.getHttpMethod() + " " + request.getEndpointPattern());
        }

        MakerCheckerConfig config = MakerCheckerConfig.builder()
                .productId(request.getProductId())
                .serviceName(request.getServiceName())
                .endpointPattern(request.getEndpointPattern())
                .httpMethod(request.getHttpMethod().toUpperCase())
                .endpointGroup(request.getEndpointGroup())
                .description(request.getDescription())
                .sameMakerCanCheck(Boolean.TRUE.equals(request.getSameMakerCanCheck()))
                .enabled(Boolean.TRUE.equals(request.getEnabled()))
                .createdBy(createdBy)
                .build();

        config = configRepository.save(config);
        log.info("Created maker-checker config: {} {} {} (by {})",
                config.getHttpMethod(), config.getEndpointPattern(), config.getServiceName(), createdBy);

        // Create SLA config if deadline specified
        if (request.getDeadlineHours() != null) {
            SlaEscalationConfig sla = SlaEscalationConfig.builder()
                    .config(config)
                    .deadlineHours(request.getDeadlineHours())
                    .escalationRole(request.getEscalationRole())
                    .autoExpire(Boolean.TRUE.equals(request.getAutoExpire()))
                    .build();
            slaRepository.save(sla);
        }

        // Save checker users
        saveCheckers(config.getId(), request.getCheckerUserIds());

        return toResponse(config);
    }

    @Transactional
    public ConfigResponse updateConfig(UUID id, ConfigRequest request) {
        MakerCheckerConfig config = findConfigOrThrow(id);

        config.setServiceName(request.getServiceName());
        config.setEndpointPattern(request.getEndpointPattern());
        config.setHttpMethod(request.getHttpMethod().toUpperCase());
        config.setEndpointGroup(request.getEndpointGroup());
        config.setDescription(request.getDescription());
        config.setSameMakerCanCheck(Boolean.TRUE.equals(request.getSameMakerCanCheck()));
        config.setEnabled(Boolean.TRUE.equals(request.getEnabled()));

        config = configRepository.save(config);

        // Update or create SLA
        if (request.getDeadlineHours() != null) {
            SlaEscalationConfig sla = slaRepository.findByConfigId(id)
                    .orElse(SlaEscalationConfig.builder().config(config).build());
            sla.setDeadlineHours(request.getDeadlineHours());
            sla.setEscalationRole(request.getEscalationRole());
            sla.setAutoExpire(Boolean.TRUE.equals(request.getAutoExpire()));
            slaRepository.save(sla);
        }

        // Update checker users
        if (request.getCheckerUserIds() != null) {
            saveCheckers(id, request.getCheckerUserIds());
        }

        return toResponse(config);
    }

    @Transactional
    public void deleteConfig(UUID id) {
        MakerCheckerConfig config = findConfigOrThrow(id);
        checkerRepository.deleteByConfigId(id);
        slaRepository.deleteByConfigId(id);
        configRepository.delete(config);
        log.info("Deleted maker-checker config: {}", id);
    }

    /**
     * Check if a given HTTP method + path matches any enabled config.
     * Used by the gateway filter to decide whether to intercept the request.
     */
    public MakerCheckerConfig findMatchingConfig(String httpMethod, String requestPath) {
        List<MakerCheckerConfig> configs = configRepository.findByEnabledTrue();

        for (MakerCheckerConfig config : configs) {
            if (config.getHttpMethod().equalsIgnoreCase(httpMethod) &&
                    pathMatches(config.getEndpointPattern(), requestPath)) {
                return config;
            }
        }
        return null;
    }

    /**
     * Get checker user IDs for a given config.
     */
    public List<UUID> getCheckerUserIds(UUID configId) {
        return checkerRepository.findByConfigId(configId).stream()
                .map(ConfigChecker::getUserId)
                .toList();
    }

    /**
     * Simple path pattern matching supporting {param} and ** wildcards.
     */
    private boolean pathMatches(String pattern, String path) {
        // Replace {param} with a regex segment
        String regex = pattern
                .replaceAll("\\{[^}]+\\}", "[^/]+")
                .replace("/**", "/.*")
                .replace("/*", "/[^/]+")
                .replace("/", "\\/");
        return Pattern.matches(regex, path);
    }

    private void saveCheckers(UUID configId, List<UUID> userIds) {
        checkerRepository.deleteByConfigId(configId);
        if (userIds != null && !userIds.isEmpty()) {
            List<ConfigChecker> checkers = userIds.stream()
                    .map(userId -> ConfigChecker.builder()
                            .configId(configId)
                            .userId(userId)
                            .build())
                    .toList();
            checkerRepository.saveAll(checkers);
        }
    }

    private MakerCheckerConfig findConfigOrThrow(UUID id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Config not found: " + id));
    }

    private ConfigResponse toResponse(MakerCheckerConfig config) {
        List<UUID> checkerIds = checkerRepository.findByConfigId(config.getId()).stream()
                .map(ConfigChecker::getUserId)
                .toList();

        ConfigResponse.ConfigResponseBuilder builder = ConfigResponse.builder()
                .id(config.getId())
                .productId(config.getProductId())
                .serviceName(config.getServiceName())
                .endpointPattern(config.getEndpointPattern())
                .httpMethod(config.getHttpMethod())
                .endpointGroup(config.getEndpointGroup())
                .description(config.getDescription())
                .sameMakerCanCheck(config.isSameMakerCanCheck())
                .enabled(config.isEnabled())
                .createdBy(config.getCreatedBy())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .checkerUserIds(checkerIds);

        slaRepository.findByConfigId(config.getId()).ifPresent(sla -> builder
                .deadlineHours(sla.getDeadlineHours())
                .escalationRole(sla.getEscalationRole())
                .autoExpire(sla.isAutoExpire()));

        return builder.build();
    }
}
