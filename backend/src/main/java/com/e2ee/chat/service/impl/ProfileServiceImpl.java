package com.e2ee.chat.service.impl;

import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.repository.UserProfileRepository;
import com.e2ee.chat.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository profileRepository;

    @Override
    public UserProfile getProfile(String username) {
        return profileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }

    @Override
    public UserProfile updateProfile(UserProfile profile) {
        UserProfile existingProfile = getProfile(profile.getUsername());
        // Update only allowed fields
        existingProfile.setDisplayName(profile.getDisplayName());
        existingProfile.setEmail(profile.getEmail());
        existingProfile.setAvatarUrl(profile.getAvatarUrl());
        return profileRepository.save(existingProfile);
    }

    @Override
    public UserProfile updateStatus(String username, String status) {
        UserProfile profile = getProfile(username);
        profile.setStatus(status);
        return profileRepository.save(profile);
    }

    @Override
    public UserProfile updatePublicKey(String username, String publicKey) {
        UserProfile profile = getProfile(username);
        profile.setPublicKey(publicKey);
        return profileRepository.save(profile);
    }

    @Override
    public UserProfile createProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setUsername(user.getUsername());
        profile.setDisplayName(user.getUsername());
        profile.setStatus("Online");
        return profileRepository.save(profile);
    }
} 