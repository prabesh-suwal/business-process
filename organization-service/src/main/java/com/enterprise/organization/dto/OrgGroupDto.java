package com.enterprise.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgGroupDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String groupType;
    private UUID branchId;
    private String branchName;
    private UUID departmentId;
    private String departmentName;
    private String status;
    private List<MemberDto> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private UUID id;
        private UUID userId;
        private String memberRole;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String code;
        private String name;
        private String description;
        private String groupType;
        private UUID branchId;
        private UUID departmentId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String description;
        private String groupType;
        private UUID branchId;
        private UUID departmentId;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddMemberRequest {
        private UUID userId;
        private String memberRole;
    }
}
