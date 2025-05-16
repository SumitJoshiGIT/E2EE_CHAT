package com.e2ee.chat.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.Map;

public class UserHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        // Try to extract username from headers (works for SockJS and native)
        String username = request.getHeaders().getFirst("username");
        if (username != null) {
            attributes.put("username", username);
        }
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                              @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // No-op
    }
}
