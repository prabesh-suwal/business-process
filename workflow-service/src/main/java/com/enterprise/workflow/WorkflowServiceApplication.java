package com.enterprise.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration.class,
        org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration.class
})
public class WorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowServiceApplication.class, args);
    }
}
