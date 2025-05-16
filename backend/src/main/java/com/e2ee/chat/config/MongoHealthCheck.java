package com.e2ee.chat.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Component to check MongoDB connection health on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoHealthCheck {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * Checks the MongoDB connection when the application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkMongoDbConnection() {
        log.info("Checking MongoDB connection...");
        try {
            // Try to list collections to verify connection
            // In Spring Boot 3.x, getCollectionNames() returns Set<String> instead of MongoIterable<String>
            Set<String> collectionNames = mongoTemplate.getCollectionNames();
            log.info("MongoDB connection successful. Found collections:");
            
            int count = 0;
            for (String collectionName : collectionNames) {
                MongoCollection<?> collection = mongoTemplate.getCollection(collectionName);
                long documentCount = collection.countDocuments();
                log.info("  - {} ({} documents)", collectionName, documentCount);
                count++;
            }
            
            if (count == 0) {
                log.warn("No collections found in the database. This might be normal for a fresh installation.");
            }
            
            log.info("MongoDB health check completed successfully.");
        } catch (MongoTimeoutException e) {
            log.error("MongoDB connection timeout! Make sure MongoDB is running and accessible.", e);
            log.error("Application may not function correctly without database access!");
        } catch (Exception e) {
            log.error("Failed to connect to MongoDB! Details: {}", e.getMessage(), e);
            log.error("Application may not function correctly without database access!");
        }
    }
}
