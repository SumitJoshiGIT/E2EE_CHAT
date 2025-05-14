package com.e2ee.chat.websocket;

import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.Message;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.service.ProfileService;
import com.e2ee.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ProfileService profileService;
    private final Set<String> onlineUsers = new HashSet<>();

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        log.debug("Received message from {} to {}: {}", 
            chatMessage.getSender(), 
            chatMessage.getRecipient(), 
            chatMessage.getContent());
            
        chatService.saveMessage(chatMessage);    
        String recipientDestination = "/user/" + chatMessage.getRecipient() + "/queue/messages";
        log.debug("Sending message to recipient at: {}", recipientDestination);
        messagingTemplate.convertAndSendToUser(
            chatMessage.getRecipient(),
            "/queue/messages",
            chatMessage
        );
        
        String senderDestination = "/user/" + chatMessage.getSender() + "/queue/messages";
        log.debug("Sending confirmation to sender at: {}", senderDestination);
        messagingTemplate.convertAndSendToUser(
            chatMessage.getSender(),
            "/queue/messages",
            chatMessage
        );
    }

    @MessageMapping("/chat.keyExchange")
    public void handleKeyExchange(@Payload ChatMessage chatMessage) {
        log.debug("Received key exchange from {} to {}", 
            chatMessage.getSender(), 
            chatMessage.getRecipient());
            
        String recipientDestination = "/user/" + chatMessage.getRecipient() + "/queue/messages";
        log.debug("Sending key exchange to recipient at: {}", recipientDestination);
        messagingTemplate.convertAndSendToUser(
            chatMessage.getRecipient(),
            "/queue/messages",
            chatMessage
        );
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        log.debug("User {} joined the chat", chatMessage.getSender());
        
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        onlineUsers.add(chatMessage.getSender());
        
        log.debug("Broadcasting user join to /topic/public");
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    @MessageMapping("/chat.leave")
    public void handleLeave(@Payload ChatMessage chatMessage) {
        log.debug("User {} left the chat", chatMessage.getSender());
        
        onlineUsers.remove(chatMessage.getSender());
        
        log.debug("Broadcasting user leave to /topic/public");
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }
    
    @MessageMapping("/chat.searchUsers")
    public void searchUsers(@Payload Map<String, Object> payload) {
        String query = (String) payload.get("query");
        String sender = (String) payload.get("sender");
        
        log.debug("Searching users for query: {} by sender: {}", query, sender);
        
        List<UserProfile> users = chatService.searchUsers(query, 4);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "SEARCH_RESULTS");
        response.put("users", users);
        
        messagingTemplate.convertAndSendToUser(
            sender,
            "/queue/messages",
            response
        );
    }
    
    @MessageMapping("/chat.createChat")
    public void createChat(@Payload Map<String, Object> payload) {
        String ownerId = (String) payload.get("ownerId");
        String targetUserId = (String) payload.get("targetUserId");
        
        log.debug("Creating chat between {} and {}", ownerId, targetUserId);
        
        try {
            Chat chat = chatService.createChatWithUser(ownerId, targetUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CHAT_CREATED");
            response.put("chat", chat);
            
            messagingTemplate.convertAndSendToUser(
                ownerId,
                "/queue/messages",
                response
            );
            
            messagingTemplate.convertAndSendToUser(
                targetUserId,
                "/queue/messages",
                response
            );
        } catch (Exception e) {
            log.error("Error creating chat", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            errorResponse.put("error", "Failed to create chat: " + e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                ownerId,
                "/queue/messages",
                errorResponse
            );
        }
    }
    
    @MessageMapping("/chat.sendMessage")
    public void sendChatMessage(@Payload Map<String, Object> payload) {
        String chatId = (String) payload.get("chatId");
        String senderId = (String) payload.get("senderId");
        String content = (String) payload.get("content");
        String messageType = (String) payload.getOrDefault("messageType", "TEXT");
        
        log.debug("Sending message to chat {}: {}", chatId, content);
        
        try {
            Message message = chatService.sendMessageToChat(chatId, senderId, content, messageType);
            
            Chat chat = chatService.getChatById(chatId);
            if (chat != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "MESSAGE");
                response.put("message", message);
                response.put("chatId", chatId);
                
                for (String participant : chat.getParticipants()) {
                    messagingTemplate.convertAndSendToUser(
                        participant,
                        "/queue/messages",
                        response
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error sending message", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            errorResponse.put("error", "Failed to send message: " + e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                senderId,
                "/queue/messages",
                errorResponse
            );
        }
    }
}
