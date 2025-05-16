package com.e2ee.chat.frontend.service;

import com.e2ee.chat.frontend.model.UserProfile;
import com.e2ee.chat.frontend.crypto.CryptoUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static final String API_BASE_URL = "http://localhost:8080/api";
    private static final String LOGIN_URL = API_BASE_URL + "/auth/login";
    private static final String REGISTER_URL = API_BASE_URL + "/auth/register";
    private static final String PROFILE_URL = API_BASE_URL + "/profile";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private String authToken;
    private String currentUsername;
    private KeyPair keyPair;
    
    public AuthService() {
        // Generate RSA key pair for E2EE
        this.keyPair = CryptoUtils.generateKeyPair();
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
            
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }
    
    public boolean login(String username, String password) {
        try {
            // Create JSON data
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("password", password);
            
            String jsonData = objectMapper.writeValueAsString(data);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();
                
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                this.authToken = root.get("token").asText();
                this.currentUsername = username;
                
                // Store the public key in the user's profile
                updateUserPublicKey();
                
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean register(String username, String password, String email) {
        try {
            // Create JSON data
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("password", password);
            // Email isn't defined in AuthRequest, need to modify backend or remove this
            
            String jsonData = objectMapper.writeValueAsString(data);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REGISTER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();
                
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public UserProfile getUserProfile(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PROFILE_URL + "/" + username))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                UserProfile profile = new UserProfile();
                profile.setUsername(root.get("username").asText());
                profile.setDisplayName(root.has("displayName") ? root.get("displayName").asText() : "");
                profile.setBio(root.has("bio") ? root.get("bio").asText() : "");
                profile.setAvatarUrl(root.has("avatarUrl") ? root.get("avatarUrl").asText() : "");
                profile.setStatus(root.has("status") ? root.get("status").asText() : "");
                profile.setPublicKey(root.has("publicKey") ? root.get("publicKey").asText() : "");
                profile.setEmail(root.has("email") ? root.get("email").asText() : "");
                return profile;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void updateUserPublicKey() {
        try {
            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            
            Map<String, String> profileUpdate = new HashMap<>();
            profileUpdate.put("publicKey", publicKeyBase64);
            
            String jsonBody = objectMapper.writeValueAsString(profileUpdate);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PROFILE_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
                
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public String getCurrentUsername() {
        return currentUsername;
    }
    
    public KeyPair getKeyPair() {
        return keyPair;
    }
    
    /**
     * Search for users by username or display name
     * @param query The search query
     * @return Array of UserProfile objects matching the query
     */
    public UserProfile[] searchUsers(String query) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/users/search?q=" + 
                    java.net.URLEncoder.encode(query, StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Debug log the response body
            System.out.println("Search response: " + response.body());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(
                    response.body(), 
                    objectMapper.getTypeFactory().constructArrayType(UserProfile.class)
                );
            }
            return new UserProfile[0];
        } catch (Exception e) {
            e.printStackTrace();
            return new UserProfile[0];
        }
    }
    
    /**
     * Get a user profile by profileId
     * @param profileId The unique identifier of the user profile
     * @return UserProfile object or null if not found
     */
    public UserProfile getUserProfileById(String profileId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/profile/id/" + profileId))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Debug log
            System.out.println("Profile by ID response code: " + response.statusCode());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                UserProfile profile = objectMapper.readValue(response.body(), UserProfile.class);
                return profile;
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error fetching user profile by ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public void logout() {
        this.authToken = null;
        this.currentUsername = null;
    }

    public String getUserIdFromToken(String token) {
        try {
            // Split the token into parts (header, payload, signature)
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token");
            }

            // Decode the payload (second part of the token)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            // Parse the payload as JSON
            JsonNode payloadJson = objectMapper.readTree(payload);

            // Debug log the payload structure
            System.out.println("Decoded token payload: " + payloadJson.toPrettyString());

            // Extract and return the user ID
            if (payloadJson.has("sub")) {
                return payloadJson.get("sub").asText();
            } else if (payloadJson.has("userId")) {
                return payloadJson.get("userId").asText();
            } else {
                throw new IllegalArgumentException("The token does not contain a 'sub' or 'userId' field");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to extract user ID from token", e);
        }
    }

    public String getUserId() {
        if (authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Auth token is not available");
        }

        try {
            // Decode the JWT token (assuming it's a JWT)
            String[] parts = authToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid auth token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payloadJson = objectMapper.readTree(payload);

            // Debug log the payload structure
            System.out.println("Decoded token payload: " + payloadJson.toPrettyString());

            // Extract the user ID from the payload
            String profileId = payloadJson.get("sub").asText();
            if (profileId == null) {
                throw new IllegalArgumentException("The auth token does not contain a 'profileId' (sub) field");
            }

            return profileId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user ID from auth token", e);
        }
    }
}
