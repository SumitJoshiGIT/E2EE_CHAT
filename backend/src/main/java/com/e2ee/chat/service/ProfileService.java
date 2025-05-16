package com.e2ee.chat.service;

import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;

import java.util.List;
import java.util.Map;

public interface ProfileService {
    UserProfile getProfile(String username);
    UserProfile getProfileById(String profileId);
    UserProfile updateProfile(UserProfile profile);
    UserProfile updateStatus(String username, String status);
    UserProfile updatePublicKey(String username, String publicKey);
    UserProfile createProfile(User user);
    UserProfile updateBio(String username, String bio);
    List<Map<String, String>> searchUsers(String query, String currentUsername);
}