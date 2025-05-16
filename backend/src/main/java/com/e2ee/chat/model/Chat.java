package com.e2ee.chat.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "chats")
public class Chat {
    @Id
    private String chatId;
    private List<String> participants = new ArrayList<>();
    private List<String> messageIds = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private String lastMessagePreview;
    private String targetUserId;
    private String targetPublicKey;
    private String ownerId;
}
