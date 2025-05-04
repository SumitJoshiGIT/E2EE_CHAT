package com.e2ee.chat.service;

import com.e2ee.chat.model.ChatMessage;

public interface ChatService {
    ChatMessage processMessage(ChatMessage message);
    void saveMessage(ChatMessage message);
} 