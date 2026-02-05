package com.cas.server.config;

import com.cas.server.service.TokenService;
import com.cas.common.security.TokenClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var tokenClaims = tokenService.parseAccessToken(token);
                String userId = tokenClaims.getSub();
                String email = tokenClaims.getEmail();

                // Create authentication with ADMIN role for now
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("JWT authenticated user: {} ({}) for request: {}", email, tokenClaims.getType(),
                        request.getRequestURI());
            } catch (Exception e) {
                log.warn("JWT authentication failed for {}: {}", request.getRequestURI(), e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't filter public endpoints
        return path.startsWith("/auth/") ||
                path.startsWith("/oauth/") ||
                path.startsWith("/.well-known/") ||
                path.startsWith("/admin/workflow-config/") || // Public dropdown data
                path.equals("/actuator/health");
    }
}
