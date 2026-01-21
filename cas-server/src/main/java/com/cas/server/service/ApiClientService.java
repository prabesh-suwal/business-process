package com.cas.server.service;

import com.cas.server.domain.client.ApiClient;
import com.cas.server.domain.product.Permission;
import com.cas.server.dto.ApiClientDto;
import com.cas.server.dto.PageDto;
import com.cas.server.repository.ApiClientRepository;
import com.cas.server.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiClientService {

    private final ApiClientRepository apiClientRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Transactional(readOnly = true)
    public PageDto<ApiClientDto> listClients(Pageable pageable) {
        Page<ApiClient> page = apiClientRepository.findAll(pageable);
        return toPageDto(page.map(this::toClientDto));
    }

    @Transactional(readOnly = true)
    public ApiClientDto getClient(UUID id) {
        ApiClient client = apiClientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found: " + id));
        return toClientDto(client);
    }

    @Transactional
    public ApiClientDto createClient(ApiClientDto.CreateClientRequest request) {
        String clientId = generateClientId();
        String clientSecret = generateClientSecret();

        Set<Permission> scopes = new HashSet<>();
        if (request.getPermissionIds() != null) {
            for (UUID permId : request.getPermissionIds()) {
                Permission perm = permissionRepository.findById(permId)
                        .orElseThrow(() -> new RuntimeException("Permission not found: " + permId));
                scopes.add(perm);
            }
        }

        ApiClient client = ApiClient.builder()
                .clientId(clientId)
                .clientSecretHash(passwordEncoder.encode(clientSecret))
                .name(request.getName())
                .description(request.getDescription())
                .allowedIps(request.getAllowedIps())
                .rateLimitPerMinute(request.getRateLimitPerMinute() != null
                        ? request.getRateLimitPerMinute()
                        : 100)
                .expiresAt(request.getExpiresAt() != null
                        ? request.getExpiresAt()
                        : Instant.now().plusSeconds(365 * 24 * 60 * 60))
                .scopes(scopes)
                .build();

        client = apiClientRepository.save(client);
        log.info("Created API client: {}", client.getClientId());

        // Return with secret (only time it's visible)
        ApiClientDto dto = toClientDto(client);
        dto.setClientSecret(clientSecret);
        return dto;
    }

    @Transactional
    public ApiClientDto updateClient(UUID id, ApiClientDto.UpdateClientRequest request) {
        ApiClient client = apiClientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found: " + id));

        if (request.getName() != null)
            client.setName(request.getName());
        if (request.getDescription() != null)
            client.setDescription(request.getDescription());
        if (request.getAllowedIps() != null)
            client.setAllowedIps(request.getAllowedIps());
        if (request.getRateLimitPerMinute() != null)
            client.setRateLimitPerMinute(request.getRateLimitPerMinute());
        if (request.getExpiresAt() != null)
            client.setExpiresAt(request.getExpiresAt());
        if (request.getStatus() != null) {
            client.setStatus(ApiClient.ClientStatus.valueOf(request.getStatus()));
        }

        if (request.getPermissionIds() != null) {
            Set<Permission> scopes = new HashSet<>();
            for (UUID permId : request.getPermissionIds()) {
                Permission perm = permissionRepository.findById(permId)
                        .orElseThrow(() -> new RuntimeException("Permission not found: " + permId));
                scopes.add(perm);
            }
            client.setScopes(scopes);
        }

        client = apiClientRepository.save(client);
        log.info("Updated API client: {}", client.getClientId());
        return toClientDto(client);
    }

    @Transactional
    public void revokeClient(UUID id) {
        ApiClient client = apiClientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found: " + id));
        client.setStatus(ApiClient.ClientStatus.SUSPENDED);
        apiClientRepository.save(client);
        log.info("Revoked API client: {}", client.getClientId());
    }

    @Transactional
    public ApiClientDto rotateSecret(UUID id) {
        ApiClient client = apiClientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found: " + id));

        String newSecret = generateClientSecret();
        client.setClientSecretHash(passwordEncoder.encode(newSecret));
        client = apiClientRepository.save(client);

        log.info("Rotated secret for API client: {}", client.getClientId());

        ApiClientDto dto = toClientDto(client);
        dto.setClientSecret(newSecret);
        return dto;
    }

    private String generateClientId() {
        return "cas_" + generateRandomString(16);
    }

    private String generateClientSecret() {
        return generateRandomString(48);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private ApiClientDto toClientDto(ApiClient client) {
        return ApiClientDto.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .name(client.getName())
                .description(client.getDescription())
                .status(client.getStatus().name())
                .allowedIps(client.getAllowedIps())
                .rateLimitPerMinute(client.getRateLimitPerMinute())
                .expiresAt(client.getExpiresAt())
                .createdAt(client.getCreatedAt())
                .lastUsedAt(client.getLastUsedAt())
                .scopes(client.getScopeCodes().stream().toList())
                .build();
    }

    private <T> PageDto<T> toPageDto(Page<T> page) {
        return PageDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
