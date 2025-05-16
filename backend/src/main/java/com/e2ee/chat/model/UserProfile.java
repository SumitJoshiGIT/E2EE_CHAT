package com.e2ee.chat.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.ToString;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@ToString(exclude = "user")
@Document(collection = "user_profiles")
public class UserProfile {
    @Id
    private ObjectId id; // Unique identifier for the user profile
    private String username;
    @DBRef
    @JsonIgnore
    private User user; // Reference to the User entity
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String status;
    private String publicKey;
    private String email;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}