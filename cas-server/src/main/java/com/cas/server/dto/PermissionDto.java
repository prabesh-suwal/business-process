package com.cas.server.dto;

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
public class PermissionDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String category;
    private List<String> productCodes;

    // Backward compatibility
    private String productCode;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePermissionRequest {
        private String code;
        private String name;
        private String description;
        private String category;
        private List<String> productCodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePermissionRequest {
        private String name;
        private String description;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignProductsRequest {
        private List<String> productCodes;
    }
}
