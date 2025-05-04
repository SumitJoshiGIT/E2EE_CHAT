package com.e2ee.chat.service.impl;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.service.ChatService;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    @Override
    public ChatMessage processMessage(ChatMessage message) {
        // TODO: Implement E2EE encryption here
        // For now, just return the message as is
        return message;
    }

    @Override
    public void saveMessage(ChatMessage message) {
        // TODO: Implement message persistence
    }
} 