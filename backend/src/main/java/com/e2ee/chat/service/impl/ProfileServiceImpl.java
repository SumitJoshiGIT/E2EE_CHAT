package com.e2ee.chat.service.impl;

import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.repository.UserProfileRepository;
import com.e2ee.chat.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);
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
        if (user.getProfile() != null) {
            return user.getProfile();
        }
        UserProfile profile = new UserProfile();
        profile.setUsername(user.getUsername());
        profile.setDisplayName(user.getUsername());
        profile.setStatus("Online");
        profile.setUser(user); // <-- Set the user reference
        profile = profileRepository.save(profile);
        user.setProfile(profile); // Link the profile to the user
        return profile;
    }

    @Override
    public UserProfile updateBio(String username, String bio) {
        UserProfile profile = getProfile(username);
        profile.setBio(bio);
        return profileRepository.save(profile);
    }

    @Override
    public List<Map<String, String>> searchUsers(String query, String currentUsername) {
        logger.debug("searchUsers called with query: {} for user: {}", query, currentUsername);
        List<Map<String, String>> results = profileRepository.findAll().stream()
                .filter(profile -> !profile.getUsername().equals(currentUsername)) // Exclude current user
                .filter(profile -> profile.getUsername().contains(query) || (profile.getDisplayName() != null && profile.getDisplayName().contains(query)))
                .limit(4) // Limit to 4 results
                .map(profile -> Map.of(
                    "profileId", String.valueOf(profile.getId()),
                    "username", String.valueOf(profile.getUsername()),
                    "displayName", String.valueOf(profile.getDisplayName()),
                    "bio", String.valueOf(profile.getBio()),
                    "email", String.valueOf(profile.getEmail()),
                    "avatarUrl", String.valueOf(profile.getAvatarUrl()),
                    "publicKey", String.valueOf(profile.getPublicKey()),
                    "status", String.valueOf(profile.getStatus())
                ))
                .collect(Collectors.toList());
        logger.debug("searchUsers returning {} results: {}", results.size(), results);
        return results;
    }

    @Override
    public UserProfile getProfileById(String profileId) {
        ObjectId objectId = new ObjectId(profileId);
        return profileRepository.findById(objectId)
                .orElseThrow(() -> new RuntimeException("Profile not found for id: " + profileId));
    }
}