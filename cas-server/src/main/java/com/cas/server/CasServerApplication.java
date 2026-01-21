package com.cas.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableRedisRepositories(basePackages = "com.cas.server.repository")
public class CasServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CasServerApplication.class, args);
    }

}
