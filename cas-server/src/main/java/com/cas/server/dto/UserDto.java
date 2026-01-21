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
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String status;
    private boolean mfaEnabled;
    private String department;
    private String userGroup;
    private UUID branchId;
    private UUID departmentId;
    private Instant createdAt;
    private Instant lastLoginAt;
    private List<RoleDto> roles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String phone;
        private String department;
        private String userGroup;
        private UUID branchId;
        private UUID departmentId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserRequest {
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String status;
        private String department;
        private String userGroup;
        private UUID branchId;
        private UUID departmentId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignRolesRequest {
        private List<UUID> roleIds;
    }
}
