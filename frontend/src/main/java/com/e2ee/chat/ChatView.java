package com.e2ee.chat;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.Message;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.service.WebSocketService;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ChatView extends BorderPane {
    private final VBox chatList;
    private final VBox messageArea;
    private final TextField messageInput;
    private final Button sendButton;
    private final Label chatHeader;
    private final Label usernameLabel;
    private final ScrollPane messageScrollPane;
    private final Map<String, ChatListItem> chatItems;
    private String selectedChat;
    private WebSocketService webSocketService;
    private final TextField searchField;
    private String currentUser;
    private java.util.Timer searchDebounceTimer;
    private final long SEARCH_DEBOUNCE_DELAY = 300; // milliseconds
    
    // Store our listener as a class field to avoid creating multiple listeners
    private ListChangeListener<UserProfile> searchResultsListener = null;

    public ChatView() {
        chatItems = new HashMap<>();

        // Create chat list (left side)
        VBox leftSide = new VBox(10);
        leftSide.setPadding(new Insets(10));
        leftSide.setPrefWidth(300);
        leftSide.setStyle("-fx-background-color: #f0f0f0;");

        // Create username label
        usernameLabel = new Label("Not logged in");
        usernameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        usernameLabel.setPadding(new Insets(10));
        usernameLabel.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;");

        // Create search field with placeholder text
        searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.setPrefHeight(40);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterChats(newVal));

        chatList = new VBox(10);
        chatList.setPadding(new Insets(10));
        
        // Add welcome message to the chat list initially
        Label welcomeLabel = new Label("Welcome! Your chats will appear here.");
        welcomeLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
        chatList.getChildren().add(welcomeLabel);

        leftSide.getChildren().addAll(usernameLabel, searchField, chatList);

        // Create message area (right side)
        messageArea = new VBox(10);
        messageArea.setPadding(new Insets(10));
        messageArea.setStyle("-fx-background-color: white;");
        
        // Add welcome message to message area
        Label selectChatLabel = new Label("Select a chat to start messaging");
        selectChatLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
        messageArea.getChildren().add(selectChatLabel);

        // Create message input area
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(10));
        inputArea.setStyle("-fx-background-color: #f0f0f0;");

        messageInput = new TextField();
        messageInput.setPromptText("Type a message...");
        messageInput.setPrefHeight(40);
        messageInput.setDisable(true); // Disabled until a chat is selected
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        sendButton = new Button("Send");
        sendButton.setPrefHeight(40);
        sendButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white;");
        sendButton.setDisable(true); // Disabled until a chat is selected

        inputArea.getChildren().addAll(messageInput, sendButton);

        // Create chat header
        chatHeader = new Label("Select a chat");
        chatHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        chatHeader.setPadding(new Insets(10));
        chatHeader.setStyle("-fx-background-color: #f0f0f0;");

        // Create scroll pane for messages
        messageScrollPane = new ScrollPane(messageArea);
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setStyle("-fx-background: white; -fx-border-color: #e0e0e0;");

        // Create right side content
        VBox rightContent = new VBox();
        rightContent.getChildren().addAll(chatHeader, messageScrollPane, inputArea);
        VBox.setVgrow(messageScrollPane, Priority.ALWAYS);

        // Create split pane
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftSide, rightContent);
        splitPane.setDividerPositions(0.3);

        setCenter(splitPane);

        // Set up event handlers
        setupEventHandlers();
    }

    public void setUsername(String username) {
        usernameLabel.setText("Logged in as: " + username);
    }

    private void filterChats(String searchText) {
        // Cancel existing timer if any
        if (searchDebounceTimer != null) {
            searchDebounceTimer.cancel();
            searchDebounceTimer = null;
        }
        
        if (searchText == null || searchText.isEmpty()) {
            // Show user's existing chats immediately if search is cleared
            displayUserChats();
        } else {
            // Show searching indicator immediately
            Platform.runLater(() -> {
                chatList.getChildren().clear();
                Label searchingLabel = new Label("Searching for users...");
                searchingLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
                chatList.getChildren().add(searchingLabel);
            });
            
            // Create a new timer for debouncing
            searchDebounceTimer = new java.util.Timer();
            searchDebounceTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    // This code runs after the debounce delay
                    Platform.runLater(() -> {
                        System.out.println("Debounced search for: " + searchText);
                        
                        // Clear existing search results
                        webSocketService.getSearchResults().clear();
                        
                        // Actually perform the search after the debounce period
                        webSocketService.searchUsersWebSocket(searchText);
                        
                        // Set up display of search results
                        displaySearchResults();
                    });
                }
            }, SEARCH_DEBOUNCE_DELAY);
        }
    }
    
    // Display user's existing chats
    private void displayUserChats() {
        Platform.runLater(() -> {
            System.out.println("Displaying user chats. Total: " + webSocketService.getUserChats().size());
            
            // Clear existing items
            chatList.getChildren().clear();
            chatItems.clear();
            
            // Sort chats by recent activity (most recent first)
            ObservableList<Chat> sortedChats = webSocketService.getUserChats().sorted((c1, c2) -> {
                // If updatedAt is null, assume it's older
                if (c1.getUpdatedAt() == null) return 1;
                if (c2.getUpdatedAt() == null) return -1;
                // Most recent first
                return c2.getUpdatedAt().compareTo(c1.getUpdatedAt());
            });
            
            // Add each chat to the UI
            for (Chat chat : sortedChats) {
                if (chat.getChatId() == null) {
                    System.out.println("Warning: Found chat with null chatId");
                    continue;
                }
                
                final String targetId = chat.getTargetUserId();
                final String ownerId = chat.getOwnerId(); 
                
                // Determine which user to display as the chat name
                final String displayName;
                if (targetId != null && targetId.equals(currentUser)) {
                    displayName = ownerId != null ? ownerId : "Unknown User";
                } else {
                    displayName = targetId != null ? targetId : "Unknown User";
                }
                
                // Get last message preview
                String lastMessage = chat.getLastMessagePreview() != null ? 
                    chat.getLastMessagePreview() : "No messages yet";
                    
                // Format time if available
                String time = "";
                if (chat.getUpdatedAt() != null) {
                    time = chat.getUpdatedAt().toLocalTime().format(
                        DateTimeFormatter.ofPattern("HH:mm")
                    );
                }
                
                final String chatId = chat.getChatId();
                
                // Create chat item and add to list
                ChatListItem chatItem = new ChatListItem(displayName, lastMessage, time);
                
                // Highlight if this is the selected chat
                if (selectedChat != null && selectedChat.equals(chatId)) {
                    chatItem.setStyle("-fx-background-color: #e0e0e0; -fx-cursor: hand;");
                }
                
                chatItem.setOnMouseClicked(e -> selectChat(chatId, displayName));
                chatItems.put(chatId, chatItem);
                chatList.getChildren().add(chatItem);
            }
            
            // If no chats, show a message
            if (chatItems.isEmpty()) {
                Label noChatsLabel = new Label("No chats yet. Search for users to start chatting!");
                noChatsLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
                chatList.getChildren().add(noChatsLabel);
            }
        });
    }
    
    // Display search results in the left panel
    private void displaySearchResults() {
        // We only set up the listener once
        if (searchResultsListener == null) {
            searchResultsListener = new ListChangeListener<UserProfile>() {
                @Override
                public void onChanged(Change<? extends UserProfile> change) {
                    // Process changes immediately
                    Platform.runLater(() -> {
                        System.out.println("Search results updated, found: " + 
                            webSocketService.getSearchResults().size() + " results");
                        
                        // Clear the list to show the search results
                        chatList.getChildren().clear();
                        
                        // Check if we have any results
                        if (webSocketService.getSearchResults().isEmpty()) {
                            Label noResultsLabel = new Label("No users found. Try a different search term.");
                            noResultsLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
                            chatList.getChildren().add(noResultsLabel);
                            return;
                        }
                        
                        // Display up to 4 search results
                        int count = 0;
                        for (UserProfile user : webSocketService.getSearchResults()) {
                            // Limit to 4 results max
                            if (count >= 4) break;
                            
                            // Skip current user
                            if (user.getUsername().equals(currentUser)) {
                                continue;
                            }
                            
                            // Create a chat list item for each user
                            ChatListItem chatItem = new ChatListItem(
                                user.getUsername(), 
                                "Click to start chat", 
                                ""
                            );
                            // Use a different style for search results
                            chatItem.setStyle("-fx-background-color: #f5f9ff; -fx-cursor: hand;");
                            chatItem.setOnMouseClicked(e -> initiateChat(user.getUsername()));
                            
                            // We don't add search results to chatItems map to keep them separate
                            chatList.getChildren().add(chatItem);
                            count++;
                        }
                    });
                }
            };
            
            // Add the listener to the search results collection
            webSocketService.getSearchResults().addListener(searchResultsListener);
        }
        
        // If we already have results available, display them immediately
        if (!webSocketService.getSearchResults().isEmpty()) {
            Platform.runLater(() -> {
                chatList.getChildren().clear();
                
                int count = 0;
                for (UserProfile user : webSocketService.getSearchResults()) {
                    // Limit to 4 results max
                    if (count >= 4) break;
                    
                    // Skip current user
                    if (user.getUsername().equals(currentUser)) {
                        continue;
                    }
                    
                    ChatListItem chatItem = new ChatListItem(
                        user.getUsername(), 
                        "Click to start chat", 
                        ""
                    );
                    chatItem.setStyle("-fx-background-color: #f5f9ff; -fx-cursor: hand;");
                    chatItem.setOnMouseClicked(e -> initiateChat(user.getUsername()));
                    
                    chatList.getChildren().add(chatItem);
                    count++;
                }
            });
        }
    }
    
    // Initiate a chat with a user from search results
    private void initiateChat(String username) {
        System.out.println("Initiating chat with user: " + username);
        
        // First check if we already have a chat with this user
        Chat existingChat = null;
        for (Chat chat : webSocketService.getUserChats()) {
            // Check if this user is either target or owner
            if ((chat.getTargetUserId() != null && chat.getTargetUserId().equals(username)) || 
                (chat.getOwnerId() != null && chat.getOwnerId().equals(username))) {
                existingChat = chat;
                break;
            }
        }
        
        if (existingChat != null) {
            // If we already have a chat, just select it
            final Chat finalChat = existingChat;
            Platform.runLater(() -> {
                selectChat(finalChat.getChatId(), username);
                // Clear search and show chats
                searchField.clear();
                displayUserChats();
            });
        } else {
            // Create a new chat and set up a one-time listener for the response
            webSocketService.createChat(username);
            
            ListChangeListener<Chat> chatCreationListener = new ListChangeListener<Chat>() {
                @Override
                public void onChanged(Change<? extends Chat> change) {
                    boolean found = false;
                    while (change.next() && !found) {
                        if (change.wasAdded()) {
                            for (Chat chat : change.getAddedSubList()) {
                                if ((chat.getTargetUserId() != null && chat.getTargetUserId().equals(username)) ||
                                    (chat.getOwnerId() != null && chat.getOwnerId().equals(username))) {
                                    
                                    final Chat newChat = chat;
                                    Platform.runLater(() -> {
                                        // Select the new chat
                                        selectChat(newChat.getChatId(), username);
                                        // Clear search and show chats
                                        searchField.clear();
                                        displayUserChats();
                                    });
                                    
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    // If we found the chat, remove this listener to avoid duplicates
                    if (found) {
                        webSocketService.getUserChats().removeListener(this);
                    }
                }
            };
            
            // Add the listener
            webSocketService.getUserChats().addListener(chatCreationListener);
        }
    }

    private void setupEventHandlers() {
        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());
    }

    private void sendMessage() {
        if (selectedChat == null || messageInput.getText().trim().isEmpty()) {
            return;
        }

        String message = messageInput.getText().trim();
        webSocketService.sendMessageToChat(selectedChat, message);
        messageInput.clear();
    }

    public void setWebSocketService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
        this.currentUser = webSocketService.getCurrentUser();
        
        // Set the username display
        setUsername(currentUser);
        
        System.out.println("WebSocket service set for user: " + currentUser);

        // Listen for new messages
        webSocketService.getMessages().addListener((ListChangeListener<ChatMessage>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ChatMessage message : change.getAddedSubList()) {
                        handleNewMessage(message);
                    }
                }
            }
        });
                
        // Listen for user chats - used for real-time updates
        webSocketService.getUserChats().addListener((ListChangeListener<Chat>) change -> {
            boolean needsDisplay = false;
            
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasReplaced()) {
                    needsDisplay = true;
                }
            }
            
            if (needsDisplay) {
                Platform.runLater(this::displayUserChats);
            }
        });

        // Listen for user status changes
        webSocketService.getUserStatus().addListener(
                (javafx.collections.MapChangeListener.Change<? extends String, ? extends Boolean> change) -> {
                    if (change.wasAdded() || change.wasRemoved()) {
                        String username = change.getKey();
                        
                        // Update status for this user in any chat items
                        for (Map.Entry<String, ChatListItem> entry : chatItems.entrySet()) {
                            ChatListItem item = entry.getValue();
                            if (item.getName().equals(username)) {
                                Platform.runLater(() -> {
                                    item.setOnline(change.getValueAdded() != null ? change.getValueAdded() : false);
                                });
                            }
                        }
                    }
                });
        
        // Load existing chats initially
        loadChatsAndMessages();
    }

    private void handleNewMessage(ChatMessage message) {
        Platform.runLater(() -> {
            String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            switch (message.getType()) {
                case CHAT:
                    String chatId = message.getChatId();
                    if (chatId == null) {
                        // Try to find chat ID from sender/recipient
                        String otherUser = message.getSender().equals(currentUser) 
                            ? message.getRecipient() 
                            : message.getSender();
                        
                        for (Chat chat : webSocketService.getUserChats()) {
                            if ((chat.getTargetUserId() != null && chat.getTargetUserId().equals(otherUser)) ||
                                (chat.getOwnerId() != null && chat.getOwnerId().equals(otherUser))) {
                                chatId = chat.getChatId();
                                break;
                            }
                        }
                    }
                    
                    if (chatId != null) {
                        boolean isSent = message.getSender().equals(currentUser);
                        MessageBubble bubble = new MessageBubble(message.getContent(), currentTime, isSent);
                        
                        // Only add to message area if this is for the currently selected chat
                        if (chatId.equals(selectedChat)) {
                            messageArea.getChildren().add(bubble);
                        }

                        // Update the chat preview in left panel
                        ChatListItem chatItem = chatItems.get(chatId);
                        if (chatItem != null) {
                            chatItem.setLastMessage(message.getContent());
                            chatItem.setTime(currentTime);
                        }
                        
                        // Force refresh of the chat list to update order (most recent first)
                        displayUserChats();
                    }
                    break;

                case JOIN:
                    System.out.println("User joined: " + message.getSender());
                    // You could add a system message to the chat area if desired
                    break;

                case LEAVE:
                    System.out.println("User left: " + message.getSender());
                    // You could add a system message to the chat area if desired
                    break;
                    
                case KEY_EXCHANGE:
                    System.out.println("Received key exchange from: " + message.getSender());
                    // We could implement a visual indicator that encryption is active
                    break;
                    
                case ENCRYPTED_CHAT:
                    // For encrypted messages, decrypt and handle similar to regular chat messages
                    System.out.println("Received encrypted message from: " + message.getSender());
                    
                    // Here we would decrypt the message and then display it
                    // For now, just show it as an encrypted message
                    if (message.getChatId() != null && message.getChatId().equals(selectedChat)) {
                        boolean isSent = message.getSender().equals(currentUser);
                        MessageBubble encryptedBubble = new MessageBubble(
                            "[Encrypted] " + message.getContent(), 
                            currentTime, 
                            isSent
                        );
                        messageArea.getChildren().add(encryptedBubble);
                    }
                    
                    // Update chat preview
                    for (Chat chat : webSocketService.getUserChats()) {
                        if (chat.getChatId() != null && chat.getChatId().equals(message.getChatId())) {
                            ChatListItem item = chatItems.get(chat.getChatId());
                            if (item != null) {
                                item.setLastMessage("[Encrypted message]");
                                item.setTime(currentTime);
                            }
                            break;
                        }
                    }
                    
                    // Refresh chat list
                    displayUserChats();
                    break;
                    
                case USER_LIST:
                    // Update any online/offline indicators in the chat list
                    System.out.println("Updated user list received");
                    
                    // Parse the content to get the list of online users
                    String[] onlineUsers = message.getContent().split(",");
                    
                    // For each chat item, update its online status
                    for (ChatListItem item : chatItems.values()) {
                        String username = item.getName();
                        boolean isOnline = false;
                        
                        // Check if this user is online
                        for (String onlineUser : onlineUsers) {
                            if (onlineUser.trim().equals(username)) {
                                isOnline = true;
                                break;
                            }
                        }
                        
                        // Update the UI
                        item.setOnline(isOnline);
                    }
                    break;
            }

            // Scroll to bottom of message area
            messageScrollPane.setVvalue(1.0);
        });
    }

    public void addChat(String chatId, String username, String lastMessage, String time) {
        ChatListItem chatItem = new ChatListItem(username, lastMessage, time);
        chatItem.setOnMouseClicked(e -> selectChat(chatId, username));
        chatItems.put(chatId, chatItem);
        chatList.getChildren().add(chatItem);
    }

    private void selectChat(String chatId, String displayName) {
        System.out.println("Selecting chat: " + chatId + " with user: " + displayName);
        
        selectedChat = chatId;
        chatHeader.setText("Chat with " + displayName);
        messageArea.getChildren().clear();

        // Update selected chat style for visual feedback
        chatItems.values().forEach(item -> 
            item.setStyle("-fx-background-color: white; -fx-cursor: hand;"));
            
        if (chatItems.containsKey(chatId)) {
            chatItems.get(chatId).setStyle("-fx-background-color: #e0e0e0; -fx-cursor: hand;");
        }
        
        // Enable message input for the selected chat
        messageInput.setDisable(false);
        sendButton.setDisable(false);
        
        // Load messages for this chat
        loadChatMessages(chatId);
    }
    
    private void loadChatMessages(String chatId) {
        System.out.println("Loading messages for chat: " + chatId);
        
        // Clear any existing messages
        messageArea.getChildren().clear();
        
        // Get messages for this chat
        ObservableList<Message> messages = webSocketService.getChatMessages(chatId);
        
        if (messages.isEmpty()) {
            // Show a helpful message if there are no messages yet
            Label noMessagesLabel = new Label("No messages yet. Send a message to start the conversation!");
            noMessagesLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
            messageArea.getChildren().add(noMessagesLabel);
        } else {
            // Display all messages in chronological order
            for (Message message : messages) {
                boolean isSent = message.getSenderId() != null && message.getSenderId().equals(currentUser);
                
                // Format the timestamp
                String time;
                if (message.getTimestamp() != null) {
                    time = message.getTimestamp().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                } else {
                    time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                }
                
                // Create and add message bubble
                MessageBubble bubble = new MessageBubble(message.getContent(), time, isSent);
                messageArea.getChildren().add(bubble);
            }
        }
        
        // Scroll to bottom of message area
        Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
    }

    public VBox getChatList() {
        return chatList;
    }

    public VBox getMessageArea() {
        return messageArea;
    }

    public TextField getMessageInput() {
        return messageInput;
    }

    public Button getSendButton() {
        return sendButton;
    }

    public Label getChatHeader() {
        return chatHeader;
    }

    public ScrollPane getMessageScrollPane() {
        return messageScrollPane;
    }

    public void loadChatsAndMessages() {
        System.out.println("Loading chats and messages for user: " + currentUser);
        
        // Request to load the user chats from the server
        webSocketService.loadUserChats();
        
        // Set up a listener for any future chat updates
        ListChangeListener<Chat> chatChangeListener = new ListChangeListener<Chat>() {
            @Override
            public void onChanged(Change<? extends Chat> change) {
                while (change.next()) {
                    if (change.wasAdded() || change.wasReplaced() || change.wasRemoved()) {
                        Platform.runLater(() -> displayUserChats());
                    }
                }
            }
        };
        
        // Remove any existing listener to avoid duplicates
        webSocketService.getUserChats().removeListener(chatChangeListener);
        webSocketService.getUserChats().addListener(chatChangeListener);
        
        // Always display chats immediately, even if empty
        Platform.runLater(this::displayUserChats);
    }
}
