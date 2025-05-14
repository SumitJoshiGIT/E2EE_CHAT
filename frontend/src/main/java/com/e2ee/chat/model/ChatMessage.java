package com.e2ee.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        KEY_EXCHANGE,    // For exchanging encryption keys
        ENCRYPTED_CHAT,  // For encrypted messages
        USER_LIST       // For broadcasting the list of online users
    }

    private MessageType type;
    private String sender;
    private String recipient;
    private String content;           // Plain content or encrypted content
    private String encryptedKey;      // Encrypted AES key (used in KEY_EXCHANGE)
    private String publicKeyBase64;   // Base64 encoded public key (used in KEY_EXCHANGE)
    private String iv;                // Initialization vector for AES-GCM
    private String chatId;            // ID of the chat this message belongs to

    public ChatMessage(MessageType type, String sender) {
        this.type = type;
        this.sender = sender;
    }

    public ChatMessage(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    public ChatMessage(MessageType type, String sender, String recipient, String content) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }
} 