package com.cas.server.controller;

import com.cas.server.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/.well-known")
@RequiredArgsConstructor
public class WellKnownController {

    private final TokenService tokenService;

    /**
     * JWK Set endpoint for token verification.
     */
    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok(tokenService.getJwkSet());
    }

    /**
     * OpenID Connect discovery document.
     */
    @GetMapping(value = "/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> openIdConfiguration() {
        return ResponseEntity.ok(tokenService.getOpenIdConfiguration());
    }
}
