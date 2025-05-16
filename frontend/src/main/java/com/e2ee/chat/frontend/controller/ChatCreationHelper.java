package com.e2ee.chat.frontend.controller;

import com.e2ee.chat.frontend.model.UserProfile;
import com.e2ee.chat.frontend.service.WebSocketService;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to patch MainController to fix chat creation issues
 */
public class ChatCreationHelper {
    
    /**
     * Start a chat with a user ensuring WebSocket messages are properly serialized
     * 
     * @param webSocketService The WebSocket service
     * @param user The user to chat with
     * @param statusLabel Status label to update
     */
    public static void startChatWithUser(WebSocketService webSocketService, UserProfile user, 
                                         String currentUserId, Label statusLabel, ListView<?> chatListView) {
        try {
            // Prepare chat creation payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("targetProfileId", user.getProfileId()); // Use profile ID instead of username
            payload.put("ownerId", currentUserId); // Use user ID instead of username
            payload.put("targetUserName", user.getUsername()); // Include the user's readable name
            payload.put("targetDisplayName", user.getDisplayName()); // Include display name if available
            
            System.out.println("Creating new chat with profile ID: " + user.getProfileId());
            
            // Send the request using the serialization-safe method
            webSocketService.sendCreateChatMessage(payload);
            
            statusLabel.setText("Creating chat with " + user.getUsername() + "...");
            
            // Force a refresh of the chat list after a delay
            new Thread(() -> {
                try {
                    // Wait 1 second
                    Thread.sleep(1000);
                    
                    // Request an updated chat list
                    webSocketService.requestChatList();
                    
                    // Wait another 3 seconds
                    Thread.sleep(3000);
                    
                    // Request again to be sure
                    webSocketService.requestChatList();
                    
                    // Show success message
                    Platform.runLater(() -> {
                        statusLabel.setText("Chat with " + user.getUsername() + " ready");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            statusLabel.setText("Error creating chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
