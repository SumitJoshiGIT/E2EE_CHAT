package com.e2ee.chat.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.index.TextIndexDefinition.TextIndexDefinitionBuilder;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Query;

import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.Message;
import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.bson.Document;

/**
 * Configuration for MongoDB indices
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @Bean
    public CommandLineRunner createIndices() {
        return args -> {
            log.info("Creating MongoDB indices...");

            try {
                // Create indices for Chat collection
                mongoTemplate.indexOps(Chat.class).ensureIndex(new Index("ownerId", Sort.Direction.ASC));
                mongoTemplate.indexOps(Chat.class).ensureIndex(new Index("participants", Sort.Direction.ASC));
                mongoTemplate.indexOps(Chat.class).ensureIndex(new Index("updatedAt", Sort.Direction.DESC));
                
                // Create indices for Message collection
                mongoTemplate.indexOps(Message.class).ensureIndex(new Index("chatId", Sort.Direction.ASC));
                mongoTemplate.indexOps(Message.class).ensureIndex(new Index("senderId", Sort.Direction.ASC));
                mongoTemplate.indexOps(Message.class).ensureIndex(new Index("timestamp", Sort.Direction.DESC));
                
                // Compound index for messages in a chat with timestamp
                Document chatMessagesIndex = new Document();
                chatMessagesIndex.put("chatId", 1);
                chatMessagesIndex.put("timestamp", -1);
                mongoTemplate.indexOps(Message.class)
                    .ensureIndex(new CompoundIndexDefinition(chatMessagesIndex));
                
                // Create indices for ChatMessage collection
                mongoTemplate.indexOps(ChatMessage.class).ensureIndex(new Index("sender", Sort.Direction.ASC));
                mongoTemplate.indexOps(ChatMessage.class).ensureIndex(new Index("recipient", Sort.Direction.ASC));
                mongoTemplate.indexOps(ChatMessage.class).ensureIndex(new Index("chatId", Sort.Direction.ASC));
                mongoTemplate.indexOps(ChatMessage.class).ensureIndex(new Index("timestamp", Sort.Direction.DESC));
                
                // Create indices for UserProfile collection
                // Note: Skip creating unique index on username since it's already the @Id field
                
                // Text index for user search
                TextIndexDefinition textIndex = new TextIndexDefinitionBuilder()
                    .onField("username")
                    .onField("displayName") 
                    .build();
                mongoTemplate.indexOps(UserProfile.class).ensureIndex(textIndex);
                
                // Create indices for User collection (avoid _id field)
                mongoTemplate.indexOps(User.class).ensureIndex(new Index("username", Sort.Direction.ASC).unique());
                
                log.info("MongoDB indices created successfully");
            } catch (Exception e) {
                log.error("Error creating MongoDB indices: " + e.getMessage(), e);
                // Continue with application startup even if index creation fails
                // This prevents application from failing to start due to index issues
            }
        };
    }
}
