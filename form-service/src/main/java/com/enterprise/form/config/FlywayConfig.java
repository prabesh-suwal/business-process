package com.enterprise.form.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

/**
 * Configuration to ensure Flyway migrations run BEFORE anything else.
 * Uses @Order(Ordered.HIGHEST_PRECEDENCE) to be initialized first.
 * 
 * Key: Using baselineVersion("0") ensures V1 migration always runs,
 * even when other frameworks have already created tables.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FlywayConfig {

    private final DataSource dataSource;

    @Autowired
    public FlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Run Flyway migrations immediately after this bean is constructed.
     * This ensures migrations run before Hibernate/JPA.
     */
    @PostConstruct
    public void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0") // Use version 0 as baseline so V1 always runs
                .table("flyway_schema_history")
                .load();

        flyway.migrate();
    }
}
