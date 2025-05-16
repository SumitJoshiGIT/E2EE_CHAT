package com.e2ee.chat.controller;

import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.e2ee.chat.model.Chat;
import com.e2ee.chat.service.ChatService;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(profileService.getProfile(userDetails.getUsername()));
    }

    @GetMapping(params = "username")
    public ResponseEntity<UserProfile> getProfileByUsername(@RequestParam String username) {
        return ResponseEntity.ok(profileService.getProfile(username));
    }

    @PutMapping
    public ResponseEntity<UserProfile> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserProfile profile) {
        profile.setUsername(userDetails.getUsername());
        return ResponseEntity.ok(profileService.updateProfile(profile));
    }

    @PutMapping("/status")
    public ResponseEntity<UserProfile> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String status) {
        return ResponseEntity.ok(profileService.updateStatus(userDetails.getUsername(), status));
    }

    @PutMapping("/public-key")
    public ResponseEntity<UserProfile> updatePublicKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String publicKey) {
        return ResponseEntity.ok(profileService.updatePublicKey(userDetails.getUsername(), publicKey));
    }

    @PutMapping("/bio")
    public ResponseEntity<UserProfile> updateBio(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String bio) {
        return ResponseEntity.ok(profileService.updateBio(userDetails.getUsername(), bio));
    }

    @GetMapping("/id/{profileId}")
    public ResponseEntity<UserProfile> getProfileById(@PathVariable String profileId) {
        try {
            UserProfile profile = profileService.getProfileById(profileId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Chat>> searchUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String query) {
        List<Map<String, String>> matchingUsers = profileService.searchUsers(userDetails.getUsername(), query);
        List<Chat> chats = matchingUsers.stream()
                .map(userMap -> chatService.createChat(
                        userDetails.getUsername(),
                        userMap.get("username"),
                        userMap.get("publicKey")))
                .toList();
        return ResponseEntity.ok(chats);
    }
}