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
public class ProductDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String status;
    private List<PermissionDto> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateProductRequest {
        private String code;
        private String name;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProductRequest {
        private String name;
        private String description;
        private String status;
    }
}
