package com.e2ee.chat.service;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.Message;
import com.e2ee.chat.model.UserProfile;

import java.util.List;

public interface ChatService {
    ChatMessage processMessage(ChatMessage message);
    void saveMessage(ChatMessage message);
    List<Chat> getChatsByOwner(String ownerId);
    List<Message> getMessagesByChatId(String chatId);
    Chat createChat(String ownerId, String targetUserId, String targetPublicKey);
    
    // New methods for WebSocket functionality
    List<UserProfile> searchUsers(String query, int limit);
    Chat createChatWithUser(String ownerId, String targetUserId);
    Message sendMessageToChat(String chatId, String senderId, String content, String messageType);
    Chat getChatById(String chatId);
    void updateChatPreview(String chatId, String previewText);
}