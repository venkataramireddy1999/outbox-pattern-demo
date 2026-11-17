package com.example.outbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OutboxPatternDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(OutboxPatternDemoApplication.class, args);
    }
}
