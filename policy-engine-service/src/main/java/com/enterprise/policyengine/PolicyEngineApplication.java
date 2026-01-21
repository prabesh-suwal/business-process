package com.enterprise.policyengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Policy Engine Service - Custom ABAC/RBAC Policy Engine
 * 
 * A microservice for managing and evaluating authorization policies.
 * Super Admins can create, update, and manage policies via the API.
 * Other services call /api/evaluate to check authorization.
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
public class PolicyEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolicyEngineApplication.class, args);
    }
}
