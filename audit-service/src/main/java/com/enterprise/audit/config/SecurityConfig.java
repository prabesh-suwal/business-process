package com.enterprise.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Audit Service.
 * 
 * This is an internal service that receives audit events from other services.
 * All endpoints are permitted since authentication is handled at the gateway
 * level.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (internal service, no browser interactions)
                .csrf(AbstractHttpConfigurer::disable)
                // Disable HTTP Basic auth (prevents browser popup)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Disable form login
                .formLogin(AbstractHttpConfigurer::disable)
                // Stateless session (no session cookies)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Permit all requests (auth handled at gateway level)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
