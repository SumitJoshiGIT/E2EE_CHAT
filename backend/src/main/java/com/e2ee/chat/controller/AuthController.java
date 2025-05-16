package com.e2ee.chat.controller;

import com.e2ee.chat.dto.AuthRequest;
import com.e2ee.chat.dto.AuthResponse;
import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.security.JwtTokenProvider;
import com.e2ee.chat.service.ProfileService;
import com.e2ee.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final ProfileService profileService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest loginRequest) {
        log.debug("Attempting login for user: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            log.debug("Authentication successful for user: {}", loginRequest.getUsername());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            User user = userService.findByUsername(loginRequest.getUsername());
            UserProfile profile;
            try {
                profile = profileService.getProfile(loginRequest.getUsername());
            } catch (RuntimeException e) {
                // If profile doesn't exist, create one
                profile = profileService.createProfile(user);
            }
            
            log.debug("Generated JWT token and retrieved/created profile for user: {}", loginRequest.getUsername());
            
            return ResponseEntity.ok(new AuthResponse(jwt, loginRequest.getUsername(), profile.getDisplayName()));
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw e;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest registerRequest) {
        log.debug("Attempting registration for user: {}", registerRequest.getUsername());

        User user = userService.createUser(registerRequest);
        UserProfile profile = profileService.createProfile(user); // Ensure profile is created

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                registerRequest.getUsername(),
                registerRequest.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        log.debug("Registration successful for user: {}", registerRequest.getUsername());
        return ResponseEntity.ok(new AuthResponse(jwt, user.getUsername(), profile.getDisplayName()));
    }
}