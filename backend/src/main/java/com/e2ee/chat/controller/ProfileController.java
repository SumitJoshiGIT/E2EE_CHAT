package com.e2ee.chat.controller;

import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(profileService.getProfile(userDetails.getUsername()));
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
} 