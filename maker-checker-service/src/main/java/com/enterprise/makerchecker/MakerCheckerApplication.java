package com.enterprise.makerchecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MakerCheckerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MakerCheckerApplication.class, args);
    }
}
