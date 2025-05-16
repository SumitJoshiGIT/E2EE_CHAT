package com.e2ee.chat.config;

import com.e2ee.chat.model.User;
import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.repository.UserRepository;
import com.e2ee.chat.repository.MessageRepository;
import com.e2ee.chat.repository.UserProfileRepository;
import com.e2ee.chat.repository.ChatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, 
                                 MessageRepository messageRepository,
                                 UserProfileRepository profileRepository,
                                 ChatRepository chatRepository, // Added ChatRepository
                                 PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("Checking if database needs initialization...");
            
            // Clear all chats
            log.info("Clearing all chats...");
            chatRepository.deleteAll();
            log.info("All chats cleared.");

            // Only initialize if the database is empty
            if (userRepository.count() == 0) {
                log.info("Database is empty. Initializing with sample data...");
                
                // Create dummy users
                List<User> users = Arrays.asList(
                    createUser("johndoe", "john@example.com", "password123", passwordEncoder),
                    createUser("janesmith", "jane@example.com", "password123", passwordEncoder),
                    createUser("alicejohnson", "alice@example.com", "password123", passwordEncoder),
                    createUser("bobwilson", "bob@example.com", "password123", passwordEncoder)
                );
                
                // Save users
                userRepository.saveAll(users);
                
                // Log user details for verification
                users.forEach(user -> {
                    log.info("Created user: username={}, password={}", user.getUsername(), user.getPassword());
                });
                
                // Create user profiles
                List<UserProfile> profiles = Arrays.asList(
                    createProfile("johndoe", "John Doe", "Software Engineer"),
                    createProfile("janesmith", "Jane Smith", "Product Manager"),
                    createProfile("alicejohnson", "Alice Johnson", "UX Designer"),
                    createProfile("bobwilson", "Bob Wilson", "Data Scientist")
                );
                
                // Set user reference in profiles before saving
                for (User user : users) {
                    UserProfile profile = profiles.stream()
                        .filter(p -> p.getUsername().equals(user.getUsername()))
                        .findFirst()
                        .orElse(null);
                    if (profile != null) {
                        profile.setUser(user);
                    }
                }

                // Save profiles
                profileRepository.saveAll(profiles);
                
                // Ensure profiles are created for all users
                users.forEach(user -> {
                    UserProfile profile = profileRepository.findByUsername(user.getUsername())
                        .orElseGet(() -> {
                            UserProfile newProfile = new UserProfile();
                            newProfile.setUsername(user.getUsername());
                            newProfile.setDisplayName(user.getUsername());
                            newProfile.setStatus("Online");
                            newProfile.setUser(user); // Set user reference here too
                            return profileRepository.save(newProfile);
                        });
                    user.setProfile(profile);
                    userRepository.save(user);
                });
                
                log.info("Database initialization completed.");
            } else {
                log.info("Database already contains data, skipping initialization.");
                // Clear all chat messages
                log.info("Clearing all chat messages...");
                messageRepository.deleteAll();
                log.info("All chat messages cleared.");
            }
        };
    }
    
    private User createUser(String username, String email, String password, PasswordEncoder encoder) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        String encodedPassword = encoder.encode(password);
        log.debug("Encoding password for user {}: raw={}, encoded={}", username, password, encodedPassword);
        user.setPassword(encodedPassword);
        user.setEnabled(true);
        user.setRole("ROLE_USER");
        return user;
    }
    
    private UserProfile createProfile(String username, String displayName, String bio) {
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setDisplayName(displayName);
        profile.setBio(bio);
        profile.setStatus("Online");
        return profile;
    }
    
}