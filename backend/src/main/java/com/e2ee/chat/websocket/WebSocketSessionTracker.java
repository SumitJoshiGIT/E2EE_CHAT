package com.e2ee.chat.websocket;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to track user sessions and disconnections
 */
@Component
public class WebSocketSessionTracker {

    private final Map<String, String> sessionToUserMapping = new ConcurrentHashMap<>();
    private final Map<String, String> userToSessionMapping = new ConcurrentHashMap<>();

    /**
     * Register a new user session
     * @param sessionId WebSocket session ID
     * @param userId User ID
     */
    public void registerSession(String sessionId, String userId) {
        System.out.println("[DEBUG] WebSocketSessionTracker: Registering session " + sessionId + " for userId (principal): " + userId);
        sessionToUserMapping.put(sessionId, userId);
        userToSessionMapping.put(userId, sessionId);
        System.out.println("[DEBUG] WebSocketSessionTracker: Active sessions: " + sessionToUserMapping.size());
    }

    /**
     * Remove a session
     * @param sessionId WebSocket session ID
     * @return User ID associated with the session or null if not found
     */
    public String removeSession(String sessionId) {
        String userId = sessionToUserMapping.remove(sessionId);
        if (userId != null) {
            userToSessionMapping.remove(userId);
            System.out.println("[DEBUG] WebSocketSessionTracker: Removed session for userId (principal): " + userId);
        } else {
            System.out.println("[DEBUG] WebSocketSessionTracker: Attempted to remove unknown session: " + sessionId);
        }
        System.out.println("[DEBUG] WebSocketSessionTracker: Active sessions after removal: " + sessionToUserMapping.size());
        return userId;
    }

    /**
     * Get user ID for a session
     * @param sessionId WebSocket session ID
     * @return User ID or null if not found
     */
    public String getUserIdBySessionId(String sessionId) {
        return sessionToUserMapping.get(sessionId);
    }

    /**
     * Get session ID for a user
     * @param userId User ID
     * @return Session ID or null if not found
     */
    public String getSessionIdByUserId(String userId) {
        return userToSessionMapping.get(userId);
    }

    /**
     * Check if a user has an active session
     * @param userId User ID
     * @return true if the user has an active session
     */
    public boolean isUserActive(String userId) {
        return userToSessionMapping.containsKey(userId);
    }
}
