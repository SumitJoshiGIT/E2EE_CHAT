package com.e2ee.chat.service;

import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;

import java.util.List;

public interface ProfileService {
    UserProfile getProfile(String username);
    UserProfile updateProfile(UserProfile profile);
    UserProfile updateStatus(String username, String status);
    UserProfile updatePublicKey(String username, String publicKey);
    UserProfile createProfile(User user);
    UserProfile updateBio(String username, String bio);
    List<UserProfile> searchUsers(String query);
}