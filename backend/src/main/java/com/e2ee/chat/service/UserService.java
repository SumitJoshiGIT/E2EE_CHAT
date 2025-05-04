package com.e2ee.chat.service;

import com.e2ee.chat.dto.AuthRequest;
import com.e2ee.chat.model.User;

public interface UserService {
    User createUser(AuthRequest request);
    User findByUsername(String username);
} 