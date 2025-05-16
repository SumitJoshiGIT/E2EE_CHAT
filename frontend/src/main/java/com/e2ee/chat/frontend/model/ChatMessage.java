package com.e2ee.chat.frontend.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMessage {
    private String id;
    
    public enum MessageType {
        MESSAGE,
        JOIN,
        LEAVE,
        KEY_EXCHANGE,    // For exchanging encryption keys
        ENCRYPTED_CHAT,  // For encrypted messages
        USER_LIST,       // For broadcasting the list of online users
        USER_STATUS      // For user status updates
    }
    
    private MessageType type;
    private String senderId;
    private String content;           // Plain content or encrypted content
    private LocalDateTime timestamp;
    private String chatId;            // ID of the chat this message belongs to
    private boolean isOwn;            // Helper property for UI - whether the message is from current user
    private String clientTempId;      // Temporary ID for message deduplication

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.clientTempId = UUID.randomUUID().toString(); // Generate temp ID for all new messages
    }

    public ChatMessage(MessageType type, String senderId) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = LocalDateTime.now();
        this.clientTempId = UUID.randomUUID().toString(); // Generate temp ID for all new messages
    }

    public ChatMessage(MessageType type, String senderId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.clientTempId = UUID.randomUUID().toString(); // Generate temp ID for all new messages
    }

    public ChatMessage(MessageType type, String senderId, String recipient, String content) {
        this.type = type;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.clientTempId = UUID.randomUUID().toString(); // Generate temp ID for all new messages
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


  

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    
    public boolean isOwn() {
        return isOwn;
    }
    
    public void setOwn(boolean own) {
        isOwn = own;
    }
    
    public String getClientTempId() {
        return clientTempId;
    }
    
    public void setClientTempId(String clientTempId) {
        this.clientTempId = clientTempId;
    }
}
