package com.cas.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the claims extracted from an access token.
 * Used by gateways to validate and extract user/client information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenClaims {

    private String jti;
    private String sub;
    private TokenType type;
    private String email;
    private String name;
    private String clientName;
    private Map<String, ProductClaims> products;
    private Set<String> scopes;
    private String aud;
    private String iss;
    private Long iat;
    private Long exp;
    private Integer ver;

    public enum TokenType {
        USER,
        SERVICE,
        ADMIN
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductClaims {
        private List<String> roles;
        private Set<String> scopes;
    }

    /**
     * Check if the token has a specific scope for any product.
     */
    public boolean hasScope(String scope) {
        if (scopes != null && scopes.contains(scope)) {
            return true;
        }
        if (products != null) {
            return products.values().stream()
                    .anyMatch(p -> p.getScopes() != null && p.getScopes().contains(scope));
        }
        return false;
    }

    /**
     * Check if the token has a scope for a specific product.
     */
    public boolean hasScope(String productCode, String scope) {
        if (products != null && products.containsKey(productCode)) {
            ProductClaims product = products.get(productCode);
            return product.getScopes() != null && product.getScopes().contains(scope);
        }
        return false;
    }

    /**
     * Check if the token is expired.
     */
    public boolean isExpired() {
        return exp != null && System.currentTimeMillis() / 1000 > exp;
    }

    /**
     * Check if the audience matches.
     */
    public boolean hasAudience(String audience) {
        return aud != null && aud.equals(audience);
    }
}
