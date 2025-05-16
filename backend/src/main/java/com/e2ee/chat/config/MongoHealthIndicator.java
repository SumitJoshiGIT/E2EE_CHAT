package com.e2ee.chat.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Health indicator for MongoDB connection status
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;
    
    @Override
    public Health health() {
        try {
            // Try to execute a simple command
            mongoTemplate.executeCommand("{ ping: 1 }");
            return Health.up()
                .withDetail("service", "MongoDB")
                .withDetail("status", "Connection successful")
                .build();
        } catch (Exception e) {
            log.error("MongoDB health check failed", e);
            return Health.down()
                .withDetail("service", "MongoDB")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
