package com.e2ee.chat.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handle MongoDB connection exceptions
     */
    @ExceptionHandler(MongoTimeoutException.class)
    public ResponseEntity<Object> handleMongoTimeoutException(MongoTimeoutException ex, WebRequest request) {
        log.error("MongoDB connection timeout", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Database Connection Error");
        body.put("message", "Unable to connect to the database. Please try again later.");
        
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Handle general MongoDB exceptions
     */
    @ExceptionHandler(MongoException.class)
    public ResponseEntity<Object> handleMongoException(MongoException ex, WebRequest request) {
        log.error("MongoDB error", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Database Error");
        body.put("message", "A database error occurred. Please try again later.");
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handle illegal argument exceptions (validation errors)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Validation error", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Fallback handler for all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please try again later.");
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
