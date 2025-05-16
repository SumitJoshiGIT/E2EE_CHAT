package com.e2ee.chat.service;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.Message;
import com.e2ee.chat.model.UserProfile;

import java.util.List;

public interface ChatService {
    // Remove ChatMessage from interface, use Message for all persistence
    Message processMessage(Message message);
    void saveMessage(Message message);
    List<Chat> getChatsByOwner(String ownerId);
    List<Message> getMessagesByChatId(String chatId);
    Chat createChat(String ownerId, String targetUserId, String targetPublicKey);
    
    // New methods for WebSocket functionality
    List<UserProfile> searchUsers(String query, int limit);
    Chat createChatWithUser(String ownerId, String targetUserId);
    Message sendMessageToChat(String chatId, String senderId, String content, String messageType);
    Chat getChatById(String chatId);
    void updateChatPreview(String chatId, String previewText);
    
    // Method to find chats between two users
    List<Chat> findChatsBetweenUsers(String user1, String user2);
    
    // Method to find all chats for a participant
    List<Chat> findChatsByParticipant(String userId);
    
    // Method to get unread messages for a user in a specific chat
    List<Message> getUnreadMessagesForUser(String chatId, String userId);
    
    // Method to mark messages as read by a user
    void markMessagesAsRead(String chatId, String userId, List<String> messageIds);
}