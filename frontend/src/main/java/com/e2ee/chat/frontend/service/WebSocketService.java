package com.e2ee.chat.frontend.service;

import com.e2ee.chat.frontend.model.Chat;
import com.e2ee.chat.frontend.model.ChatMessage;
import com.e2ee.chat.frontend.model.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class WebSocketService {
    private static final String WEBSOCKET_URL = "http://localhost:8080/ws"; // Changed to http for SockJS
    private static final String SEND_ENDPOINT = "/app/chat.send";
    private static final String KEY_EXCHANGE_ENDPOINT = "/app/chat.keyExchange";
    private static final String CREATE_GROUP_ENDPOINT = "/app/chat.createGroup";
    private static final String PROMOTE_ADMIN_ENDPOINT = "/app/chat.promoteAdmin";
    private static final String REMOVE_PARTICIPANT_ENDPOINT = "/app/chat.removeParticipant";
    private static final String UPDATE_GROUP_NAME_ENDPOINT = "/app/chat.updateGroupName";

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String username;
    private String userId;
    private final ObjectMapper objectMapper;
    private boolean connected = false;

    private final Map<String, Chat> chats = new HashMap<>();
    private final List<UserProfile> onlineUsers = new ArrayList<>();

    // Message deduplication: track processed messages by their unique signature
    private final Map<String, Long> processedMessages = new HashMap<>();
    private static final long MESSAGE_DEDUP_TIMEOUT = 5000; // 5 seconds (reduced from 1 minute)

    private Consumer<ChatMessage> messageHandler;
    private Consumer<List<Chat>> chatListHandler;
    private Consumer<List<UserProfile>> userListHandler;

    public WebSocketService() {
        this.objectMapper = new ObjectMapper();
        // Configure Jackson to handle Java 8 time
        this.objectMapper.findAndRegisterModules();
    }

    public void connect(String username, String userId) {
        this.userId = userId;
        this.username = username;

        // Create a WebSocketClient that supports SockJS
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules(); // Optional, but safe
        converter.setObjectMapper(mapper);
        stompClient.setMessageConverter(converter);

        try {
            // Remove connectHeaders, not used with SockJS
            // Explicitly use userId parameter for server-side principal identification
            String fullUrl = WEBSOCKET_URL + "?userId=" + userId;

            System.out.println("\n========== WEBSOCKET ATTEMPTING CONNECTION ==========");
            System.out.println("Connection URL: " + fullUrl);
            System.out.println("Profile ID (Principal): " + userId);
            System.out.println("Display Name: " + username);
            System.out.println("Using 'userId' parameter for principal identification");

            // Connect without connectHeaders
            stompSession = stompClient.connect(fullUrl, new CustomStompSessionHandler()).get();
            connected = true;

            System.out.println("\n========== WEBSOCKET CONNECTION ESTABLISHED ==========");
            System.out.println("Successfully connected to WebSocket server with userId: " + userId);

            // Subscribe to personal message queue with a dedicated handler
            StompSession.Subscription messagesSub = stompSession.subscribe("/user/queue/messages",
                    new StompSessionHandler() {
                        @Override
                        public void afterConnected(@NonNull StompSession session,
                                @NonNull StompHeaders connectedHeaders) {
                            // Not needed - connection already established
                        }

                        @Override
                        public void handleException(@NonNull StompSession session, @Nullable StompCommand command,
                                @NonNull StompHeaders headers, @Nullable byte[] payload, @NonNull Throwable exception) {
                            System.err.println("Exception in message queue handler: " + exception.getMessage());
                            exception.printStackTrace();
                        }

                        @Override
                        public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                            System.err.println("Transport error in message queue handler: " + exception.getMessage());
                            exception.printStackTrace();
                        }

                        @Override
                        public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                            return Map.class;
                        }

                        @Override
                        public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                            System.out.println("\n========== PERSONAL MESSAGE QUEUE FRAME RECEIVED ==========");
                            System.out.println(
                                    "SPECIAL HANDLER: Received frame in personal queue with headers: " + headers);
                            System.out.println("SPECIAL HANDLER: Payload class: "
                                    + (payload != null ? payload.getClass().getName() : "null"));
                            System.out.println("SPECIAL HANDLER: Payload content raw: " + payload);

                            // Process the frame
                            processWebSocketFrame(headers, payload);
                        }
                    });
            System.out.println("Subscribed to personal message queue: " + messagesSub.getSubscriptionId());

            // Subscribe to public channel for online user list
            StompSession.Subscription publicSub = stompSession.subscribe("/topic/public",
                    new CustomStompSessionHandler());
            System.out.println("Subscribed to public channel: " + publicSub.getSubscriptionId());

            // Subscribe to chat updates channel
            StompSession.Subscription chatUpdatesSub = stompSession.subscribe("/user/queue/chat.updates",
                    new CustomStompSessionHandler());
            System.out.println("Subscribed to chat updates channel: " + chatUpdatesSub.getSubscriptionId());

            // Also subscribe to a potential alternative chat notifications channel
            StompSession.Subscription chatNotificationsSub = stompSession.subscribe("/user/queue/notifications",
                    new CustomStompSessionHandler());
            System.out.println("Subscribed to notifications channel: " + chatNotificationsSub.getSubscriptionId());

            // Additional subscription to a topic for chat events
            StompSession.Subscription chatTopicSub = stompSession.subscribe("/topic/chat.events",
                    new CustomStompSessionHandler());
            System.out.println("Subscribed to chat events topic: " + chatTopicSub.getSubscriptionId());

            // Add explicit subscription for chat list response
            StompSession.Subscription chatListSub = stompSession.subscribe("/user/queue/chat.list",
                    new StompSessionHandler() {
                        @Override
                        public void afterConnected(@NonNull StompSession session,
                                @NonNull StompHeaders connectedHeaders) {
                            // Not needed - connection already established
                        }

                        @Override
                        public void handleException(@NonNull StompSession session, @Nullable StompCommand command,
                                @NonNull StompHeaders headers, @Nullable byte[] payload, @NonNull Throwable exception) {
                            System.err.println("Exception in chat list handler: " + exception.getMessage());
                            exception.printStackTrace();
                        }

                        @Override
                        public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                            System.err.println("Transport error in chat list handler: " + exception.getMessage());
                            exception.printStackTrace();
                        }

                        @Override
                        public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                            return Map.class;
                        }

                        @Override
                        public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                            System.out.println("\n========== CHAT LIST SPECIFIC CHANNEL FRAME RECEIVED ==========");
                            System.out.println(
                                    "CHAT LIST HANDLER: Received frame in chat.list channel with headers: " + headers);
                            System.out.println("CHAT LIST HANDLER: Payload class: "
                                    + (payload != null ? payload.getClass().getName() : "null"));
                            System.out.println("CHAT LIST HANDLER: Payload content raw: " + payload);

                            // Process the frame with our main handler logic
                            processWebSocketFrame(headers, payload);
                        }
                    });
            System.out.println("Subscribed to chat list response channel: " + chatListSub.getSubscriptionId());

            // Additional chat list channel subscription
            StompSession.Subscription chatListAltSub = stompSession.subscribe("/user/" + userId + "/queue/chat.list",
                    new StompSessionHandler() {
                        @Override
                        public void afterConnected(@NonNull StompSession session,
                                @NonNull StompHeaders connectedHeaders) {
                            // Not needed - connection already established
                        }

                        @Override
                        public void handleException(@NonNull StompSession session, @Nullable StompCommand command,
                                @NonNull StompHeaders headers, @Nullable byte[] payload, @NonNull Throwable exception) {
                            System.err.println("Exception in alternative chat list handler: " + exception.getMessage());
                            exception.printStackTrace();
                        }

                        @Override
                        public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                            System.err.println(
                                    "Transport error in alternative chat list handler: " + exception.getMessage());
                            exception.printStackTrace();
                        }

                        @Override
                        public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                            return Map.class;
                        }

                        @Override
                        public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
                            System.out.println("\n========== ALTERNATIVE CHAT LIST CHANNEL FRAME RECEIVED ==========");
                            System.out.println(
                                    "ALT CHAT LIST HANDLER: Received frame in userId-specific chat.list channel with headers: "
                                            + headers);
                            System.out.println("ALT CHAT LIST HANDLER: Payload class: "
                                    + (payload != null ? payload.getClass().getName() : "null"));
                            System.out.println("ALT CHAT LIST HANDLER: Payload content raw: " + payload);

                            // Process the frame with our main handler logic
                            processWebSocketFrame(headers, payload);
                        }
                    });
            System.out.println(
                    "Subscribed to alternative chat list response channel: " + chatListAltSub.getSubscriptionId());

            // Request initial chat list
            requestChatList();
            System.out.println("Sent request for initial chat list");
            System.out.println("========== WEBSOCKET SETUP COMPLETE ==========\n");

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to connect to WebSocket server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to request chat list from server
    public void requestChatList() {
        if (!connected || stompSession == null) {
            System.err.println("Cannot request chat list - not connected to WebSocket server");
            return;
        }

        try {
            System.out.println("\n========== REQUESTING CHAT LIST ==========");
            System.out.println("Requesting chat list for userId (principal): " + userId);

            // Create headers with content type
            StompHeaders headers = new StompHeaders();
            headers.setDestination("/app/chat.getChats");
            headers.set("content-type", "application/json;charset=UTF-8");
            headers.set("userId", userId); // Add userId header for principal identification

            // Create payload with userId (not username)
            Map<String, Object> payload = new HashMap<>();
            payload.put("profileId", userId);
            payload.put("action", "GET_CHATS");
            payload.put("timestamp", System.currentTimeMillis());

            // Convert payload to JSON string
            String jsonPayload = objectMapper.writeValueAsString(payload);
            System.out.println("Sending payload as JSON string: " + jsonPayload);

            // Send the JSON string as payload
            stompSession.send(headers, jsonPayload.getBytes(StandardCharsets.UTF_8));

            // Subscribe specifically to the chat list response on a dedicated channel
            StompSession.Subscription chatListSub = stompSession.subscribe("/user/queue/chat.list",
                    new CustomStompSessionHandler());
            System.out.println("Subscribed to chat list response channel: " + chatListSub.getSubscriptionId());

            System.out.println("Chat list request sent to server");
            System.out.println("========== CHAT LIST REQUEST SENT ==========\n");
        } catch (Exception e) {
            System.err.println("Error requesting chat list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to manually create a chat when server doesn't respond
    public void manuallyCreateChat(String targetUserId) {
        // Generate a temporary chat ID
        String tempChatId = "temp_" + userId + "_" + targetUserId + "_" + System.currentTimeMillis();

        Chat newChat = new Chat();
        newChat.setChatId(tempChatId);

        List<String> participants = new ArrayList<>();
        participants.add(userId); // Using userId instead of username
        participants.add(targetUserId);
        newChat.setParticipants(participants);

        newChat.setTargetUserId(targetUserId);
        newChat.setOwnerId(userId); // Using userId instead of username

        // Do NOT add to chats map or update UI here!
        System.out.println(
                "Manually created temporary chat with ID: " + tempChatId + " with target user: " + targetUserId);

        // Request updated chat list from server to sync
        requestChatList();
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            connected = false;
        }
    }

    public String sendMessage(String chatId, String content) {
        if (!connected || stompSession == null) {
            System.err.println("[DEBUG] sendMessage: Not connected to WebSocket server");
            return null;
        }
        System.out.println("[DEBUG] sendMessage: Preparing to send message");
        System.out.println("[DEBUG] sendMessage: Sender (username): " + username);
        System.out.println("[DEBUG] sendMessage: Sender (userId): " + userId);
        System.out.println("[DEBUG] sendMessage: chatId: " + chatId);
        System.out.println("[DEBUG] sendMessage: Content: " + content);
        System.out.println("[DEBUG] sendMessage: Endpoint: " + SEND_ENDPOINT);

        // Get the chat from local storage to check if it's a group chat
        Chat chat = chats.get(chatId);
        boolean isGroupChat = chat != null && "group".equalsIgnoreCase(chat.getChatType());

        if (isGroupChat && chat != null) {
            System.out.println("[DEBUG] sendMessage: Sending to group chat: " + chat.getGroupName());
        }

        // Generate a client-side temporary ID for deduplication
        String clientTempId = UUID.randomUUID().toString();

        // Create a map with field names that match the backend model
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", UUID.randomUUID().toString()); // Using UUID instead of id
        messageMap.put("messageType", "TEXT"); // Using messageType instead of type
        messageMap.put("senderId", userId);
        messageMap.put("chatId", chatId);
        messageMap.put("content", content);
        messageMap.put("timestamp", LocalDateTime.now());
        messageMap.put("clientTempId", clientTempId); // Add the client temp ID

        // Add chat type for proper handling on the backend
        if (isGroupChat) {
            messageMap.put("chatType", "group");
        }

        try {
            System.out.println("[DEBUG] sendMessage: Message map: " + messageMap);
            String jsonPayload = objectMapper.writeValueAsString(messageMap);
            System.out.println("[DEBUG] sendMessage: JSON payload: " + jsonPayload);
            stompSession.send(SEND_ENDPOINT, jsonPayload.getBytes(StandardCharsets.UTF_8));
            System.out.println("[DEBUG] sendMessage: Message sent successfully");

            // For group chats, request an updated chat list after sending
            if (isGroupChat) {
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Short delay to ensure message is processed
                        requestChatList();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            return clientTempId;
        } catch (Exception e) {
            System.err.println("[DEBUG] sendMessage: Error serializing or sending message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String sendEncryptedMessage(String chatId, String encryptedContent, String encryptedKey, String iv) {
        if (!connected || stompSession == null) {
            System.err.println("[DEBUG] sendEncryptedMessage: Not connected to WebSocket server");
            return null;
        }
        System.out.println("[DEBUG] sendEncryptedMessage: Preparing to send encrypted message");
        System.out.println("[DEBUG] sendEncryptedMessage: Sender (username): " + username);
        System.out.println("[DEBUG] sendEncryptedMessage: Sender (userId): " + userId);
        System.out.println("[DEBUG] sendEncryptedMessage: chatId: " + chatId);
        System.out.println("[DEBUG] sendEncryptedMessage: EncryptedContent: " + encryptedContent);
        System.out.println("[DEBUG] sendEncryptedMessage: EncryptedKey: " + encryptedKey);
        System.out.println("[DEBUG] sendEncryptedMessage: IV: " + iv);
        System.out.println("[DEBUG] sendEncryptedMessage: Endpoint: " + SEND_ENDPOINT);

        // Generate a client-side temporary ID for deduplication
        String clientTempId = UUID.randomUUID().toString();

        // Create a map with field names that match the backend model
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", UUID.randomUUID().toString()); // Using UUID instead of id
        messageMap.put("messageType", "ENCRYPTED_CHAT"); // Using messageType instead of type
        messageMap.put("senderId", userId);
        messageMap.put("chatId", chatId);
        messageMap.put("content", encryptedContent);
        messageMap.put("timestamp", LocalDateTime.now());
        // Add encryption-related fields if the backend model supports them
        messageMap.put("encryptedKey", encryptedKey);
        messageMap.put("iv", iv);
        messageMap.put("clientTempId", clientTempId); // Add the client temp ID

        try {
            System.out.println("[DEBUG] sendEncryptedMessage: Message map: " + messageMap);
            String jsonPayload = objectMapper.writeValueAsString(messageMap);
            System.out.println("[DEBUG] sendEncryptedMessage: JSON payload: " + jsonPayload);
            stompSession.send(SEND_ENDPOINT, jsonPayload.getBytes(StandardCharsets.UTF_8));
            System.out.println("[DEBUG] sendEncryptedMessage: Encrypted message sent successfully");
            return clientTempId;
        } catch (Exception e) {
            System.err.println(
                    "[DEBUG] sendEncryptedMessage: Error serializing or sending encrypted message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void sendKeyExchange(String chatId, String publicKey) {
        if (!connected || stompSession == null) {
            System.err.println("[DEBUG] sendKeyExchange: Not connected to WebSocket server");
            return;
        }
        System.out.println("[DEBUG] sendKeyExchange: Preparing to send key exchange");
        System.out.println("[DEBUG] sendKeyExchange: Sender (username): " + username);
        System.out.println("[DEBUG] sendKeyExchange: Sender (userId): " + userId);
        System.out.println("[DEBUG] sendKeyExchange: chatId: " + chatId);
        System.out.println("[DEBUG] sendKeyExchange: PublicKey: " + publicKey);
        System.out.println("[DEBUG] sendKeyExchange: Endpoint: " + KEY_EXCHANGE_ENDPOINT);

        // Create a map with field names that match the backend model
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", UUID.randomUUID().toString()); // Using UUID instead of id
        messageMap.put("messageType", "KEY_EXCHANGE"); // Using messageType instead of type
        messageMap.put("senderId", userId);
        messageMap.put("chatId", chatId);
        messageMap.put("content", publicKey); // Store public key in content field
        messageMap.put("timestamp", LocalDateTime.now());

        try {
            System.out.println("[DEBUG] sendKeyExchange: Message map: " + messageMap);
            String jsonPayload = objectMapper.writeValueAsString(messageMap);
            System.out.println("[DEBUG] sendKeyExchange: JSON payload: " + jsonPayload);
            stompSession.send(KEY_EXCHANGE_ENDPOINT, jsonPayload.getBytes(StandardCharsets.UTF_8));
            System.out.println("[DEBUG] sendKeyExchange: Key exchange message sent successfully");
        } catch (Exception e) {
            System.err.println(
                    "[DEBUG] sendKeyExchange: Error serializing or sending key exchange message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendCreateChatMessage(Map<String, Object> payload) {
        if (!connected || stompSession == null) {
            System.err.println("Cannot send create chat message - not connected to WebSocket server");
            return;
        }

        try {
            System.out.println("[DEBUG] sendCreateChatMessage called. Payload: " + payload);

            // Create headers with content type
            StompHeaders headers = new StompHeaders();
            headers.setDestination("/app/chat.createChatRequest");
            headers.set("content-type", "application/json;charset=UTF-8");

            // Convert payload to JSON string
            String jsonPayload = objectMapper.writeValueAsString(payload);
            System.out.println("[DEBUG] Sending create chat JSON payload: " + jsonPayload);

            // Send with JSON string as payload
            stompSession.send(headers, jsonPayload.getBytes(StandardCharsets.UTF_8));

            System.out.println("Create chat request sent to server");
            System.out.println("========== CREATE CHAT REQUEST SENT ==========\n");
        } catch (Exception e) {
            System.err.println("Error sending create chat message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendCreateChatMessage(String targetUserId) {
        if (!connected || stompSession == null) {
            System.err.println("Cannot send create chat message - not connected to WebSocket server");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ownerId", userId); // Use userId instead of username
            payload.put("targetUserId", targetUserId); // Use targetUserId directly

            System.out.println("\n========== SENDING CREATE CHAT REQUEST ==========");
            System.out.println("Sending create chat request between " + userId + " and " + targetUserId);
            System.out.println("Request payload: " + payload);

            // Create headers with content type
            StompHeaders headers = new StompHeaders();
            headers.setDestination("/app/chat.createChatRequest");
            headers.set("content-type", "application/json;charset=UTF-8");

            // Convert payload to JSON string
            String jsonPayload = objectMapper.writeValueAsString(payload);
            System.out.println("Sending payload as JSON string: " + jsonPayload);

            // Send with JSON string as payload
            stompSession.send(headers, jsonPayload.getBytes(StandardCharsets.UTF_8));

            System.out.println("Create chat request sent to server");
            System.out.println("========== CREATE CHAT REQUEST SENT ==========\n");
        } catch (Exception e) {
            System.err.println("Error sending create chat message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createGroupChatWS(String ownerId, String groupName, List<String> participants) {
        try {
            // Log what we're about to do
            System.out.println("\n========== CREATING GROUP CHAT ==========");
            System.out.println("Owner: " + ownerId);
            System.out.println("Group Name: " + groupName);
            System.out.println("Participants: " + participants);

            Map<String, Object> payload = new HashMap<>();
            payload.put("ownerId", ownerId);
            payload.put("groupName", groupName);
            payload.put("participants", participants);
            // Explicitly add chatType field
            payload.put("chatType", "group");

            if (stompSession != null && stompSession.isConnected()) {
                // Send the payload as a raw Map, not as a JSON string
                stompSession.send(CREATE_GROUP_ENDPOINT, payload);
                System.out.println("Group chat creation request sent");

                // Request an updated chat list after a short delay
                new Thread(() -> {
                    try {
                        // Wait 1 second to give the server time to process
                        Thread.sleep(1000);
                        // Request updated chat list
                        requestChatList();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                System.err.println("Cannot create group chat - not connected to WebSocket server");
            }
            System.out.println("========== GROUP CHAT CREATION REQUEST SENT ==========\n");
        } catch (Exception e) {
            System.err.println("Error creating group chat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Promotes a user to admin in a group chat
     * @param chatId The ID of the group chat
     * @param userId The ID of the user to promote
     */
    public void promoteToAdmin(String chatId, String userId) {
        if (!isConnected() || chatId == null || userId == null) {
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("chatId", chatId);
        payload.put("userId", userId);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            stompSession.send(PROMOTE_ADMIN_ENDPOINT, jsonPayload.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent promote admin request for user " + userId + " in chat " + chatId);
        } catch (Exception e) {
            System.err.println("Error promoting user to admin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes a participant from a group chat
     * @param chatId The ID of the group chat
     * @param userId The ID of the user to remove
     */
    public void removeFromGroup(String chatId, String userId) {
        if (!isConnected() || chatId == null || userId == null) {
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("chatId", chatId);
        payload.put("userId", userId);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            stompSession.send(REMOVE_PARTICIPANT_ENDPOINT, jsonPayload.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent remove participant request for user " + userId + " from chat " + chatId);
        } catch (Exception e) {
            System.err.println("Error removing participant: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the name of a group chat
     * @param chatId The ID of the group chat
     * @param newName The new name for the group
     */
    public void updateGroupName(String chatId, String newName) {
        if (!isConnected() || chatId == null || newName == null) {
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("chatId", chatId);
        payload.put("newName", newName);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            stompSession.send(UPDATE_GROUP_NAME_ENDPOINT, jsonPayload.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent update group name request for chat " + chatId + " to " + newName);
        } catch (Exception e) {
            System.err.println("Error updating group name: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setMessageHandler(Consumer<ChatMessage> handler) {
        this.messageHandler = handler;
    }

    public void setChatListHandler(Consumer<List<Chat>> handler) {
        this.chatListHandler = handler;
    }

    public void setUserListHandler(Consumer<List<UserProfile>> handler) {
        this.userListHandler = handler;
    }

    public boolean isConnected() {
        return connected;
    }

    public Map<String, Chat> getChats() {
        return chats;
    }

    public List<UserProfile> getOnlineUsers() {
        return onlineUsers;
    }

    public void setAuthToken(String authToken) {
        // For WebSocket connections that require token-based authentication
        // This would be used when connecting to the WebSocket in a secure environment
        // But in this implementation we're using simple username authentication
    }

    // Helper method to process WebSocket frames directly from inner anonymous
    // classes
    private void processWebSocketFrame(StompHeaders headers, Object payload) {
        // Call our handler's implementation directly
        if (stompSession != null && stompSession.isConnected()) {
            try {
                // Find the custom handler instance that's currently registered
                CustomStompSessionHandler handler = findActiveSessionHandler();
                if (handler != null) {
                    // Call its handleFrame method directly
                    handler.handleFrame(headers, payload);
                } else {
                    // Fallback: Create a new handler just for processing this frame
                    new CustomStompSessionHandler().handleFrame(headers, payload);
                }
            } catch (Exception e) {
                System.err.println("Error processing frame via helper: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Find the current active session handler
    private CustomStompSessionHandler findActiveSessionHandler() {
        // In this basic implementation, we just create a new one
        // In a more complex app, you might want to maintain a reference to the active
        // handler
        return new CustomStompSessionHandler();
    }

    public String sendFileMessage(String chatId, String fileUrl, String fileName, long fileSize, String mimeType) {
        if (!connected || stompSession == null) {
            System.err.println("[DEBUG] sendFileMessage: Not connected to WebSocket server");
            return null;
        }
        System.out.println("[DEBUG] sendFileMessage: Preparing to send file message");
        System.out.println("[DEBUG] sendFileMessage: Sender (username): " + username);
        System.out.println("[DEBUG] sendFileMessage: Sender (userId): " + userId);
        System.out.println("[DEBUG] sendFileMessage: chatId: " + chatId);
        System.out.println("[DEBUG] sendFileMessage: File URL: " + fileUrl);
        System.out.println("[DEBUG] sendFileMessage: File Name: " + fileName);
        System.out.println("[DEBUG] sendFileMessage: File Size: " + fileSize);
        System.out.println("[DEBUG] sendFileMessage: MIME Type: " + mimeType);
        System.out.println("[DEBUG] sendFileMessage: Endpoint: " + SEND_ENDPOINT);

        // Get the chat from local storage to check if it's a group chat
        Chat chat = chats.get(chatId);
        boolean isGroupChat = chat != null && "group".equalsIgnoreCase(chat.getChatType());

        // Generate a client-side temporary ID for deduplication
        String clientTempId = UUID.randomUUID().toString();

        // Create a map with field names that match the backend model
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", UUID.randomUUID().toString());
        messageMap.put("messageType", "FILE");
        messageMap.put("senderId", userId);
        messageMap.put("chatId", chatId);
        messageMap.put("content", fileUrl);
        messageMap.put("fileName", fileName);
        messageMap.put("fileSize", fileSize);
        messageMap.put("mimeType", mimeType);
        messageMap.put("timestamp", LocalDateTime.now());
        messageMap.put("clientTempId", clientTempId);
        if (isGroupChat) {
            messageMap.put("chatType", "group");
        }
        try {
            String jsonPayload = objectMapper.writeValueAsString(messageMap);
            stompSession.send(SEND_ENDPOINT, jsonPayload.getBytes(StandardCharsets.UTF_8));
            System.out.println("[DEBUG] sendFileMessage: File message sent successfully");
            return clientTempId;
        } catch (Exception e) {
            System.err.println("[DEBUG] sendFileMessage: Error serializing or sending file message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private class CustomStompSessionHandler implements StompSessionHandler {
        @Override
        public void afterConnected(@NonNull StompSession session, @NonNull StompHeaders connectedHeaders) {
            System.out.println("\n========== WEBSOCKET CONNECTION CALLBACK ==========");
            System.out.println("Connected to WebSocket server");
            System.out.println("Session ID: " + session.getSessionId());
            System.out.println("Session connected: " + session.isConnected());
            System.out.println("Connection headers: " + connectedHeaders);

            // Log principal information if available in the headers
            if (connectedHeaders.containsKey("user-name")) {
                System.out.println("Principal (user-name) in headers: " + connectedHeaders.getFirst("user-name"));
            }

            // Also log our client-side principal identifier that we sent
            System.out.println("Client-side principal (userId): " + userId);

            System.out.println("========== CONNECTION CALLBACK COMPLETE ==========\n");
        }

        @Override
        public void handleException(@NonNull StompSession session, @Nullable StompCommand command,
                @NonNull StompHeaders headers, @Nullable byte[] payload, @NonNull Throwable exception) {
            System.err.println("Exception in WebSocket session: " + exception.getMessage());
            exception.printStackTrace();
            connected = false;
        }

        @Override
        public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
            System.err.println("Transport error in WebSocket session: " + exception.getMessage());
            exception.printStackTrace();
            connected = false;
        }

        @Override
        public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
            return Map.class; // We'll decode JSON manually for flexibility
        }

        @Override
        public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
            System.out.println("[DEBUG] handleFrame called. Headers: " + headers + ", Payload class: "
                    + (payload != null ? payload.getClass().getName() : "null") + ", Payload: " + payload);
            try {
                System.out.println("\n========== WEBSOCKET FRAME RECEIVED ==========");
                System.out.println("WEBSOCKET DEBUG: Received frame with headers: " + headers);
                System.out.println("WEBSOCKET DEBUG: Destination: " + headers.getDestination());

                // Log any user/principal information in the headers
                if (headers.containsKey("user-name")) {
                    System.out.println("WEBSOCKET DEBUG: User-Name in headers: " + headers.getFirst("user-name"));
                }
                if (headers.containsKey("principal")) {
                    System.out.println("WEBSOCKET DEBUG: Principal in headers: " + headers.getFirst("principal"));
                }
                if (headers.containsKey("simpUser")) {
                    System.out.println("WEBSOCKET DEBUG: SimpUser in headers: " + headers.getFirst("simpUser"));
                }

                // Check additional header patterns that might contain principal information
                headers.forEach((key, values) -> {
                    if (key.toLowerCase().contains("user") ||
                            key.toLowerCase().contains("principal") ||
                            key.toLowerCase().contains("auth")) {
                        System.out.println("WEBSOCKET DEBUG: Potential principal info - " + key + ": " + values);
                    }
                });

                // Log local principal identifier for comparison
                System.out.println("WEBSOCKET DEBUG: Current local userId (principal): " + userId);

                System.out.println(
                        "WEBSOCKET DEBUG: Payload class: " + (payload != null ? payload.getClass().getName() : "null"));
                System.out.println("WEBSOCKET DEBUG: Payload content raw: " + payload);
                System.out.println("WEBSOCKET DEBUG: Thread ID: " + Thread.currentThread().threadId());
                System.out.println("WEBSOCKET DEBUG: Is connected: " + connected + ", Session connected: "
                        + (stompSession != null ? stompSession.isConnected() : "session null"));

                // --- REPLACED: Enhanced catch-all for non-Map payloads ---
                if (!(payload instanceof Map)) {
                    System.out.println("[DEBUG] Payload is not a Map. Actual type: "
                            + (payload != null ? payload.getClass().getName() : "null"));
                    String asString = null;
                    if (payload instanceof byte[]) {
                        asString = new String((byte[]) payload, java.nio.charset.StandardCharsets.UTF_8);
                        System.out.println("[DEBUG] Payload as UTF-8 string: " + asString);
                    } else if (payload instanceof String) {
                        asString = (String) payload;
                        System.out.println("[DEBUG] Payload as String: " + asString);
                    }
                    if (asString != null) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsed = objectMapper.readValue(asString, Map.class);
                            System.out.println("[DEBUG] Parsed JSON as Map: " + parsed);
                            if (parsed.containsKey("chats")) {
                                System.out.println(
                                        "[DEBUG] Detected chat list in parsed JSON, calling handleChatListMessage");
                                handleChatListMessage(parsed);
                            } else if (parsed.containsKey("type")) {
                                System.out.println("[DEBUG] Detected type in parsed JSON: " + parsed.get("type"));
                                // Handle GROUP_CHAT_CREATED events
                                if ("GROUP_CHAT_CREATED".equals(parsed.get("type"))) {
                                    System.out.println(
                                            "[DEBUG] Detected GROUP_CHAT_CREATED event, calling handleChatCreatedMessage");
                                    handleChatCreatedMessage(parsed);
                                }
                                // Handle other types as needed
                            } else {
                                System.out.println("[DEBUG] Parsed JSON does not match known message types: " + parsed);
                            }
                        } catch (Exception parseEx) {
                            System.err.println(
                                    "[DEBUG] Failed to parse non-map payload as JSON: " + parseEx.getMessage());
                            parseEx.printStackTrace();
                        }
                    } else if (payload != null) {
                        System.out.println("[DEBUG] Payload toString(): " + payload.toString());
                    }
                }
                // --- END REPLACED ---

                if (payload instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageMap = (Map<String, Object>) payload;

                    // Log all received messages for debugging
                    System.out.println("Received WebSocket message: " + messageMap);
                    System.out.println("WEBSOCKET DEBUG: Message keys: " + messageMap.keySet());

                    // Convert the map to JSON and then to the appropriate model
                    String json = objectMapper.writeValueAsString(messageMap);
                    System.out.println("WEBSOCKET DEBUG: JSON representation: " + json);

                    // Explicitly check for chat list response first
                    if (messageMap.containsKey("chats") ||
                            (messageMap.containsKey("response") && messageMap.containsKey("action")
                                    && "GET_CHATS".equals(messageMap.get("action")))) {
                        System.out.println("WEBSOCKET DEBUG: Detected chat list response");
                        handleChatListMessage(messageMap);
                        return;
                    }

                    if (messageMap.containsKey("type")) {
                        String type = (String) messageMap.get("type");
                        System.out.println("WEBSOCKET DEBUG: Message type: " + type);

                        switch (type) {
                            case "KEY_EXCHANGE":
                                System.out.println("WEBSOCKET DEBUG: Processing chat/key message");
                                ChatMessage chatMessage = objectMapper.readValue(json, ChatMessage.class);
                                handleChatMessage(chatMessage);
                                break;
                            case "USER_LIST":
                                System.out.println("WEBSOCKET DEBUG: Processing USER_LIST message");
                                handleUserListMessage(messageMap);
                                break;
                            case "USER_STATUS":
                                System.out.println("WEBSOCKET DEBUG: Processing USER_STATUS message");
                                handleUserStatusMessage(messageMap);
                                break;
                            case "ADMIN_ACTION":
                                System.out.println("WEBSOCKET DEBUG: Processing ADMIN_ACTION response");
                                handleAdminActionResponse(messageMap);
                                break;
                            case "CHAT_CREATED":
                            case "GROUP_CHAT_CREATED":
                                // Unified handling for any type of chat creation
                                System.out.println("WEBSOCKET DEBUG: Processing chat creation message: " + type);
                                handleChatCreatedMessage(messageMap);
                                break;
                            case "MESSAGE":
                                System.out.println("WEBSOCKET DEBUG: Processing MESSAGE type message");
                                // This is a new message sent to a chat
                                if (messageMap.containsKey("chatId")) {
                                    String chatId = (String) messageMap.get("chatId");

                                    Map<String, Object> messageData = (Map<String, Object>) messageMap;

                                    ChatMessage message = new ChatMessage();
                                    message.setType(ChatMessage.MessageType.MESSAGE);
                                    message.setSenderId((String) messageData.get("senderId"));
                                    message.setContent((String) messageData.get("content"));
                                    message.setChatId(chatId);

                                    // Set message ID if available
                                    if (messageData.containsKey("messageId")) {
                                        message.setId((String) messageData.get("messageId"));
                                    }

                                    // Set timestamp if available
                                    if (messageData.containsKey("timestamp")) {
                                        Object timestampObj = messageData.get("timestamp");
                                        if (timestampObj instanceof LocalDateTime) {
                                            message.setTimestamp((LocalDateTime) timestampObj);
                                        } else if (timestampObj instanceof String) {
                                            try {
                                                message.setTimestamp(LocalDateTime.parse((String) timestampObj));
                                            } catch (Exception e) {
                                                message.setTimestamp(LocalDateTime.now());
                                            }
                                        } else {
                                            message.setTimestamp(LocalDateTime.now());
                                        }
                                    } else {
                                        message.setTimestamp(LocalDateTime.now());
                                    }

                                    // Extract and set clientTempId for deduplication
                                    if (messageData.containsKey("clientTempId")) {
                                        message.setClientTempId((String) messageData.get("clientTempId"));
                                        System.out.println(
                                                "WEBSOCKET DEBUG: Setting clientTempId: " + message.getClientTempId());
                                    }

                                    // Check if this message was sent by the current user
                                    if (message.getSenderId() != null && message.getSenderId().equals(userId)) {
                                        message.setOwn(true);
                                    }

                                    handleChatMessage(message);
                                }
                                break;
                            case "ENCRYPTED_CHAT":
                                System.out.println("WEBSOCKET DEBUG: Processing ENCRYPTED_CHAT type message");
                                // This is a new encrypted message sent to a chat
                                if (messageMap.containsKey("chatId")) {
                                    String chatId = (String) messageMap.get("chatId");

                                    Map<String, Object> messageData = (Map<String, Object>) messageMap;

                                    ChatMessage message = new ChatMessage();
                                    message.setType(ChatMessage.MessageType.ENCRYPTED_CHAT);
                                    message.setSenderId((String) messageData.get("senderId"));
                                    message.setContent((String) messageData.get("content"));
                                    message.setChatId(chatId);

                                    // Set message ID if available
                                    if (messageData.containsKey("messageId")) {
                                        message.setId((String) messageData.get("messageId"));
                                    }

                                    // Set timestamp if available
                                    if (messageData.containsKey("timestamp")) {
                                        Object timestampObj = messageData.get("timestamp");
                                        if (timestampObj instanceof LocalDateTime) {
                                            message.setTimestamp((LocalDateTime) timestampObj);
                                        } else if (timestampObj instanceof String) {
                                            try {
                                                message.setTimestamp(LocalDateTime.parse((String) timestampObj));
                                            } catch (Exception e) {
                                                message.setTimestamp(LocalDateTime.now());
                                            }
                                        } else {
                                            message.setTimestamp(LocalDateTime.now());
                                        }
                                    } else {
                                        message.setTimestamp(LocalDateTime.now());
                                    }

                                    // Extract and set clientTempId for deduplication
                                    if (messageData.containsKey("clientTempId")) {
                                        message.setClientTempId((String) messageData.get("clientTempId"));
                                        System.out
                                                .println("WEBSOCKET DEBUG: Setting clientTempId for encrypted message: "
                                                        + message.getClientTempId());
                                    }

                                    // Check if this message was sent by the current user
                                    if (message.getSenderId() != null && message.getSenderId().equals(userId)) {
                                        message.setOwn(true);
                                    }

                                    handleChatMessage(message);
                                }
                                break;
                            case "CHAT_LIST":
                                System.out.println("WEBSOCKET DEBUG: Processing CHAT_LIST message");
                                handleChatListMessage(messageMap);
                                break;
                            default:
                                System.out.println("Received unknown message type: " + type);
                        }
                    } else if (messageMap.containsKey("chats")) {
                        System.out.println("WEBSOCKET DEBUG: Processing chat list message");
                        handleChatListMessage(messageMap);
                    } else if (messageMap.containsKey("chatId") && messageMap.containsKey("participants")) {
                        // Handle single chat creation response
                        System.out.println("WEBSOCKET DEBUG: Processing chat creation by chatId and participants");
                        handleChatCreatedMessage(messageMap);
                    } else if (messageMap.containsKey("action") && "CHAT_CREATED".equals(messageMap.get("action"))) {
                        // Alternative format for chat creation
                        System.out.println("WEBSOCKET DEBUG: Processing chat creation by action=CHAT_CREATED");
                        handleChatCreatedMessage(messageMap);
                    } else {
                        System.out.println("Received unrecognized message format: " + messageMap);
                    }
                } else {
                    System.out.println("WEBSOCKET DEBUG: Received non-map payload: "
                            + (payload != null ? payload.getClass() : "null"));
                    if (payload != null) {
                        System.out.println("WEBSOCKET DEBUG: Payload content: " + payload);
                        // Try to parse as JSON string
                        if (payload instanceof String) {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> parsed = objectMapper.readValue((String) payload, Map.class);
                                System.out.println("WEBSOCKET DEBUG: Parsed JSON as Map: " + parsed);
                                if (parsed.containsKey("chats")) {
                                    System.out.println(
                                            "WEBSOCKET DEBUG: Detected chat list in parsed JSON, calling handleChatListMessage");
                                    handleChatListMessage(parsed);
                                } else if (parsed.containsKey("type")) {
                                    System.out.println(
                                            "WEBSOCKET DEBUG: Detected type in parsed JSON: " + parsed.get("type"));
                                    // Optionally, handle other types as needed
                                } else {
                                    System.out
                                            .println("WEBSOCKET DEBUG: Parsed JSON does not match known message types: "
                                                    + parsed);
                                }
                            } catch (Exception parseEx) {
                                System.err.println("WEBSOCKET DEBUG: Failed to parse non-map payload as JSON: "
                                        + parseEx.getMessage());
                                parseEx.printStackTrace();
                            }
                        }
                    }
                }
                System.out.println("========== WEBSOCKET FRAME PROCESSED ==========\n");
            } catch (Exception e) {
                System.err.println("ERROR HANDLING WEBSOCKET FRAME: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleChatMessage(ChatMessage message) {
            System.out.println("\n======= HANDLING CHAT MESSAGE =======");
            System.out.println("Message type: " + message.getType());
            System.out.println("Sender ID: " + message.getSenderId());
            System.out.println("Current user ID: " + userId);
            System.out.println("Chat ID: " + message.getChatId());
            System.out.println("Content: " + message.getContent());
            System.out.println("ClientTempId: " + message.getClientTempId());

            // Create a unique signature for this message for deduplication
            // Use timestamp to allow same content to be sent multiple times
            String messageSignature = String.format("%s:%s:%s:%s:%s",
                    message.getSenderId(),
                    message.getChatId(),
                    message.getContent(),
                    message.getClientTempId() != null ? message.getClientTempId() : "",
                    message.getTimestamp() != null ? message.getTimestamp().toString() : System.currentTimeMillis());

            long currentTime = System.currentTimeMillis();

            // Check if we've already processed this message recently
            if (processedMessages.containsKey(messageSignature)) {
                long lastProcessed = processedMessages.get(messageSignature);
                if (currentTime - lastProcessed < MESSAGE_DEDUP_TIMEOUT) {
                    System.out.println(
                            "WEBSOCKET DEDUP: Skipping duplicate message (signature: " + messageSignature + ")");
                    return;
                }
            }

            // Mark this message as processed
            processedMessages.put(messageSignature, currentTime);

            // Clean up old entries to prevent memory leaks
            processedMessages.entrySet().removeIf(entry -> currentTime - entry.getValue() > MESSAGE_DEDUP_TIMEOUT);

            System.out.println("WEBSOCKET DEDUP: Processing new message (signature: " + messageSignature + ")");
            System.out.println("ClientTempId: " + message.getClientTempId());

            // Mark message as own if sent by current user
            if (message.getSenderId().equals(userId)) {
                message.setOwn(true);
                System.out.println("Marked message as own (sent by current user)");
            }

            // Log sender, recipient, and chatId
            System.out.println(
                    String.format("Received message from %s in chat %s", message.getSenderId(), message.getChatId()));

            // Update or create chat for this message
            String chatId = message.getChatId();
            Chat chat = chats.getOrDefault(chatId, new Chat());
            System.out.println("Found existing chat: " + (chats.containsKey(chatId) ? "yes" : "no"));
            if (chats.containsKey(chatId)) {
                System.out.println("Existing chat participants: " + chat.getParticipants());
            }

            if (!chats.containsKey(chatId)) {
                // We received a message for a chat we don't know about yet
                // Create a new chat and add it to our list
                chat.setChatId(chatId);

                // Set participants - include both the sender and current user
                List<String> participants = new ArrayList<>();
                participants.add(userId); // Add current user

                // Always add the sender to participants
                if (!participants.contains(message.getSenderId())) {
                    participants.add(message.getSenderId());
                }

                chat.setParticipants(participants);

                // Set owner ID to current user
                chat.setOwnerId(userId);

                // If this is a message from another user to us, set them as the target
                if (!message.getSenderId().equals(userId)) {
                    chat.setTargetUserId(message.getSenderId());
                    chat.setTargetUsername(message.getSenderId()); // Default to using ID as username
                }

                // Add this new chat to our map
                chats.put(chatId, chat);

                // Request full chat list from server to ensure we have complete chat info
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Short delay
                        requestChatList();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                // Debug output
                System.out.println("[WebSocketService] Created new chat for message. ChatId: " + chatId +
                        ", Participants: " + participants + ", TargetUser: " + chat.getTargetUserId());
            }

            // Get the chat type
            boolean isGroupChat = chat.getChatType() != null && "group".equalsIgnoreCase(chat.getChatType());

            // If this is a message for a group chat, make sure the message has the chatId
            // set
            if (isGroupChat) {
                message.setChatId(chatId);
                System.out.println("[WebSocketService] Handling message for group chat: " + chat.getGroupName());
            } else {
                // For private chat, ensure the chat message has the correct chatId
                if (message.getChatId() == null || message.getChatId().isEmpty()) {
                    message.setChatId(chatId);
                    System.out.println("[WebSocketService] Updated empty chatId for private chat message");
                }
            }

            // Add message to chat
            chat.addMessage(message);

            // Update last message preview
            chat.setLastMessagePreview(message.getContent());

            // Ensure the chat message handler is called to update the UI
            if (messageHandler != null) {
                System.out.println("Notifying UI about message: " + message.getContent());
                messageHandler.accept(message);
            } else {
                System.out.println("WARNING: messageHandler is null, UI will not be updated");
            }

            System.out.println("======= CHAT MESSAGE HANDLED =======\n");
        }

        private void handleUserListMessage(Map<String, Object> messageMap) {
            String content = (String) messageMap.get("content");
            if (content != null && !content.isEmpty()) {
                String[] usernames = content.split(",");
                onlineUsers.clear();

                for (String name : usernames) {
                    UserProfile profile = new UserProfile(name);
                    profile.setOnline(true);
                    onlineUsers.add(profile);
                }

                // Notify UI
                if (userListHandler != null) {
                    userListHandler.accept(onlineUsers);
                }
            }
        }

        private void handleUserStatusMessage(Map<String, Object> messageMap) {
            String userId = (String) messageMap.get("userId");
            boolean online = (boolean) messageMap.get("online");

            // Update user status
            for (UserProfile profile : onlineUsers) {
                if (profile.getUsername().equals(userId)) {
                    profile.setOnline(online);
                    break;
                }
            }

            // Notify UI
            if (userListHandler != null) {
                userListHandler.accept(onlineUsers);
            }
        }

        private void handleChatListMessage(Map<String, Object> messageMap) {
            try {
                System.out.println("\n========== CHAT LIST RESPONSE DEBUG START ==========");
                System.out.println("Current user: " + userId);
                System.out.println("Raw chat list message: " + messageMap);

                // Check different possible formats for chat list
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> chatsList = null;

                if (messageMap.containsKey("chats")) {
                    chatsList = (List<Map<String, Object>>) messageMap.get("chats");
                } else if (messageMap.containsKey("response")) {
                    // Try to get chats from response field
                    Object response = messageMap.get("response");
                    if (response instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMap = (Map<String, Object>) response;
                        if (responseMap.containsKey("chats")) {
                            chatsList = (List<Map<String, Object>>) responseMap.get("chats");
                        }
                    } else if (response instanceof List) {
                        // The response might be the chat list directly
                        chatsList = (List<Map<String, Object>>) response;
                    }
                } else if (messageMap.containsKey("data")) {
                    // Another possible format
                    Object data = messageMap.get("data");
                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) data;
                        if (dataMap.containsKey("chats")) {
                            chatsList = (List<Map<String, Object>>) dataMap.get("chats");
                        }
                    } else if (data instanceof List) {
                        chatsList = (List<Map<String, Object>>) data;
                    }
                }

                if (chatsList == null || chatsList.isEmpty()) {
                    System.out.println("Received empty chat list");
                    System.out.println("========== CHAT LIST RESPONSE DEBUG END ==========" + "\n");
                    return;
                }
                System.out.println("Received " + chatsList.size() + " chats from server");
                System.out.println("Chat list content: " + chatsList);

                // Remove temporary chats before adding server chats
                List<String> keysToRemove = new ArrayList<>();
                for (String key : chats.keySet()) {
                    if (key.startsWith("temp_")) {
                        keysToRemove.add(key);
                    }
                }

                if (!keysToRemove.isEmpty()) {
                    System.out.println("Removing " + keysToRemove.size() + " temporary chats: " + keysToRemove);
                    for (String key : keysToRemove) {
                        chats.remove(key);
                    }
                }

                List<Chat> receivedChats = new ArrayList<>();

                // Process each chat from the server
                for (Map<String, Object> chatMap : chatsList) {
                    String chatId = (String) chatMap.get("chatId");
                    System.out.println("Processing chat with ID: " + chatId);

                    // Check if we already have this chat and preserve its messages
                    List<ChatMessage> existingMessages = new ArrayList<>();
                    if (chats.containsKey(chatId) && chats.get(chatId).getMessages() != null) {
                        existingMessages = new ArrayList<>(chats.get(chatId).getMessages());
                        System.out.println(
                                "Preserving " + existingMessages.size() + " existing messages for chat " + chatId);
                    }

                    Chat chat = chats.getOrDefault(chatId, new Chat());
                    chat.setChatId(chatId);

                    @SuppressWarnings("unchecked")
                    List<String> participants = (List<String>) chatMap.get("participants");
                    chat.setParticipants(participants);
                    System.out.println("Chat participants: " + participants);

                    // For private chats, set the target user (the other participant)
                    String chatType = (String) chatMap.get("chatType");
                    boolean isGroupChat = chatType != null && "group".equalsIgnoreCase(chatType);

                    if (!isGroupChat) {
                        String targetUser = participants.stream()
                                .filter(p -> !p.equals(userId))
                                .findFirst()
                                .orElse("");

                        chat.setTargetUserId(targetUser);
                    }

                    chat.setOwnerId(userId);

                    // Check if we have additional user information in the response
                    if (chatMap.containsKey("targetUserName") && chatMap.get("targetUserName") != null) {
                        chat.setTargetUsername((String) chatMap.get("targetUserName"));
                    }

                    // Set chatType if present
                    if (chatMap.containsKey("chatType") && chatMap.get("chatType") != null) {
                        chat.setChatType((String) chatMap.get("chatType"));
                        System.out.println("Set chatType to: " + chat.getChatType());
                    }

                    // Set groupName if present
                    if (chatMap.containsKey("groupName") && chatMap.get("groupName") != null) {
                        chat.setGroupName((String) chatMap.get("groupName"));
                        System.out.println("Set groupName to: " + chat.getGroupName());
                    }

                    // Restore the existing messages
                    if (!existingMessages.isEmpty()) {
                        chat.setMessages(existingMessages);
                        System.out.println("Restored " + existingMessages.size() + " messages for chat " + chatId);
                    }

                    // Add last message preview if available
                    if (chatMap.containsKey("lastMessage") && chatMap.get("lastMessage") != null) {
                        chat.setLastMessagePreview((String) chatMap.get("lastMessage"));
                    } else if (!existingMessages.isEmpty()) {
                        // Get the last message from the existing messages as a preview
                        ChatMessage lastMessage = existingMessages.get(existingMessages.size() - 1);
                        chat.setLastMessagePreview(lastMessage.getContent());
                    }

                    chats.put(chatId, chat);
                    receivedChats.add(chat);
                    System.out.println("Added chat: " + chat);
                }

                System.out.println("Total chats after update: " + chats.size());

                // Notify UI
                if (chatListHandler != null) {
                    List<Chat> chatList = new ArrayList<>(chats.values());
                    System.out.println("Updating UI with " + chatList.size() + " chats");
                    chatListHandler.accept(chatList);
                    System.out.println("UI update complete");
                } else {
                    System.err.println("Warning: chatListHandler is null, can't update UI");
                }

                System.out.println("========== CHAT LIST RESPONSE DEBUG END ==========" + "\n");

            } catch (Exception e) {
                System.err.println("ERROR processing chat list message: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleChatCreatedMessage(Map<String, Object> messageMap) {
            try {
                System.out.println("\n======= CHAT CREATION DEBUG START =======");
                System.out.println("Current user: " + userId);
                System.out.println("Existing chats before processing: " + chats.size());
                System.out.println("Is chatListHandler null? " + (chatListHandler == null));

                // Determine if this is a group chat or private chat - unified approach
                boolean isGroupChat = false;
                String groupName = null;
                String chatType = (String) messageMap.get("chatType");

                // Check different ways that might indicate a group chat
                if (chatType != null && "group".equalsIgnoreCase(chatType)) {
                    isGroupChat = true;
                    groupName = (String) messageMap.get("groupName");
                    System.out.println("Detected GROUP chat by chatType with name: " + groupName);
                }

                // Also check message type fields (various possible formats)
                String messageType = (String) messageMap.get("type");
                if (messageType != null &&
                        ("GROUP_CHAT_CREATED".equalsIgnoreCase(messageType) ||
                                "CHAT_CREATED".equalsIgnoreCase(messageType))) {

                    // If it explicitly states GROUP_CHAT_CREATED, it's definitely a group chat
                    if ("GROUP_CHAT_CREATED".equalsIgnoreCase(messageType)) {
                        isGroupChat = true;
                    }

                    // Get group name if this is a group chat
                    if (isGroupChat && groupName == null) {
                        groupName = (String) messageMap.get("groupName");
                        System.out.println("Detected chat creation event of type: " + messageType +
                                (isGroupChat ? " (GROUP)" : " (PRIVATE)") +
                                (groupName != null ? " with name: " + groupName : ""));
                    }
                }

                String chatId = null;

                // First try to get chatId directly
                if (messageMap.containsKey("chatId")) {
                    chatId = (String) messageMap.get("chatId");
                    System.out.println("Found chatId directly in message: " + chatId);
                }

                // If not found, try to extract from chat object
                if (chatId == null && messageMap.containsKey("chat")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> chatObj = (Map<String, Object>) messageMap.get("chat");
                    if (chatObj != null && chatObj.containsKey("chatId")) {
                        chatId = (String) chatObj.get("chatId");
                        System.out.println("Extracted chatId from chat object: " + chatId);
                    }
                }

                // Still not found, try id field
                if (chatId == null && messageMap.containsKey("id")) {
                    chatId = (String) messageMap.get("id");
                    System.out.println("Using 'id' field as chatId: " + chatId);
                }

                // If we still don't have a chatId, we can't process this message
                if (chatId == null) {
                    System.err.println("ERROR: No usable chat ID found in message, can't process chat creation");
                    System.out.println("Message map keys: " + messageMap.keySet());
                    System.out.println("Full message: " + messageMap);

                    // Force a request for the chat list instead
                    requestChatList();

                    System.out.println("======= CHAT CREATION DEBUG END =======\n");
                    return;
                }

                // Check if we already have this chat
                if (chats.containsKey(chatId)) {
                    System.out.println("Chat with ID " + chatId + " already exists, not adding duplicate");
                    System.out.println("Existing chat: " + chats.get(chatId));
                    System.out.println("======= CHAT CREATION DEBUG END =======\n");
                    return;
                }

                // Create new chat object
                Chat newChat = new Chat();
                newChat.setChatId(chatId);

                // Set chat type for group chats
                if (isGroupChat) {
                    newChat.setChatType("group");
                    newChat.setGroupName(groupName);
                    System.out.println("Setting up GROUP chat with name: " + groupName);
                }

                // Extract participants
                List<String> participants = new ArrayList<>();

                // Try to get participants from different possible fields
                if (messageMap.containsKey("chat")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> chatData = (Map<String, Object>) messageMap.get("chat");

                    if (chatData != null && chatData.containsKey("participants")) {
                        @SuppressWarnings("unchecked")
                        List<String> chatParticipants = (List<String>) chatData.get("participants");
                        if (chatParticipants != null && !chatParticipants.isEmpty()) {
                            participants = chatParticipants;
                            System.out.println("Got participants from chat object: " + participants);
                        }
                    }
                }

                // Try direct participants field
                if (participants.isEmpty() && messageMap.containsKey("participants")) {
                    @SuppressWarnings("unchecked")
                    List<String> directParticipants = (List<String>) messageMap.get("participants");
                    if (directParticipants != null && !directParticipants.isEmpty()) {
                        participants = directParticipants;
                        System.out.println("Got participants directly: " + participants);
                    }
                }

                // Try sender/recipient fields
                if (participants.isEmpty()) {
                    String sender = (String) messageMap.get("sender");
                    String recipient = (String) messageMap.get("recipient");

                    if (sender != null && recipient != null) {
                        participants.add(sender);
                        participants.add(recipient);
                        System.out.println("Created participants from sender/recipient: " + participants);
                    }
                }

                // Try owner/target fields
                if (participants.isEmpty()) {
                    String ownerId = (String) messageMap.get("ownerId");
                    String targetUserId = (String) messageMap.get("targetUserId");

                    if (ownerId != null && targetUserId != null) {
                        participants.add(ownerId);
                        participants.add(targetUserId);
                        System.out.println("Created participants from owner/target: " + participants);
                    }
                }

                // Last resort - just use current user and first available user reference
                if (participants.isEmpty()) {
                    participants.add(userId); // Use our userId
                    String targetUsername = (String) messageMap.get("targetUsername");
                    if (targetUsername != null) {
                        participants.add(targetUsername);
                        System.out
                                .println("Created participants with current user and targetUsername: " + participants);
                    } else {
                        // Try to find any user in the message
                        for (String key : messageMap.keySet()) {
                            if (key.contains("user") && !key.equals("userId")
                                    && messageMap.get(key) instanceof String) {
                                String possibleUser = (String) messageMap.get(key);
                                if (!possibleUser.equals(userId)) {
                                    participants.add(possibleUser);
                                    System.out.println("Added participant from field " + key + ": " + possibleUser);
                                    break;
                                }
                            }
                        }
                    }
                }

                // For group chats, we accept fewer participants as they may just include the
                // current user
                if (participants.isEmpty() || (!isGroupChat && participants.size() < 2)) {
                    System.err.println("ERROR: Could not determine chat participants, incomplete participants list: "
                            + participants);
                    System.out.println("======= CHAT CREATION DEBUG END =======\n");
                    return;
                }

                newChat.setParticipants(participants);

                // Set owner and target
                String ownerId = (String) messageMap.get("ownerId");
                if (ownerId != null) {
                    newChat.setOwnerId(ownerId);
                } else {
                    newChat.setOwnerId(userId); // Default to current user
                }

                // For group chats, we don't need to set targetUserId
                if (!isGroupChat) {
                    // Set target user (the other participant)
                    String targetUser = participants.stream()
                            .filter(p -> !p.equals(userId))
                            .findFirst()
                            .orElse("");

                    newChat.setTargetUserId(targetUser);

                    // Check for target user information that might be included in the message
                    String targetUsername = (String) messageMap.get("targetUserName");
                    if (targetUsername != null) {
                        newChat.setTargetUsername(targetUsername);
                        System.out.println("Setting target username: " + targetUsername);
                    }
                }

                // Add to chats map
                chats.put(chatId, newChat);

                System.out.println("New chat created: " + newChat);

                // Notify UI of updated chat list
                if (chatListHandler != null) {
                    List<Chat> chatList = new ArrayList<>(chats.values());
                    System.out.println("Updating UI with " + chatList.size() + " chats");
                    chatListHandler.accept(chatList);

                    // Request updated chat list from server to ensure we have everything
                    requestChatList();
                } else {
                    System.err.println("Warning: chatListHandler is null, can't update UI");
                }

                System.out.println("======= CHAT CREATION DEBUG END =======\n");
            } catch (Exception e) {
                System.err.println("ERROR processing chat creation: " + e.getMessage());
                e.printStackTrace();

                // Try to recover by requesting full chat list
                requestChatList();
            }
        }

        private void handleAdminActionResponse(Map<String, Object> messageMap) {
            try {
                String action = (String) messageMap.get("action");
                String status = (String) messageMap.get("status");
                String chatId = (String) messageMap.get("chatId");
                String targetUserId = (String) messageMap.get("targetUserId");

                System.out.println("Received admin action response: " + messageMap);

                if (status != null && status.equals("SUCCESS")) {
                    switch (action) {
                        case "PROMOTE_ADMIN":
                            Chat chat = chats.get(chatId);
                            if (chat != null) {
                                // Add user to admin list if not already there
                                chat.addAdmin(targetUserId);
                                System.out.println("User " + targetUserId + " promoted to admin in chat " + chatId);
                            }
                            break;
                        case "REMOVE_PARTICIPANT":
                            chat = chats.get(chatId);
                            if (chat != null) {
                                // Remove user from participants and admins
                                chat.removeParticipant(targetUserId);
                                chat.removeAdmin(targetUserId);
                                System.out.println("User " + targetUserId + " removed from chat " + chatId);
                            }
                            break;
                        case "UPDATE_GROUP_NAME":
                            chat = chats.get(chatId);
                            String newName = (String) messageMap.get("newName");
                            if (chat != null && newName != null) {
                                chat.setGroupName(newName);
                                System.out.println("Updated group name to " + newName + " for chat " + chatId);
                            }
                            break;
                    }

                    // Update UI
                    if (chatListHandler != null) {
                        List<Chat> chatList = new ArrayList<>(chats.values());
                        chatListHandler.accept(chatList);
                    }
                } else {
                    System.err.println("Admin action failed: " + messageMap.get("message"));
                }
            } catch (Exception e) {
                System.err.println("Error handling admin action response: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
