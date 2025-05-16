package com.e2ee.chat.frontend.controller;

import com.e2ee.chat.frontend.E2EEChatFrontendApplication;
import com.e2ee.chat.frontend.model.Chat;
import com.e2ee.chat.frontend.model.ChatMessage;
import com.e2ee.chat.frontend.model.UserProfile;
import com.e2ee.chat.frontend.service.AuthService;
import com.e2ee.chat.frontend.service.WebSocketService;
import com.e2ee.chat.frontend.crypto.CryptoUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import javax.crypto.SecretKey;
import java.net.URL;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    
    @FXML
    private ListView<Chat> chatListView;
    
    @FXML
    private ListView<ChatMessage> messageListView;
    
    @FXML
    private TextField messageField;
    
    @FXML
    private Button sendButton;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private Label currentChatLabel;
    
    @FXML
    private Label statusLabel;
    
    private AuthService authService;
    private WebSocketService webSocketService;
    
    private ObservableList<Chat> chats = FXCollections.observableArrayList();
    private ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private ObservableList<UserProfile> searchResults = FXCollections.observableArrayList();
    
    private Chat currentChat;
    
    // Store AES keys for each chat
    private Map<String, SecretKey> chatKeys = new HashMap<>();
    
    // Store user profiles for chat display
    private Map<String, UserProfile> userProfiles = new HashMap<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = E2EEChatFrontendApplication.getAuthService();
        webSocketService = E2EEChatFrontendApplication.getWebSocketService();
        
        // Set up chat list
        chatListView.setItems(chats);
        chatListView.setCellFactory(new Callback<ListView<Chat>, ListCell<Chat>>() {
            @Override
            public ListCell<Chat> call(ListView<Chat> param) {
                return new ChatListCell();
            }
        });
        
        // Set up message list
        messageListView.setItems(messages);
        messageListView.setCellFactory(new Callback<ListView<ChatMessage>, ListCell<ChatMessage>>() {
            @Override
            public ListCell<ChatMessage> call(ListView<ChatMessage> param) {
                return new MessageListCell();
            }
        });
        
        // Handle chat selection
        chatListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    selectChat(newValue);
                }
            }
        );
        
        // Set up chat message handlers
        webSocketService.setMessageHandler(this::handleChatMessage);
        webSocketService.setChatListHandler(this::updateChatList);
        webSocketService.setUserListHandler(this::updateUserList);
        
        // Enter key in message field should send message
        messageField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSendButton(null);
            }
        });
        
        // Hide elements when no chat is selected
        messageField.setDisable(true);
        sendButton.setDisable(true);
        currentChatLabel.setText("No chat selected");
    }
    
    private void selectChat(Chat chat) {
        currentChat = chat;
        
        // Clear and load messages for this chat
        messages.clear();
        messages.addAll(chat.getMessages());
        
        // Scroll to most recent message
        if (!messages.isEmpty()) {
            messageListView.scrollTo(messages.size() - 1);
        }
        
        // Update UI
        messageField.setDisable(false);
        sendButton.setDisable(false);
        
        // Get the display name for the chat
        String displayName = chat.getTargetUserId();
        String targetId = chat.getTargetUserId();

        // Try to fetch profile if not in cache
        if (targetId != null && fetchUserProfileIfNeeded(targetId)) {
            UserProfile targetProfile = userProfiles.get(targetId);
            if (targetProfile != null) {
                if (targetProfile.getDisplayName() != null && !targetProfile.getDisplayName().isEmpty()) {
                    displayName = targetProfile.getDisplayName();
                } else if (targetProfile.getUsername() != null && !targetProfile.getUsername().isEmpty()) {
                    displayName = targetProfile.getUsername();
                }
            }
        }
        
        currentChatLabel.setText("Chat with: " + displayName);
        
        // Check if we have a shared key for this chat
        if (!chatKeys.containsKey(chat.getChatId())) {
            // If the other user has shared their public key, create and send an AES key
            if (chat.getTargetPublicKey() != null && !chat.getTargetPublicKey().isEmpty()) {
                sendKeyExchange(chat);
            }
        }
    }
    
    @FXML
    public void handleSendButton(ActionEvent event) {
        if (currentChat == null || messageField.getText().trim().isEmpty()) {
            return;
        }
        
        String message = messageField.getText().trim();
        sendMessage(message);
        messageField.clear();
    }
    
    @FXML
    public void handleSearchButton(ActionEvent event) {
        String query = searchField.getText().trim();
        
        if (!query.isEmpty()) {
            // Search for users
            new Thread(() -> {
                UserProfile[] results = authService.searchUsers(query);
                
                Platform.runLater(() -> {
                    if (results != null && results.length > 0) {
                        searchResults.clear();
                        searchResults.addAll(results);
                        
                        // Show search results in a popup
                        showUserSearchResults();
                    } else {
                        statusLabel.setText("No users found matching: " + query);
                        // Clear message after delay
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3))
                            .onFinishedProperty().set(e -> statusLabel.setText(""));
                    }
                });
            }).start();
        }
    }
    
    @FXML
    public void handleLogoutButton(ActionEvent event) {
        // Disconnect from WebSocket
        if (webSocketService.isConnected()) {
            webSocketService.disconnect();
        }
        
        // Logout
        authService.logout();
        
        // Return to login screen
        E2EEChatFrontendApplication.showLoginScreen();
    }
    
    private void handleChatMessage(ChatMessage message) {
        System.out.println("handleChatMessage triggered for chatId: " + message.getChatId());
        System.out.println("Message content: " + message.getContent());
        System.out.println("Message type: " + message.getType());
        System.out.println("Current chatId: " + (currentChat != null ? currentChat.getChatId() : "null"));

        Platform.runLater(() -> {
            // Check if this is for the current chat
            if (currentChat != null && message.getChatId().equals(currentChat.getChatId())) {
                // If encrypted, try to decrypt
                if (message.getType() == ChatMessage.MessageType.ENCRYPTED_CHAT) {
                    SecretKey key = chatKeys.get(message.getChatId());
                    if (key != null) {
                        try {
                            String decryptedContent = CryptoUtils.decryptMessage(
                                message.getContent(),
                                "",
                                key
                            );
                            message.setContent(decryptedContent);
                        } catch (Exception e) {
                            message.setContent("[Encrypted message - cannot decrypt]");
                        }
                    } else {
                        message.setContent("[Encrypted message - key exchange required]");
                    }
                }
                
                // Add to current chat
                currentChat.addMessage(message);
                
                // Update message list and scroll to bottom
                if (!messages.contains(message)) {
                    messages.add(message);
                    messageListView.scrollTo(messages.size() - 1);
                }
            } else if (message.getType() == ChatMessage.MessageType.KEY_EXCHANGE) {
                // Handle key exchange message
                processKeyExchange(message);
            }
        });

            System.out.println("Message Processed");
    }
    
    public void updateChatList(List<Chat> updatedChats) {
        Platform.runLater(() -> {
            // Only keep chats with a valid chatId (fetched from backend)
            List<Chat> validChats = updatedChats.stream()
                .filter(chat -> chat.getChatId() != null && !chat.getChatId().isEmpty())
                .toList();

            System.out.println("Updating chat list with " + validChats.size() + " chats");
            System.out.println("Updated chats details: " + validChats);

            chats.clear();
            chats.addAll(validChats);
            chatListView.refresh();

            // If we have chats but none is selected, select the first one
            if (!chats.isEmpty() && chatListView.getSelectionModel().getSelectedItem() == null) {
                chatListView.getSelectionModel().select(0);
                Chat selectedChat = chatListView.getSelectionModel().getSelectedItem();
                if (selectedChat != null) {
                    selectChat(selectedChat);
                }
            }
            System.out.println("Chat list updated, UI refreshed with " + chats.size() + " chats");
        });
    }
    
    private void updateUserList(List<UserProfile> onlineUsers) {
        // Update online status in our chat list
        Platform.runLater(() -> {
            // Store all received profiles in our userProfiles map for future reference
            for (UserProfile profile : onlineUsers) {
                // Only store if we have a valid profileId
                if (profile.getProfileId() != null && !profile.getProfileId().isEmpty()) {
                    userProfiles.put(profile.getProfileId(), profile);
                }
            }
            
            for (Chat chat : chats) {
                boolean isOnline = onlineUsers.stream()
                    .anyMatch(u -> u.getProfileId().equals(chat.getTargetUserId()));
                
                // Store online status in the chat object
                chat.setTargetUserOnline(isOnline);
            }
            
            // Refresh the chat list view to update online status indicators
            chatListView.refresh();
        });
    }
    
    /**
     * Fetch a user profile by ID if it's not already in the cache
     * @param profileId The profile ID to fetch
     * @return True if the profile was fetched or already exists, false otherwise
     */
    private boolean fetchUserProfileIfNeeded(String profileId) {
        // If already in cache, nothing to do
        if (userProfiles.containsKey(profileId)) {
            return true;
        }
        
        // Skip if profileId is null or empty
        if (profileId == null || profileId.isEmpty()) {
            System.out.println("Cannot fetch profile for null or empty ID");
            return false;
        }
        
        System.out.println("Fetching user profile for ID: " + profileId);
        
        // Fetch from server
        UserProfile profile = authService.getUserProfileById(profileId);
        if (profile != null) {
            userProfiles.put(profileId, profile);
            
            // Update any matching chats with the username
            for (Chat chat : chats) {
                if (chat.getTargetUserId() != null && chat.getTargetUserId().equals(profileId)) {
                    chat.setTargetUsername(profile.getUsername());
                }
            }
            
            // Refresh chat list to show updated names
            Platform.runLater(() -> chatListView.refresh());
            
            return true;
        }
        
        System.out.println("Failed to fetch profile for ID: " + profileId);
        return false;
    }
    
    private void sendMessage(String message) {
        if (currentChat == null) {
            return;
        }
        
        // Check if we have a shared key for encryption
        SecretKey key = chatKeys.get(currentChat.getChatId());
        
        if (key != null) {
            // Encrypt the message
            CryptoUtils.EncryptionResult encResult = CryptoUtils.encryptMessage(message, key);
            
            // Send encrypted message
            webSocketService.sendEncryptedMessage(
                currentChat.getTargetUserId(),
                encResult.getEncryptedData(),
                "",  // No need to send encrypted key for each message
                encResult.getIv()
            );
        } else {
            // If no shared key yet, send plain message
            webSocketService.sendMessage(currentChat.getChatId(), message);
            
            // And try to initiate key exchange
            if (currentChat.getTargetPublicKey() != null && !currentChat.getTargetPublicKey().isEmpty()) {
                sendKeyExchange(currentChat);
            }
        }
        
        // Add to local display (showing unencrypted version)
        ChatMessage localMessage = new ChatMessage();
        localMessage.setType(key != null ? ChatMessage.MessageType.ENCRYPTED_CHAT : ChatMessage.MessageType.MESSAGE);
        localMessage.setSenderId(authService.getUserId());
        localMessage.setContent(message);  // Show the unencrypted version locally
        localMessage.setTimestamp(LocalDateTime.now());
        localMessage.setOwn(true);
        
        // Add to current messages
        messages.add(localMessage);
        messageListView.scrollTo(messages.size() - 1);
    }
    
    private void sendKeyExchange(Chat chat) {
        try {
            // Generate a new AES key for this chat
            SecretKey aesKey = CryptoUtils.generateAESKey();
            
            // Store the key
            chatKeys.put(chat.getChatId(), aesKey);
            
            // Get target user's public key
            PublicKey targetPublicKey = CryptoUtils.decodePublicKey(chat.getTargetPublicKey());
            
            // Encrypt the AES key with target's public key
            String encryptedKey = CryptoUtils.encryptAESKey(aesKey, targetPublicKey);
            
            // Send to other user
            webSocketService.sendKeyExchange(chat.getTargetUserId(), encryptedKey);
            
            statusLabel.setText("Secure key exchange completed.");
            // Clear message after delay
            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2))
                .onFinishedProperty().set(e -> statusLabel.setText(""));
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Key exchange failed");
        }
    }
    
    private void processKeyExchange(ChatMessage message) {
        try {
            // Extract encrypted AES key from message
            String encryptedKeyBase64 = message.getContent();
            
            // Decrypt using our private key
            SecretKey aesKey = CryptoUtils.decryptAESKey(
                encryptedKeyBase64, 
                authService.getKeyPair().getPrivate()
            );
            
            // Store for this chat
            chatKeys.put(message.getChatId(), aesKey);
            
            statusLabel.setText("Received secure key from " + message.getSenderId());
            // Clear message after delay
            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2))
                .onFinishedProperty().set(e -> statusLabel.setText(""));
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error processing key exchange");
        }
    }
    
    private void showUserSearchResults() {
        if (searchResults.isEmpty()) {
            return;
        }
        
        // Create dialog
        Dialog<UserProfile> dialog = new Dialog<>();
        dialog.setTitle("User Search Results");
        dialog.setHeaderText("Select a user to chat with");
        
        // Set button types
        ButtonType selectButtonType = new ButtonType("Start Chat", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);
        
        // Create list view
        ListView<UserProfile> resultsListView = new ListView<>();
        // Print the objects in searchResults for debugging
        searchResults.forEach(user -> System.out.println("Search result: " + user));
        resultsListView.setItems(searchResults);
        resultsListView.setCellFactory(param -> new ListCell<UserProfile>() {
            @Override
            protected void updateItem(UserProfile item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + 
                           (item.getDisplayName() != null && !item.getDisplayName().isEmpty() ? 
                           " (" + item.getDisplayName() + ")" : ""));
                }
            }
        });
        
        // Set content
        dialog.getDialogPane().setContent(resultsListView);
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return resultsListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });
        
        // Show dialog and process result
        dialog.showAndWait().ifPresent(selectedUser -> {
            // Start chat with selected user
            startChatWithUser(selectedUser);
        });
    }
    
    private void startChatWithUser(UserProfile user) {
        System.out.println("Starting chat with user: " + user.getUsername());
        
        // Store the user profile for future reference
        userProfiles.put(user.getProfileId(), user);
        
        // Check if chat already exists
        Chat existingChat = chats.stream()
            .filter(c -> c.getTargetUserId().equals(user.getProfileId()))
            .findFirst()
            .orElse(null);
        
        if (existingChat != null) {
            // Select existing chat
            chatListView.getSelectionModel().select(existingChat);
            statusLabel.setText("Chat with " + user.getDisplayName() + " already exists");
        } else {
            System.out.println("Debug: Selected user's userId = " + user.getProfileId());
            // Use the helper class with improved WebSocket message handling
            ChatCreationHelper.startChatWithUser(
                webSocketService, 
                user, 
                authService.getUserId(), 
                statusLabel,
                chatListView
            );
        }
    }
    
    // Custom cell for chat list
    private class ChatListCell extends ListCell<Chat> {
        private HBox content;
        private Label nameLabel;
        private Label previewLabel;
        private javafx.scene.shape.Circle statusIndicator;
        
        public ChatListCell() {
            super();
            
            // Status indicator
            statusIndicator = new javafx.scene.shape.Circle(5);
            statusIndicator.setStroke(javafx.scene.paint.Color.GRAY);
            
            // Name and preview labels
            nameLabel = new Label();
            nameLabel.setStyle("-fx-font-weight: bold;");
            previewLabel = new Label();
            previewLabel.setStyle("-fx-text-fill: #666;");
            previewLabel.setMaxWidth(Double.MAX_VALUE);
            
            VBox vbox = new VBox(nameLabel, previewLabel);
            HBox.setHgrow(vbox, Priority.ALWAYS);
            
            content = new HBox(5, statusIndicator, vbox);
            content.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }
        
        @Override
        protected void updateItem(Chat item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                // Try to get a nice display name from the userProfiles map
                String displayName = item.getTargetUserId();
                String targetId = item.getTargetUserId();
                
                // Try to fetch profile if not in cache
                if (targetId != null && fetchUserProfileIfNeeded(targetId)) {
                    UserProfile targetProfile = userProfiles.get(targetId);
                    if (targetProfile != null) {
                        // Use display name if available, otherwise username
                        if (targetProfile.getDisplayName() != null && !targetProfile.getDisplayName().isEmpty()) {
                            displayName = targetProfile.getDisplayName();
                        } else if (targetProfile.getUsername() != null && !targetProfile.getUsername().isEmpty()) {
                            displayName = targetProfile.getUsername();
                        }
                    }
                }
                
                nameLabel.setText(displayName);
                previewLabel.setText(item.getLastMessagePreview());
                
                // Update online status
                statusIndicator.setFill(item.isTargetUserOnline() ? 
                    javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.LIGHTGRAY);
                
                setGraphic(content);
            }
        }
    }
    
    // Custom cell for message list
    private class MessageListCell extends ListCell<ChatMessage> {
        private HBox container;
        private VBox messageBox;
        private Label messageLabel;
        private Label timeLabel;
        
        public MessageListCell() {
            super();
            messageBox = new VBox(2);
            messageLabel = new Label();
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(300);
            
            timeLabel = new Label();
            timeLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");
            
            messageBox.getChildren().addAll(messageLabel, timeLabel);
            container = new HBox();
            container.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
        }
        
        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                messageLabel.setText(item.getContent());
                
                // Format timestamp
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                timeLabel.setText(item.getTimestamp().format(formatter));
                
                // Adjust styling based on whether message is from current user
                if (item.isOwn() || item.getSenderId().equals(authService.getUserId())) {
                    messageBox.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-padding: 10;");
                    container.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    container.getChildren().setAll(new javafx.scene.layout.Region(), messageBox);
                    HBox.setHgrow(container.getChildren().get(0), Priority.ALWAYS);
                } else {
                    messageBox.setStyle("-fx-background-color: #ECECEC; -fx-background-radius: 10; -fx-padding: 10;");
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    container.getChildren().setAll(messageBox, new javafx.scene.layout.Region());
                    HBox.setHgrow(container.getChildren().get(1), Priority.ALWAYS);
                }
                
                setGraphic(container);
            }
        }
    }
}
