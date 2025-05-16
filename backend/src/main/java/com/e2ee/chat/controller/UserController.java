package com.e2ee.chat.controller;

import com.e2ee.chat.model.User;
import com.e2ee.chat.service.UserService;
import com.e2ee.chat.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal; // Added for getting current user
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<List<String>> getAllUsers() {
        List<String> usernames = userService.getAllUsers().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usernames);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, String>>> searchUsers(@RequestParam(name = "q", required = false) String query, Principal principal) { // Added Principal
        String currentUsername = principal.getName(); // Get current username
        if (query == null || query.trim().isEmpty()) {
            // Pass currentUsername to service method, adjust if empty query should return all others or nothing
            return ResponseEntity.ok(profileService.searchUsers("", currentUsername)); 
        }
        return ResponseEntity.ok(profileService.searchUsers(query.trim(), currentUsername));
    }
    
    @GetMapping("/online")
    public ResponseEntity<List<Map<String, String>>> getOnlineUsers(Principal principal) { // Added Principal
        String currentUsername = principal.getName(); // Get current username
        // Get all profiles and filter by status, excluding the current user
        List<Map<String, String>> onlineUsers = profileService.searchUsers("", currentUsername).stream() // Pass currentUsername
            .filter(profile -> "Online".equalsIgnoreCase(profile.get("status")))
            // No need to filter out current user here as searchUsers already does it
            .toList();
        return ResponseEntity.ok(onlineUsers);
    }

    @PostMapping("/createChat")
    public ResponseEntity<String> createChat(@RequestBody Map<String, String> payload) {
        String ownerId = payload.get("ownerId");
        String targetProfileId = payload.get("targetProfileId");
        // TODO: Implement chat creation logic using ownerId and targetProfileId
        // For now, just return success for compliance
        return ResponseEntity.ok("Chat creation request received: ownerId=" + ownerId + ", targetProfileId=" + targetProfileId);
    }
}