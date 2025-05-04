package com.e2ee.chat.controller;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class WebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public ChatMessage handleMessage(ChatMessage message) {
        return message;
    }
    
    @MessageMapping("/users")
    @SendTo("/topic/users")
    public List<String> getOnlineUsers() {
        return webSocketService.getOnlineUsers();
    }
} 