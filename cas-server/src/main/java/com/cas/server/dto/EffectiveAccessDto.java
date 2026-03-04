package com.cas.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Effective access context returned by GET /api/me.
 * Contains resolved permissions grouped by Product → Module → Actions.
 * Frontend uses PRODUCT.MODULE.ACTION key pattern (e.g., MMS.MEMO.APPROVE).
 * No raw roles are exposed — only resolved actions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveAccessDto {

    private UserProfile user;
    private List<ProductAccess> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfile {
        private String id;
        private String username;
        private String email;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAccess {
        private String code;
        private String name;
        private List<ModuleAccess> modules;
        private Map<String, Object> constraints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleAccess {
        private String code;
        private List<String> permissions;
    }
}
