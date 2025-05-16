package com.e2ee.chat.repository;

import com.e2ee.chat.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<Message, String> {
    List<Message> findByChatId(String chatId);
}
