package com.enterprise.organization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class OrganizationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrganizationServiceApplication.class, args);
    }
}
