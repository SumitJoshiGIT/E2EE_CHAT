package com.e2ee.chat.config;

import com.e2ee.chat.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Component
public class WebSocketEventListener {
    
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = headerAccessor.getUser().getName();
        
        if (username != null) {
            List<String> users = webSocketService.getOnlineUsers();
            if (!users.contains(username)) {
                users.add(username);
                messagingTemplate.convertAndSend("/topic/users", users);
            }
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = headerAccessor.getUser().getName();
        
        if (username != null) {
            List<String> users = webSocketService.getOnlineUsers();
            users.remove(username);
            messagingTemplate.convertAndSend("/topic/users", users);
        }
    }
} 