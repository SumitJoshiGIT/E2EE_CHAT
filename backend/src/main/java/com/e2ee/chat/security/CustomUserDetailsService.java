package com.e2ee.chat.security;

import com.e2ee.chat.model.User;
import com.e2ee.chat.repository.UserRepository;
import com.e2ee.chat.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public UserDetails loadUserById(String profileId) throws UsernameNotFoundException {
        logger.debug("Attempting to load user by profileId: {}", profileId);
        ObjectId objectId = new ObjectId(profileId);
        var profileOpt = profileRepository.findById(objectId);
        logger.debug("Profile found: {}", profileOpt.isPresent() ? profileOpt.get() : "NOT FOUND");
        return profileOpt
                .map(profile -> profile.getUser())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with profile id: " + profileId));
    }
}