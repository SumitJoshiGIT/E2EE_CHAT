package com.e2ee.chat.websocket;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        // Forward the encrypted message to the recipient
        messagingTemplate.convertAndSendToUser(
            chatMessage.getRecipient(),
            "/queue/messages",
            chatMessage
        );
    }

    @MessageMapping("/chat.keyExchange")
    public void handleKeyExchange(@Payload ChatMessage chatMessage) {
        // Forward the key exchange message to the recipient
        messagingTemplate.convertAndSendToUser(
            chatMessage.getRecipient(),
            "/queue/messages",
            chatMessage
        );
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Add username to web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        
        // Notify others about new user
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    @MessageMapping("/chat.leave")
    public void handleLeave(@Payload ChatMessage chatMessage) {
        // Notify others about user leaving
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }
} 