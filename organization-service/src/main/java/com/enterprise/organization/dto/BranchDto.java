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
public class BranchDto {
    private UUID id;
    private String code;
    private String name;
    private String localName;
    private String branchType;
    private UUID geoLocationId;
    private String geoLocationName;
    private UUID parentBranchId;
    private String parentBranchName;
    private String address;
    private String phone;
    private String email;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String code;
        private String name;
        private String localName;
        private String branchType;
        private UUID geoLocationId;
        private UUID parentBranchId;
        private String address;
        private String phone;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String localName;
        private String branchType;
        private UUID geoLocationId;
        private UUID parentBranchId;
        private String address;
        private String phone;
        private String email;
        private String status;
    }
}
