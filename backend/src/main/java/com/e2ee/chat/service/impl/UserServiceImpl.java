package com.e2ee.chat.service.impl;

import com.e2ee.chat.dto.AuthRequest;
import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.repository.UserRepository;
import com.e2ee.chat.repository.UserProfileRepository;
import com.e2ee.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Override
    public List<UserProfile> searchUsers(String query) {
        String lowercaseQuery = query.toLowerCase();
        return userProfileRepository.findAll().stream()
                .filter(profile -> profile.getUsername().toLowerCase().contains(lowercaseQuery)
                        || (profile.getDisplayName() != null && profile.getDisplayName().toLowerCase().contains(lowercaseQuery)))
                .toList();
    }
}