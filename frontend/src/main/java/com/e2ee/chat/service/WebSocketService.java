package com.e2ee.chat.service;

import com.e2ee.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class WebSocketService {
    private StompSession stompSession;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String token;
    private String currentUser;
    private final ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final ObservableMap<String, Boolean> userStatus = FXCollections.observableHashMap();
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }
    
    public String getCurrentUser() {
        return currentUser;
    }
    
    public void connect() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        
        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        try {
            stompSession = stompClient.connect("http://localhost:8081/ws", new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    subscribeToMessages();
                    subscribeToUserStatus();
                }
                
                @Override
                public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                    System.err.println("WebSocket error: " + exception.getMessage());
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to connect to WebSocket: " + e.getMessage());
        }
    }
    
    private void subscribeToMessages() {
        stompSession.subscribe("/topic/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessage message = (ChatMessage) payload;
                Platform.runLater(() -> messages.add(message));
            }
        });
    }
    
    private void subscribeToUserStatus() {
        stompSession.subscribe("/topic/users", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String[].class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String[] users = (String[]) payload;
                Platform.runLater(() -> {
                    onlineUsers.clear();
                    onlineUsers.addAll(users);
                    // Update user status
                    userStatus.clear();
                    for (String user : users) {
                        userStatus.put(user, true);
                    }
                });
            }
        });
    }
    
    public void sendMessage(String content, String recipient) {
        if (stompSession != null && stompSession.isConnected()) {
            ChatMessage message = new ChatMessage();
            message.setSender(currentUser);
            message.setRecipient(recipient);
            message.setContent(content);
            message.setType(ChatMessage.MessageType.CHAT);
            stompSession.send("/app/chat", message);
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
        if (stompSession != null) {
            stompSession.disconnect();
        }
    }
} 