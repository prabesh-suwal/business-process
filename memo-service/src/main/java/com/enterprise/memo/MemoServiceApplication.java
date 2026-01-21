package com.enterprise.memo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = { "com.enterprise.memo", "com.cas.common" })
public class MemoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoServiceApplication.class, args);
    }
}
