package com.e2ee.chat.config;

import com.e2ee.chat.model.User;
import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.repository.UserRepository;
import com.e2ee.chat.repository.MessageRepository;
import com.e2ee.chat.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, 
                                 MessageRepository messageRepository,
                                 UserProfileRepository profileRepository,
                                 PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("Initializing database...");
            
            // Test password encoding
            String testPassword = "password123";
            String encodedPassword = passwordEncoder.encode(testPassword);
            log.info("Password encoding test: raw={}, encoded={}", testPassword, encodedPassword);
            log.info("Password verification test: {}", passwordEncoder.matches(testPassword, encodedPassword));
            
            // Clear existing data
            userRepository.deleteAll();
            profileRepository.deleteAll();
            messageRepository.deleteAll();

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
            
            // Save profiles
            profileRepository.saveAll(profiles);
            
            // Create dummy messages
            List<ChatMessage> messages = Arrays.asList(
                createMessage("johndoe", "janesmith", "Hey Jane, how's the project going?"),
                createMessage("janesmith", "johndoe", "Going well! We're on track for the release."),
                createMessage("alicejohnson", "bobwilson", "Bob, can you help me with the data visualization?"),
                createMessage("bobwilson", "alicejohnson", "Sure, what do you need help with?"),
                createMessage("johndoe", "alicejohnson", "Alice, I love the new design!"),
                createMessage("alicejohnson", "johndoe", "Thanks John! Glad you like it."),
                createMessage("janesmith", "bobwilson", "Bob, when can we expect the analytics report?"),
                createMessage("bobwilson", "janesmith", "I'll have it ready by tomorrow morning.")
            );
            
            // Save messages
            messageRepository.saveAll(messages);
            
            log.info("Database initialization completed.");
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
    
    private ChatMessage createMessage(String sender, String recipient, String content) {
        ChatMessage message = new ChatMessage();
        message.setType(ChatMessage.MessageType.CHAT);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
} 