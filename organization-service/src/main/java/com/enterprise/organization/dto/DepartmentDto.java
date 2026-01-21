package com.enterprise.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private UUID parentId;
    private String parentName;
    private UUID branchId;
    private String branchName;
    private UUID headUserId;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String code;
        private String name;
        private String description;
        private UUID parentId;
        private UUID branchId;
        private UUID headUserId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String description;
        private UUID parentId;
        private UUID branchId;
        private UUID headUserId;
        private String status;
    }
}
