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
        CHAT,
        JOIN,
        LEAVE,
        KEY_EXCHANGE,    // For exchanging encryption keys
        ENCRYPTED_CHAT   // For encrypted messages
    }
    
    private MessageType type;
    private String sender;
    private String recipient;
    private String content;           // Plain content or encrypted content
    private String encryptedKey;      // Encrypted AES key (used in KEY_EXCHANGE)
    private String publicKeyBase64;   // Base64 encoded public key (used in KEY_EXCHANGE)
    private String iv;                // Initialization vector for AES-GCM
    private LocalDateTime timestamp;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(MessageType type, String sender) {
        this.type = type;
        this.sender = sender;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(MessageType type, String sender, String recipient, String content) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
} 