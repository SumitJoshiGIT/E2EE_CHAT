package com.e2ee.chat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;

public class UsernameHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull org.springframework.http.server.ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        System.out.println("[DEBUG] UsernameHandshakeInterceptor: beforeHandshake called");
        if (request instanceof ServletServerHttpRequest) {
            String query = ((ServletServerHttpRequest) request).getServletRequest().getQueryString();
            System.out.println("[DEBUG] Handshake query string: " + query);
            
            // First try to extract userId (new parameter)
            if (query != null && query.contains("userId=")) {
                String userId = query.substring(query.indexOf("userId=") + 7);
                int ampIdx = userId.indexOf('&');
                if (ampIdx != -1) {
                    userId = userId.substring(0, ampIdx);
                }
                System.out.println("[DEBUG] UserId extracted: '" + userId + "'");
                attributes.put("username", userId); // Store userId as username for backward compatibility
                System.out.println("[DEBUG] UsernameHandshakeInterceptor: Using userId as principal: " + userId);
            }
            // Fallback to username for backward compatibility
            else if (query != null && query.contains("username=")) {
                String username = query.substring(query.indexOf("username=") + 9);
                int ampIdx = username.indexOf('&');
                if (ampIdx != -1) {
                    username = username.substring(0, ampIdx);
                }
                System.out.println("[DEBUG] Username extracted (fallback): '" + username + "'");
                attributes.put("username", username);
                System.out.println("[DEBUG] UsernameHandshakeInterceptor: Put username in attributes: " + username);
            } else {
                System.out.println("[DEBUG] UsernameHandshakeInterceptor: No userId or username found in query string");
            }
        } else {
            System.out.println("[DEBUG] UsernameHandshakeInterceptor: Not a ServletServerHttpRequest");
        }
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull org.springframework.http.server.ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, @Nullable Exception ex) {
        System.out.println("[DEBUG] UsernameHandshakeInterceptor: afterHandshake called");
        // No-op
    }
}
