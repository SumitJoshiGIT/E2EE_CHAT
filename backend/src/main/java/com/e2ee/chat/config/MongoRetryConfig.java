package com.e2ee.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for MongoDB retry mechanism
 * This will help with temporary network issues
 */
@Configuration
@EnableRetry
@Slf4j
public class MongoRetryConfig {

    @Bean
    public RetryTemplate mongoRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Set up exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500); // 500ms initial backoff
        backOffPolicy.setMaxInterval(10000);   // Max 10 seconds between retries
        backOffPolicy.setMultiplier(2);        // Double the interval each time
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Configure which exceptions should trigger retry
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(MongoSocketException.class, true);
        retryableExceptions.put(MongoTimeoutException.class, true);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true); // Max 3 attempts
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Add a listener for logging
        retryTemplate.registerListener(new org.springframework.retry.listener.RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void onError(org.springframework.retry.RetryContext context, 
                                                        org.springframework.retry.RetryCallback<T, E> callback, 
                                                        Throwable throwable) {
                log.warn("MongoDB operation failed. Retry attempt {} of {}. Error: {}", 
                        context.getRetryCount(), 
                        retryPolicy.getMaxAttempts(),
                        throwable.getMessage());
            }
        });
        
        return retryTemplate;
    }
}
