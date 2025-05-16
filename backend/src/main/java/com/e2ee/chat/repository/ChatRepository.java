package com.e2ee.chat.repository;

import com.e2ee.chat.model.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ChatRepository extends MongoRepository<Chat, String> {
    List<Chat> findByOwnerId(String ownerId);
    
    // Find chats where a user is a participant
    @Query("{ 'participants': { $in: [?0] } }")
    List<Chat> findByParticipantsContaining(String userId);
    
    // Find chat by both participants
    @Query("{ 'participants': { $all: [?0, ?1] } }")
    List<Chat> findByParticipantsContainingBoth(String user1, String user2);
}
