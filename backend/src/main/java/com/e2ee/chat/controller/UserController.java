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
    public ResponseEntity<List<Map<String, String>>> searchUsers(@RequestParam(name = "q", required = false) String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(profileService.searchUsers(""));
        }
        return ResponseEntity.ok(profileService.searchUsers(query.trim()));
    }
    
    @GetMapping("/online")
    public ResponseEntity<List<Map<String, String>>> getOnlineUsers() {
        // Get all profiles and filter by status
        List<Map<String, String>> onlineUsers = profileService.searchUsers("").stream()
            .filter(profile -> "Online".equalsIgnoreCase(profile.get("status")))
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