package com.enterprise.memo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MemoGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoGatewayApplication.class, args);
    }
}
