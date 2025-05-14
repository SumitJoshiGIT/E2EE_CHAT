package com.e2ee.chat.service;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketService {
    private StompSession stompSession;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String token;
    private String currentUser;
    private final ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final ObservableMap<String, Boolean> userStatus = FXCollections.observableHashMap();
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;
    private MessageStorageService messageStorageService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String backendUrl = "http://localhost:8080";
    
    // Observable lists for different message types
    private final ObservableList<UserProfile> searchResults = FXCollections.observableArrayList();
    private final ObservableList<Chat> userChats = FXCollections.observableArrayList();
    private final Map<String, ObservableList<Message>> chatMessages = new HashMap<>();
    
    // Getter methods for the new observables
    public ObservableList<UserProfile> getSearchResults() {
        return searchResults;
    }
    
    public ObservableList<Chat> getUserChats() {
        return userChats;
    }
    
    public ObservableList<Message> getChatMessages(String chatId) {
        if (!chatMessages.containsKey(chatId)) {
            chatMessages.put(chatId, FXCollections.observableArrayList());
        }
        return chatMessages.get(chatId);
    }
    
    // Load user's chats from the server
    public void loadUserChats() {
        try {
            String url = backendUrl + "/api/chats/user/" + currentUser;
            ResponseEntity<Chat[]> response = restTemplate.getForEntity(url, Chat[].class);
            
            if (response.getBody() != null) {
                Platform.runLater(() -> {
                    userChats.clear();
                    userChats.addAll(Arrays.asList(response.getBody()));
                    System.out.println("Loaded " + userChats.size() + " chats for user " + currentUser);
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to load user chats: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setCurrentUser(String username) {
        this.currentUser = username;
        this.messageStorageService = new MessageStorageService(username);
        // Load saved messages when user is set
        messages.setAll(messageStorageService.loadMessages());
    }
    
    public String getCurrentUser() {
        return currentUser;
    }
    
    public void connect() {
        if (stompSession != null && stompSession.isConnected()) {
            return;
        }

        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        
        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        CompletableFuture<StompSession> future = new CompletableFuture<>();
        
        try {
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add("Authorization", "Bearer " + token);
            
            System.out.println("Connecting to WebSocket with token: " + token);
            
            stompSession = stompClient.connect("http://localhost:8080/ws", new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    System.out.println("WebSocket connected successfully");
                    System.out.println("Connected headers: " + connectedHeaders);
                    future.complete(session);
                }
                
                @Override
                public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                    System.err.println("WebSocket error: " + exception.getMessage());
                    System.err.println("Command: " + command);
                    System.err.println("Headers: " + headers);
                    future.completeExceptionally(exception);
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    System.err.println("WebSocket transport error: " + exception.getMessage());
                    future.completeExceptionally(exception);
                }
            }, connectHeaders).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // Wait for connection to be established
            stompSession = future.get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // Subscribe to topics after connection is established
            subscribeToMessages();
            subscribeToUserStatus();
            
            // Notify that user has joined
            notifyUserJoined();
            
        } catch (Exception e) {
            System.err.println("Failed to connect to WebSocket: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to WebSocket", e);
        }
    }
    
    private void subscribeToMessages() {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("WebSocket is not connected");
        }
        
        System.out.println("Subscribing to user messages...");
        
        // Subscribe to user-specific messages
        stompSession.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof ChatMessage) {
                    ChatMessage message = (ChatMessage) payload;
                    Platform.runLater(() -> {
                        System.out.println("\n=== New Message Received ===");
                        System.out.println("From: " + message.getSender());
                        System.out.println("To: " + message.getRecipient());
                        System.out.println("Content: " + message.getContent());
                        System.out.println("Type: " + message.getType());
                        System.out.println("Headers: " + headers);
                        System.out.println("========================\n");
                        
                        // Only add the message if it's a chat message
                        if (message.getType() == ChatMessage.MessageType.CHAT) {
                            // Determine the chat partner
                            String chatPartner = message.getSender().equals(currentUser) 
                                ? message.getRecipient() 
                                : message.getSender();
                            
                            System.out.println("Chat partner: " + chatPartner);
                            
                            // Add message to the list
                            messages.add(message);
                            // Save messages after each new message
                            messageStorageService.saveMessages(messages);
                        }
                    });
                } else if (payload instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = (Map<String, Object>) payload;
                    String type = (String) response.get("type");
                    
                    Platform.runLater(() -> {
                        System.out.println("\n=== Custom Message Received ===");
                        System.out.println("Type: " + type);
                        System.out.println("Headers: " + headers);
                        System.out.println("==========================\n");
                        
                        try {
                            if ("SEARCH_RESULTS".equals(type)) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> usersData = (List<Map<String, Object>>) response.get("users");
                                List<UserProfile> profiles = new ArrayList<>();
                                
                                if (usersData != null) {
                                    for (Map<String, Object> userData : usersData) {
                                        UserProfile profile = new UserProfile();
                                        profile.setUsername((String) userData.get("username"));
                                        profile.setDisplayName((String) userData.get("displayName"));
                                        profile.setStatus((String) userData.get("status"));
                                        profiles.add(profile);
                                    }
                                }
                                
                                searchResults.clear();
                                searchResults.addAll(profiles);
                                System.out.println("Received " + searchResults.size() + " search results");
                                
                            } else if ("CHAT_CREATED".equals(type)) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> chatData = (Map<String, Object>) response.get("chat");
                                
                                if (chatData != null) {
                                    Chat chat = new Chat();
                                    chat.setChatId((String) chatData.get("chatId"));
                                    chat.setOwnerId((String) chatData.get("ownerId"));
                                    chat.setTargetUserId((String) chatData.get("targetUserId"));
                                    
                                    // Add to user's chats if not already there
                                    boolean found = false;
                                    for (Chat existingChat : userChats) {
                                        if (existingChat.getChatId().equals(chat.getChatId())) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!found) {
                                        userChats.add(chat);
                                        System.out.println("Added new chat: " + chat.getChatId());
                                    }
                                }
                                
                            } else if ("MESSAGE".equals(type)) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> messageData = (Map<String, Object>) response.get("message");
                                String chatId = (String) response.get("chatId");
                                
                                if (messageData != null && chatId != null) {
                                    Message message = new Message();
                                    message.setMessageId((String) messageData.get("messageId"));
                                    message.setChatId(chatId);
                                    message.setSenderId((String) messageData.get("senderId"));
                                    message.setContent((String) messageData.get("content"));
                                    
                                    // Add message to the appropriate chat's message list
                                    ObservableList<Message> chatMessagesList = getChatMessages(chatId);
                                    chatMessagesList.add(message);
                                    System.out.println("Added message to chat " + chatId);
                                    
                                    // Refresh the chat's updated timestamp
                                    for (Chat chat : userChats) {
                                        if (chat.getChatId().equals(chatId)) {
                                            if (message.getTimestamp() != null) {
                                                chat.setUpdatedAt(message.getTimestamp());
                                            }
                                            chat.setLastMessagePreview(message.getContent());
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing message: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }
            }
        });

        System.out.println("Subscribing to public messages...");
        
        // Subscribe to public messages (user status updates)
        stompSession.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof ChatMessage) {
                    ChatMessage message = (ChatMessage) payload;
                    Platform.runLater(() -> {
                        System.out.println("\n=== Public Message Received ===");
                        System.out.println("Type: " + message.getType());
                        System.out.println("Content: " + message.getContent());
                        System.out.println("Headers: " + headers);
                        System.out.println("============================\n");
                        
                        if (message.getType() == ChatMessage.MessageType.JOIN) {
                            // Don't add current user to the list
                            if (!message.getSender().equals(currentUser) && !onlineUsers.contains(message.getSender())) {
                                onlineUsers.add(message.getSender());
                                userStatus.put(message.getSender(), true);
                            }
                        } else if (message.getType() == ChatMessage.MessageType.LEAVE) {
                            onlineUsers.remove(message.getSender());
                            userStatus.remove(message.getSender());
                        } else if (message.getType() == ChatMessage.MessageType.USER_LIST) {
                            // Update the complete list of online users
                            onlineUsers.clear();
                            userStatus.clear();
                            String[] users = message.getContent().split(",");
                            for (String user : users) {
                                // Don't add current user to the list
                                if (!user.isEmpty() && !user.equals(currentUser)) {
                                    onlineUsers.add(user);
                                    userStatus.put(user, true);
                                }
                            }
                        }
                    });
                }
            }
        });
    }
    
    private void subscribeToUserStatus() {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("WebSocket is not connected");
        }
        
        stompSession.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof ChatMessage) {
                    ChatMessage message = (ChatMessage) payload;
                    Platform.runLater(() -> {
                        if (message.getType() == ChatMessage.MessageType.JOIN) {
                            // Don't add current user to the list
                            if (!message.getSender().equals(currentUser) && !onlineUsers.contains(message.getSender())) {
                                onlineUsers.add(message.getSender());
                                userStatus.put(message.getSender(), true);
                            }
                        } else if (message.getType() == ChatMessage.MessageType.LEAVE) {
                            onlineUsers.remove(message.getSender());
                            userStatus.remove(message.getSender());
                        } else if (message.getType() == ChatMessage.MessageType.USER_LIST) {
                            // Update the complete list of online users
                            onlineUsers.clear();
                            userStatus.clear();
                            String[] users = message.getContent().split(",");
                            for (String user : users) {
                                // Don't add current user to the list
                                if (!user.isEmpty() && !user.equals(currentUser)) {
                                    onlineUsers.add(user);
                                    userStatus.put(user, true);
                                }
                            }
                        }
                    });
                }
            }
        });
    }
    
    private void notifyUserJoined() {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("WebSocket is not connected");
        }
        
        ChatMessage joinMessage = new ChatMessage();
        joinMessage.setType(ChatMessage.MessageType.JOIN);
        joinMessage.setSender(currentUser);
        stompSession.send("/app/chat.addUser", joinMessage);
    }
    
    public void sendMessage(String content, String recipient) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("WebSocket is not connected");
        }
        
        ChatMessage message = new ChatMessage();
        message.setSender(currentUser);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setType(ChatMessage.MessageType.CHAT);
        
        try {
            System.out.println("\n=== Sending Message ===");
            System.out.println("To: " + recipient);
            System.out.println("Content: " + content);
            System.out.println("=====================\n");
            
            stompSession.send("/app/chat.send", message);
            
            // Add message to local storage immediately
            Platform.runLater(() -> {
                messages.add(message);
                messageStorageService.saveMessages(messages);
            });
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send message", e);
        }
    }
    
    public ObservableList<String> getOnlineUsers() {
        return onlineUsers;
    }
    
    public ObservableList<ChatMessage> getMessages() {
        return messages;
    }
    
    public ObservableMap<String, Boolean> getUserStatus() {
        return userStatus;
    }
    
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            ChatMessage leaveMessage = new ChatMessage();
            leaveMessage.setType(ChatMessage.MessageType.LEAVE);
            leaveMessage.setSender(currentUser);
            stompSession.send("/app/chat.leave", leaveMessage);
            stompSession.disconnect();
        }
    }

    public List<UserProfile> searchUsers(String query) {
        // Make a REST call to the backend to search for users
        String url = backendUrl + "/api/profile/search?query=" + query;
        ResponseEntity<UserProfile[]> response = restTemplate.getForEntity(url, UserProfile[].class);
        return Arrays.asList(response.getBody());
    }

    public List<Chat> getChats() {
        String url = backendUrl + "/api/chats";
        ResponseEntity<Chat[]> response = restTemplate.getForEntity(url, Chat[].class);
        return Arrays.asList(response.getBody());
    }

    public List<Message> getMessages(String chatId) {
        String url = backendUrl + "/api/messages?chatId=" + chatId;
        ResponseEntity<Message[]> response = restTemplate.getForEntity(url, Message[].class);
        return Arrays.asList(response.getBody());
    }

    // Search users method using WebSocket
    public void searchUsersWebSocket(String query) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("WebSocket is not connected");
        }
        
        // Clear existing search results before sending new search
        Platform.runLater(() -> searchResults.clear());
        
        Map<String, Object> searchPayload = new HashMap<>();
        searchPayload.put("query", query);
        searchPayload.put("sender", currentUser);
        
        System.out.println("Searching users with query: " + query);
        
        try {
            stompSession.send("/app/chat.searchUsers", searchPayload);
        } catch (Exception e) {
            System.err.println("Error searching for users: " + e.getMessage());
            e.printStackTrace();
            
            // If WebSocket fails, try fallback to REST API
            try {
                String url = backendUrl + "/api/users/search?query=" + query;
                ResponseEntity<UserProfile[]> response = restTemplate.getForEntity(url, UserProfile[].class);
                
                if (response.getBody() != null) {
                    final List<UserProfile> profiles = Arrays.asList(response.getBody());
                    
                    Platform.runLater(() -> {
                        searchResults.clear();
                        searchResults.addAll(profiles);
                        System.out.println("Received " + searchResults.size() + " search results via REST");
                    });
                }
            } catch (Exception restError) {
                System.err.println("Failed to search users via REST: " + restError.getMessage());
            }
        }
    }
    
    // Create chat with user
    public void createChat(String targetUserId) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("WebSocket is not connected");
        }
        
        Map<String, Object> createChatPayload = new HashMap<>();
        createChatPayload.put("ownerId", currentUser);
        createChatPayload.put("targetUserId", targetUserId);
        
        System.out.println("Creating chat with user: " + targetUserId);
        stompSession.send("/app/chat.createChat", createChatPayload);
    }
    
    // Send message to a specific chat
    public void sendMessageToChat(String chatId, String content) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("WebSocket is not connected");
        }
        
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("chatId", chatId);
        messagePayload.put("senderId", currentUser);
        messagePayload.put("content", content);
        messagePayload.put("messageType", "TEXT");
        
        System.out.println("Sending message to chat " + chatId + ": " + content);
        stompSession.send("/app/chat.sendMessage", messagePayload);
    }
}