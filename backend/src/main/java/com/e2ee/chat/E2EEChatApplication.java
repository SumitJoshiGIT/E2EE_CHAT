package com.e2ee.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class E2EEChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(E2EEChatApplication.class, args);
    }
} 