package com.enterprise.form.config;

import com.cas.common.security.UserContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for form-service.
 * Gateway handles authentication, this service uses UserContextFilter
 * to extract user info from forwarded headers.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }

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
                        .anyRequest().permitAll())
                .addFilterAfter(userContextFilter(), UsernamePasswordAuthenticationFilter.class);

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
