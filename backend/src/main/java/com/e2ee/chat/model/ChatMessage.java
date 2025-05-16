package com.e2ee.chat.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;
    
    public enum MessageType {
        MESSAGE,
        JOIN,
        LEAVE,
        KEY_EXCHANGE,    // For exchanging encryption keys
        ENCRYPTED_CHAT,  // For encrypted messages
        USER_LIST       // For broadcasting the list of online users
    }
    
    private MessageType type;
    private String senderId; // Replacing sender with senderId
    private String content;           // Plain content or encrypted content
    private LocalDateTime timestamp;
    private String chatId;            // ID of the chat this message belongs to
    private String clientTempId;      // Original client-side tempId for message deduplication

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(MessageType type, String senderId) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(MessageType type, String senderId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(MessageType type, String senderId, String chatId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.chatId = chatId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getClientTempId() {
        return clientTempId;
    }

    public void setClientTempId(String clientTempId) {
        this.clientTempId = clientTempId;
    }
}