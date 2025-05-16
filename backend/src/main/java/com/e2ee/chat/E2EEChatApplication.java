package com.e2ee.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableMongoRepositories
@Slf4j
public class E2EEChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(E2EEChatApplication.class, args);
    }
    
    @Bean
    public ApplicationListener<ApplicationReadyEvent> mongoDbConnectionValidator(final MongoTemplate mongoTemplate) {
        return event -> {
            try {
                // Check MongoDB connection by listing collections
                mongoTemplate.getCollectionNames();
                log.info("Successfully connected to MongoDB");
            } catch (Exception e) {
                log.error("Failed to connect to MongoDB. Application may not function correctly.", e);
                // Don't throw exception to prevent application from exiting
                // but log the error so it's visible
            }
        };
    }
}