package com.lms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LmsGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmsGatewayApplication.class, args);
    }

}
