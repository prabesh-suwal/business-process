package com.cas.server.controller;

import com.cas.common.dto.ApiMessage;
import com.cas.server.dto.EffectiveAccessDto;
import com.cas.server.service.EffectiveAccessService;
import com.cas.server.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Returns the effective access context for the authenticated user.
 * Frontend calls this after login to know what to show/hide.
 * 
 * Response contains resolved PRODUCT.MODULE.ACTION permissions grouped by
 * product.
 * No raw roles are exposed.
 */
@Slf4j
@Tag(name = "User Access", description = "Endpoints for retrieving user access context and effective permissions")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final EffectiveAccessService effectiveAccessService;
    private final TokenService tokenService;

    /**
     * Get the full effective access context for the authenticated user.
     * Returns all products the user has access to, with modules and actions.
     */
    @Operation(summary = "Get Effective Access", description = "Returns the authenticated user's resolved permissions grouped by Product → Module → Actions")
    @ApiMessage("Effective access retrieved")
    @GetMapping("/me")
    public ResponseEntity<EffectiveAccessDto> getEffectiveAccess(
            @RequestHeader("Authorization") String authHeader) {

        UUID userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        EffectiveAccessDto access = effectiveAccessService.getEffectiveAccess(userId);
        log.debug("Effective access computed for user {} — {} products", userId, access.getProducts().size());

        return ResponseEntity.ok(access);
    }

    /**
     * Get effective access for a specific product only.
     * Useful when the frontend only needs access for one product context.
     */
    @Operation(summary = "Get Product Access", description = "Returns the authenticated user's resolved permissions for a specific product")
    @ApiMessage("Product access retrieved")
    @GetMapping("/me/access/{productCode}")
    public ResponseEntity<EffectiveAccessDto> getProductAccess(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String productCode) {

        UUID userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        EffectiveAccessDto access = effectiveAccessService.getEffectiveAccessForProduct(userId, productCode);
        return ResponseEntity.ok(access);
    }

    /**
     * Extract user ID from Bearer token.
     */
    private UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            var claims = tokenService.parseAccessToken(token);
            return UUID.fromString(claims.getSub());
        } catch (Exception e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }
}
