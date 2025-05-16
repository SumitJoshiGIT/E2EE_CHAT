package com.e2ee.chat.frontend.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Chat {
    private String chatId;
    private List<String> participants = new ArrayList<>();
    private List<ChatMessage> messages = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final StringProperty lastMessagePreview = new SimpleStringProperty();
    private String targetUserId;
    private String targetPublicKey;
    private String ownerId;
    private String sharedSecret; // Added for E2EE
    
    // JavaFX properties for UI binding
    private final StringProperty targetUsername = new SimpleStringProperty();
    private final BooleanProperty targetUserOnline = new SimpleBooleanProperty(false);
    
    public Chat() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.updatedAt = message.getTimestamp();
        this.setLastMessagePreview(message.getContent());
    }
    
    // Getters and Setters
    public String getChatId() {
        return chatId;
    }
    
    public String getId() {
        return chatId;  // Add this method for compatibility
    }
    
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    
    public List<String> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getLastMessagePreview() {
        return lastMessagePreview.get();
    }
    
    public StringProperty lastMessagePreviewProperty() {
        return lastMessagePreview;
    }
    
    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview.set(lastMessagePreview);
    }
    
    public String getTargetUserId() {
        return targetUserId;
    }
    
    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
        this.targetUsername.set(targetUserId);
    }
    
    public String getTargetPublicKey() {
        return targetPublicKey;
    }
    
    public void setTargetPublicKey(String targetPublicKey) {
        this.targetPublicKey = targetPublicKey;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getSharedSecret() { // Added for E2EE
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) { // Added for E2EE
        this.sharedSecret = sharedSecret;
    }
    
    public String getTargetUsername() {
        return targetUsername.get();
    }
    
    public StringProperty targetUsernameProperty() {
        return targetUsername;
    }
    
    public void setTargetUsername(String targetUsername) {
        this.targetUsername.set(targetUsername);
    }
    
    public boolean isTargetUserOnline() {
        return targetUserOnline.get();
    }
    
    public BooleanProperty targetUserOnlineProperty() {
        return targetUserOnline;
    }
    
    public void setTargetUserOnline(boolean targetUserOnline) {
        this.targetUserOnline.set(targetUserOnline);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return chatId != null && chatId.equals(chat.chatId);
    }

    @Override
    public int hashCode() {
        return chatId != null ? chatId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "chatId='" + chatId + '\'' +
                ", participants=" + participants +
                ", targetUserId='" + targetUserId + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", messagesCount=" + (messages != null ? messages.size() : 0) +
                ", lastMessagePreview='" + lastMessagePreview.get() + '\'' +
                '}';
    }
}
