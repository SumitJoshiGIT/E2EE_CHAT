package com.e2ee.chat.controller;

import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.Message;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.service.ChatService;
import com.e2ee.chat.service.ProfileService;
import com.e2ee.chat.websocket.ChatMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ProfileService profileService;
    private final ChatMessageHandler chatMessageHandler;

    @GetMapping
    public ResponseEntity<List<Chat>> getChats(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.getChatsByOwner(userDetails.getUsername()));
    }
    
    @GetMapping("/participant")
    public ResponseEntity<List<Chat>> getChatsByParticipant(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.findChatsByParticipant(userDetails.getUsername()));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Message>> getMessages(@RequestParam String chatId) {
        return ResponseEntity.ok(chatService.getMessagesByChatId(chatId));
    }
    
    @GetMapping("/user-status")
    public ResponseEntity<Map<String, Boolean>> getUsersStatus(@RequestParam List<String> userIds) {
        Map<String, Boolean> statusMap = new HashMap<>();
        for (String userId : userIds) {
            statusMap.put(userId, chatMessageHandler.isUserOnline(userId));
        }
        return ResponseEntity.ok(statusMap);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<UserProfile>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Don't include the current user in search results
        List<UserProfile> results = chatService.searchUsers(query, limit);
        results.removeIf(profile -> profile.getUsername().equals(userDetails.getUsername()));
        
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/{chatId}")
    public ResponseEntity<Chat> getChatById(@PathVariable String chatId, @AuthenticationPrincipal UserDetails userDetails) {
        Chat chat = chatService.getChatById(chatId);
        
        // Security check - only return if user is a participant
        if (chat != null && chat.getParticipants().contains(userDetails.getUsername())) {
            return ResponseEntity.ok(chat);
        }
        
        return ResponseEntity.notFound().build();
    }
}
