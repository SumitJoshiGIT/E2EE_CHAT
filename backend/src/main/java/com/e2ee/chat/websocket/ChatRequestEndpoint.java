package com.e2ee.chat.websocket;

import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Separate controller for handling chat list requests
 * This helps avoid serialization issues with the chat list endpoint
 */
@Controller
@Slf4j
public class ChatRequestEndpoint {

    @Autowired
    private ChatMessageHandler chatMessageHandler;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Handle request for user's chat list
     * This endpoint is called specifically to get the chat list
     */
    @MessageMapping("/chat.getChats")
    public void getChatList(@Payload String payload) {
        log.info("Received chat list request with payload: {}", payload);
        try {
            // Decode Base64 to get the JSON string
            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            // Forward to the main handler
            chatMessageHandler.handleChatListRequest(json);
        } catch (Exception e) {
            log.error("Error decoding or parsing chat list payload", e);
        }
    }

    /**
     * Handle chat creation requests
     * This endpoint is called specifically to create a new chat
     */
    @MessageMapping("/chat.createChatRequest")
    public void createChat(@Payload String payload) {
        log.info("Received chat creation request with payload: {}", payload);
        try {
            // Decode Base64 to get the JSON string
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(payload);
            String json = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            // Parse the JSON payload
            java.util.Map<String, Object> payloadMap = objectMapper.readValue(json, java.util.Map.class);
            // Forward to the main handler
            chatMessageHandler.createChat(payloadMap);
        } catch (Exception e) {
            log.error("Error parsing chat creation payload", e);
        }
    }
}
