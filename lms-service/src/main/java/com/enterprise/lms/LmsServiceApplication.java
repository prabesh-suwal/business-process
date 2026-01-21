package com.enterprise.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LmsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmsServiceApplication.class, args);
    }
}
