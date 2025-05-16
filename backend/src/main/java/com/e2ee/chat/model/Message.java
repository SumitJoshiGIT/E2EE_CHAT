package com.e2ee.chat.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String messageId;
    private String chatId;
    private String senderId;
    private String content;
    private LocalDateTime timestamp;
    private String messageType = "TEXT"; // Default is TEXT, can be IMAGE, FILE, etc.
    private String fileName; // For file attachments
    private long fileSize; // For file attachments
    private String mimeType; // For file attachments
    private String status = "SENT"; // SENT, DELIVERED, READ
    private List<String> readBy = new ArrayList<>(); // List of user IDs who have read the message
    private String clientTempId; // Client-generated temporary ID for message deduplication
    
    // Constructor for text messages
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor for text messages
    public Message(String chatId, String senderId, String content) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.messageType = "TEXT";
    }
    
    // Constructor for file messages
    public Message(String chatId, String senderId, String content, String fileName, long fileSize, String mimeType) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content; // URL or path to the file
        this.timestamp = LocalDateTime.now();
        this.messageType = "FILE";
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }
}
