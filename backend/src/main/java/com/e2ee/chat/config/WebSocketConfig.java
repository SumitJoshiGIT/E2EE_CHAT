package com.e2ee.chat.config;

import java.security.Principal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.lang.NonNull;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketErrorHandler webSocketErrorHandler;

    public WebSocketConfig(WebSocketErrorHandler webSocketErrorHandler) {
        this.webSocketErrorHandler = webSocketErrorHandler;
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new UsernameHandshakeInterceptor())
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // Set the error handler for STOMP messages
        registry.setErrorHandler(webSocketErrorHandler);
    }
    
    /**
     * Configure the message converter for WebSocket messages
     */
    @Bean
    public MappingJackson2MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setSerializedPayloadClass(String.class);
        return converter;
    }
    
    /**
     * Customize error handling for WebSocket messages
     */
    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(128 * 1024) // 128KB
               .setSendBufferSizeLimit(512 * 1024) // 512KB
               .setSendTimeLimit(20000); // 20 seconds
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Try to get userId from headers first
                    String userId = accessor.getFirstNativeHeader("userId");
                    
                    // If no userId in headers, try session attributes
                    if (userId == null && accessor.getSessionAttributes() != null) {
                        Object attr = accessor.getSessionAttributes().get("username");  // We stored userId here in handshake interceptor
                        if (attr != null) {
                            userId = attr.toString();
                            System.out.println("[DEBUG] ChannelInterceptor: Got userId from session attributes: " + userId);
                        }
                    }
                    
                    final String finalUserId = userId;
                    System.out.println("[DEBUG] ChannelInterceptor: CONNECT received, userId: " + finalUserId);
                    if (finalUserId != null) {
                        accessor.setUser(new Principal() {
                            @Override
                            public String getName() {
                                return finalUserId;
                            }
                        });
                        System.out.println("[DEBUG] ChannelInterceptor: Principal set to userId: " + finalUserId);
                    } else {
                        System.out.println("[DEBUG] ChannelInterceptor: No userId found in CONNECT");
                    }
                }
                return message;
            }
        });
    }
}