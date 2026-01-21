package com.cas.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private UUID id;
    private String productCode;
    private String code;
    private String name;
    private String description;
    private boolean systemRole;
    private UUID parentRoleId;
    private Instant createdAt;
    private Set<PermissionDto> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoleRequest {
        private String productCode;
        private String code;
        private String name;
        private String description;
        private UUID parentRoleId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRoleRequest {
        private String name;
        private String description;
        private UUID parentRoleId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetPermissionsRequest {
        private List<UUID> permissionIds;
    }
}
