package com.e2ee.chat.config;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

/**
 * Filter to add request tracing for debugging
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Get request ID from header or generate a new one
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        
        // Add request ID to MDC for logging
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        
        // Add request ID to response headers
        response.setHeader(REQUEST_ID_HEADER, requestId);
        
        try {
            // Log request details
            if (log.isDebugEnabled()) {
                log.debug("Incoming request [{}]: {} {} (from {})", 
                    requestId,
                    request.getMethod(), 
                    request.getRequestURI(),
                    request.getRemoteAddr());
            }
            
            // Process request
            long startTime = System.currentTimeMillis();
            filterChain.doFilter(request, response);
            
            // Log response time
            if (log.isDebugEnabled()) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Request completed [{}]: {} {} - Status: {} (took {} ms)",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(), 
                    response.getStatus(),
                    duration);
            }
        } catch (Exception e) {
            log.error("Error processing request [{}]: {} {} - Exception: {}", 
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage(), e);
            throw e;
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
