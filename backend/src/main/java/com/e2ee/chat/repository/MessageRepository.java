package com.e2ee.chat.repository;

import com.e2ee.chat.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByChatIdOrderByTimestampAsc(String chatId);
    // Add more methods as needed for chatId-based access
}