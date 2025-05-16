package com.e2ee.chat.service;

import com.e2ee.chat.dto.AuthRequest;
import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;

import java.util.List;

public interface UserService {
    User createUser(AuthRequest request);
    User findByUsername(String username);
    List<User> getAllUsers();
    List<UserProfile> searchUsers(String query);
}