package com.enterprise.policyengine.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a fallback CacheManager when Redis is unavailable.
 * If spring-boot-starter-data-redis auto-configures a RedisCacheManager,
 * that bean takes precedence.
 */
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("policies", "evaluations", "policyGroups");
    }
}
