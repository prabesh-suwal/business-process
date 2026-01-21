package com.cas.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiClientDto {
    private UUID id;
    private String clientId;
    private String name;
    private String description;
    private String status;
    private String[] allowedIps;
    private Integer rateLimitPerMinute;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant lastUsedAt;
    private List<String> scopes;

    // Only returned on create
    private String clientSecret;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateClientRequest {
        private String name;
        private String description;
        private String[] allowedIps;
        private Integer rateLimitPerMinute;
        private Instant expiresAt;
        private List<UUID> permissionIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateClientRequest {
        private String name;
        private String description;
        private String status;
        private String[] allowedIps;
        private Integer rateLimitPerMinute;
        private Instant expiresAt;
        private List<UUID> permissionIds;
    }
}
