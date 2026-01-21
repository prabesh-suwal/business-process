package com.enterprise.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for workflow-service.
 * For now, we permit all requests since authentication is handled at the
 * gateway.
 * TODO: Add JWT validation filter for production environments.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        // All API endpoints - gateway handles auth
                        .requestMatchers("/api/**").permitAll()
                        // Everything else
                        .anyRequest().permitAll());

        return http.build();
    }

    /**
     * Suppress Spring Security's auto-generated password warning.
     */
    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return new org.springframework.security.provisioning.InMemoryUserDetailsManager();
    }
}
