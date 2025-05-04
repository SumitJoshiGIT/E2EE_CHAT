package com.e2ee.chat.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "user_profiles")
public class UserProfile {
    @Id
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String status;
    private String publicKey;
    private String email;
} 