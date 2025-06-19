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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Callback;

import javax.crypto.SecretKey;
import java.net.URL;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    private Button newGroupButton;
    
    @FXML
    private Label profileInitialsLabel;
    
    @FXML
    private StackPane profilePicContainer;
    
    @FXML
    private Label memberStatusLabel;
    
    @FXML
    private Button chatOptionsButton;
    
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
    
    // Store pending messages by clientTempId for deduplication
    private Map<String, ChatMessage> pendingMessages = new HashMap<>();
    
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
            System.out.println("KEY PRESSED: " + event.getCode());
            if (event.getCode() == KeyCode.ENTER) {
                System.out.println("ENTER KEY DETECTED - calling handleSendButton");
                handleSendButton(null);
            }
        });
        
        // Hide elements when no chat is selected
        messageField.setDisable(true);
        sendButton.setDisable(true);
        currentChatLabel.setText("No chat selected");
        memberStatusLabel.setText("No members");
        profileInitialsLabel.setText("");
        
        // Initialize profile picture container with default style
        profilePicContainer.setStyle("-fx-background-color: #DDD; -fx-background-radius: 20;");
        
        // Initialize chat options button
        chatOptionsButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Test button functionality
        System.out.println("INITIALIZATION - sendButton exists: " + (sendButton != null));
        if (sendButton != null) {
            System.out.println("INITIALIZATION - sendButton disabled: " + sendButton.isDisabled());
            System.out.println("INITIALIZATION - sendButton visible: " + sendButton.isVisible());
        }
    }
    
    private void selectChat(Chat chat) {
        currentChat = chat;
        
        System.out.println("\n========== SELECTING CHAT ==========");
        System.out.println("Chat ID: " + chat.getChatId());
        System.out.println("Chat Type: " + chat.getChatType());
        System.out.println("Group Name: " + chat.getGroupName());
        System.out.println("Target User: " + chat.getTargetUserId());
        
        messages.clear();
        if (chat.getMessages() != null) { // Guard against null messages list
            messages.addAll(chat.getMessages());
            System.out.println("Loaded " + chat.getMessages().size() + " messages from chat to UI");
        } else {
            System.out.println("Chat has no messages list, initializing empty");
            chat.setMessages(new ArrayList<>());
        }
        
        // Force refresh of the message list view
        messageListView.refresh();
        
        if (!messages.isEmpty()) {
            messageListView.scrollTo(messages.size() - 1);
        }
        
        System.out.println("UI messages list now has " + messages.size() + " messages");
        System.out.println("ListView item count: " + messageListView.getItems().size());
        
        messageField.setDisable(false);
        sendButton.setDisable(false);
        
        System.out.println("BUTTONS ENABLED - messageField disabled: " + messageField.isDisabled());
        System.out.println("BUTTONS ENABLED - sendButton disabled: " + sendButton.isDisabled());
        System.out.println("BUTTONS ENABLED - sendButton visible: " + sendButton.isVisible());
        
        // Check if this is a group chat
        boolean isGroupChat = chat.getChatType() != null && "group".equalsIgnoreCase(chat.getChatType());
        String headerText;
        
        if (isGroupChat) {
            // Use group name for header
            headerText = chat.getGroupName();
            if (headerText == null || headerText.trim().isEmpty() || "null".equalsIgnoreCase(headerText.trim())) {
                headerText = "Group Chat";
            }
            headerText = "👥 " + headerText + " (" + chat.getParticipants().size() + " members)";
        } else {
            // For one-on-one chats, use username
            String displayName = chat.getTargetUsername(); // Primarily rely on this
            String targetId = chat.getTargetUserId();

            // If chat.getTargetUsername() is still the ID, or null/empty/"null", make one last attempt to resolve.
            if (targetId != null && (displayName == null || displayName.isEmpty() || displayName.equalsIgnoreCase("null") || displayName.equals(targetId))) {
                System.out.println("[SELECT_CHAT] Name for " + targetId + " is not optimal ('" + displayName + "'). Attempting fetch.");
                fetchUserProfileIfNeeded(targetId); // This will update chat.setTargetUsername()
                displayName = chat.getTargetUsername(); // Re-fetch from chat object, which should now be updated.
                System.out.println("[SELECT_CHAT] After fetch, new displayName for " + targetId + ": '" + displayName + "'");
            }
            
            headerText = displayName;
        
            headerText = displayName;
        }
        
        // Final fallback if headerText is still not good for one-on-one chats
        if (!isGroupChat && (headerText == null || headerText.isEmpty() || headerText.equalsIgnoreCase("null"))) {
            String targetId = chat.getTargetUserId();
            headerText = (targetId != null && !targetId.isEmpty()) ? targetId : "Unknown User";
            System.out.println("[SELECT_CHAT] Fallback headerText set to: '" + headerText + "'");
        }

        // Update chat header
        currentChatLabel.setText(headerText);
        
        // Update member status and profile picture
        updateChatHeaderUI(chat);
        
        // Only attempt key exchange for one-on-one chats
        if (!isGroupChat && !chatKeys.containsKey(chat.getChatId())) {
            if (chat.getTargetPublicKey() != null && !chat.getTargetPublicKey().isEmpty()) {
                sendKeyExchange(chat);
            }
        }
        
        System.out.println("Chat selected successfully: " + headerText);
        System.out.println("========== CHAT SELECTION COMPLETE ==========\n");
    }
    
    @FXML
    public void handleSendButton(ActionEvent event) {
        System.out.println("\n========== HANDLE SEND BUTTON CLICKED ==========");
        System.out.println("Current chat: " + (currentChat != null ? currentChat.getChatId() : "null"));
        System.out.println("Message field text: '" + messageField.getText() + "'");
        System.out.println("Message field trimmed: '" + messageField.getText().trim() + "'");
        System.out.println("Is message field empty? " + messageField.getText().trim().isEmpty());
        
        if (currentChat == null) {
            System.out.println("Cannot send: no chat selected");
            return;
        }
        
        if (messageField.getText().trim().isEmpty()) {
            System.out.println("Cannot send: message field is empty");
            return;
        }
        
        String message = messageField.getText().trim();
        System.out.println("Calling sendMessage with: '" + message + "'");
        sendMessage(message);
        messageField.clear();
        System.out.println("Message field cleared");
        System.out.println("========== HANDLE SEND BUTTON COMPLETE ==========\n");
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
    
    @FXML
    private void handleEditProfileButton(ActionEvent event) {
        try {
            // Debug auth status first
            System.out.println("DEBUG AUTH STATUS BEFORE OPENING PROFILE EDITOR:");
            System.out.println(authService.checkAuthStatus());
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/e2ee/chat/frontend/profile_editor.fxml"));
            Parent root = loader.load();
            
            // Get the controller
            ProfileEditorController controller = loader.getController();
            
            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Profile");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            
            // Get the current user profile
            UserProfile profile = null;
            try {
                profile = authService.getUserProfile();
                if (profile == null) {
                    System.err.println("Warning: getUserProfile() returned null. Creating fallback profile.");
                    // Create a default profile as fallback
                    profile = new UserProfile();
                    profile.setUsername(authService.getCurrentUsername());
                    profile.setDisplayName(authService.getCurrentUsername());
                    profile.setStatus("Online");
                }
            } catch (Exception e) {
                System.err.println("Error retrieving user profile: " + e.getMessage());
                e.printStackTrace();
                // Create a default profile as fallback
                profile = new UserProfile();
                profile.setUsername(authService.getCurrentUsername());
                profile.setDisplayName(authService.getCurrentUsername());
                profile.setStatus("Online");
            }
            
            // Set up the controller
            controller.setup(authService, profile, dialogStage, () -> {
                // Callback to run after profile is updated
                statusLabel.setText("Profile updated successfully!");
                
                // Clear message after a delay
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(() -> statusLabel.setText(""));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            });
            
            // Show the dialog and wait for it to close
            dialogStage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error opening profile editor: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleNewGroupButton(ActionEvent event) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Create Group Chat");
        dialog.setHeaderText("Enter group name and add users");

        Label nameLabel = new Label("Group Name:");
        TextField groupNameField = new TextField();
        groupNameField.setPromptText("Group name");

        Label usersLabel = new Label("Add Users:");
        TextField searchUserField = new TextField();
        searchUserField.setPromptText("Search username");
        ListView<String> userResults = new ListView<>();
        userResults.setPrefHeight(100);
        ObservableList<String> selectedUsers = FXCollections.observableArrayList();
        ListView<String> selectedUsersList = new ListView<>(selectedUsers);
        selectedUsersList.setPrefHeight(80);
        Button addUserButton = new Button("Add");
        addUserButton.setOnAction(e -> {
            String selected = userResults.getSelectionModel().getSelectedItem();
            if (selected != null && !selectedUsers.contains(selected)) {
                selectedUsers.add(selected);
            }
        });
        Button removeUserButton = new Button("Remove");
        removeUserButton.setOnAction(e -> {
            String selected = selectedUsersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedUsers.remove(selected);
            }
        });
        searchUserField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().length() > 1) {
                searchUsers(newVal.trim(), userResults);
            } else {
                userResults.getItems().clear();
            }
        });
        HBox userButtons = new HBox(5, addUserButton, removeUserButton);
        VBox vbox = new VBox(10, nameLabel, groupNameField, usersLabel, searchUserField, userResults, userButtons, new Label("Selected Users:"), selectedUsersList);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Map<String, Object> result = new HashMap<>();
                result.put("groupName", groupNameField.getText().trim());
                result.put("participants", new ArrayList<>(selectedUsers));
                return result;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(result -> {
            String groupName = (String) result.get("groupName");
            @SuppressWarnings("unchecked")
            List<String> selectedParticipants = (List<String>) result.get("participants");
            if (groupName.isEmpty() || selectedParticipants.isEmpty()) {
                showAlert("Error", "Group name and at least one user are required.");
                return;
            }
            
            // Show a status message before starting
            statusLabel.setText("Creating group chat \"" + groupName + "\"...");
            
            // Convert selected display names to user IDs
            List<String> participantIds = new ArrayList<>();
            for (String selectedDisplayName : selectedParticipants) {
                // Find the corresponding user profile
                UserProfile selectedUser = null;
                for (UserProfile user : searchResults) {
                    String displayText = user.getDisplayName() != null && !user.getDisplayName().isEmpty() 
                        ? user.getDisplayName() + " (" + user.getUsername() + ")"
                        : user.getUsername();
                    if (displayText.equals(selectedDisplayName)) {
                        selectedUser = user;
                        break;
                    }
                }
                
                if (selectedUser != null && selectedUser.getProfileId() != null) {
                    participantIds.add(selectedUser.getProfileId());
                    System.out.println("Added participant ID: " + selectedUser.getProfileId() + " for " + selectedDisplayName);
                } else {
                    System.err.println("Could not find user ID for selected participant: " + selectedDisplayName);
                }
            }
            
            if (participantIds.isEmpty()) {
                showAlert("Error", "Could not resolve participant user IDs. Please try again.");
                return;
            }
            
            // Add current user to participants if not already included
            String currentUserId = authService.getUserId();
            if (!participantIds.contains(currentUserId)) {
                participantIds.add(currentUserId);
            }
            
            System.out.println("\n========== CREATING GROUP CHAT FROM UI ==========");
            System.out.println("Group Name: " + groupName);
            System.out.println("Participant IDs: " + participantIds);
            
            // Use WebSocket to create group chat
            webSocketService.createGroupChatWS(currentUserId, groupName, participantIds);
            
            // Schedule multiple chat list refreshes with increasing delays
            // This ensures we catch the update once the server processes it
            scheduleChatRefreshWithDelay(1);  // 1 second
            scheduleChatRefreshWithDelay(3);  // 3 seconds
            scheduleChatRefreshWithDelay(5);  // 5 seconds
            
            // Update status message after a delay
            javafx.animation.PauseTransition statusUpdate = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            statusUpdate.setOnFinished(e -> {
                statusLabel.setText("Group chat \"" + groupName + "\" created successfully!");
                
                // Clear status after a few seconds
                javafx.animation.PauseTransition clearStatus = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                clearStatus.setOnFinished(ev -> statusLabel.setText(""));
                clearStatus.play();
            });
            statusUpdate.play();
        });
    }

    // Listen for GROUP_CHAT_CREATED events in your WebSocket message handler
    private void handleChatMessage(ChatMessage message) {
        System.out.println("\n========== MAIN CONTROLLER - HANDLING CHAT MESSAGE ==========");
        System.out.println("handleChatMessage triggered for chatId: " + message.getChatId());
        System.out.println("Message content: " + message.getContent());
        System.out.println("Message type: " + message.getType());
        System.out.println("Sender ID: " + message.getSenderId());
        System.out.println("Current User ID: " + authService.getUserId());
        System.out.println("Current chatId: " + (currentChat != null ? currentChat.getChatId() : "null"));
        System.out.println("Message clientTempId: " + message.getClientTempId());
        System.out.println("Is message from current user? " + (message.getSenderId().equals(authService.getUserId())));
        System.out.println("Total chats in list: " + chats.size());
        System.out.println("Current thread: " + Thread.currentThread().getName());
        System.out.println("Is JavaFX Application Thread: " + Platform.isFxApplicationThread());

        Platform.runLater(() -> {
            // Check if this is our own message being echoed back (only applies to sender)
            String clientTempId = message.getClientTempId();
            boolean isOwnMessage = message.getSenderId().equals(authService.getUserId());
            
            if (isOwnMessage && clientTempId != null && !clientTempId.isEmpty() && pendingMessages.containsKey(clientTempId)) {
                System.out.println("Found our own message echo with clientTempId: " + clientTempId);
                System.out.println("Pending messages map contains: " + pendingMessages.keySet());
                
                // This is our own message being echoed back
                // Update the server-assigned ID in the existing message
                ChatMessage existingMessage = pendingMessages.get(clientTempId);
                existingMessage.setId(message.getId());
                
                // Find the corresponding chat and update the message ID there
                Chat targetChat = null;
                for (Chat c : chats) {
                    if (c.getChatId() != null && c.getChatId().equals(message.getChatId())) {
                        targetChat = c;
                        break;
                    }
                }
                
                if (targetChat != null) {
                    // Update the message ID in the chat's message list
                    if (targetChat.getMessages() != null) {
                        for (ChatMessage chatMsg : targetChat.getMessages()) {
                            if (clientTempId.equals(chatMsg.getClientTempId())) {
                                chatMsg.setId(message.getId());
                                System.out.println("Updated message ID in chat's message list");
                                break;
                            }
                        }
                    }
                    
                    // If this is the current chat, update ID in UI as well
                    if (currentChat != null && currentChat.getChatId().equals(targetChat.getChatId())) {
                        for (ChatMessage uiMsg : messages) {
                            if (clientTempId.equals(uiMsg.getClientTempId())) {
                                uiMsg.setId(message.getId());
                                System.out.println("Updated message ID in UI");
                                break;
                            }
                        }
                        // Refresh UI to show any status updates
                        messageListView.refresh();
                    }
                }
                
                // Remove from pending messages as it's been acknowledged
                pendingMessages.remove(clientTempId);
                System.out.println("Removed message from pending messages. Remaining: " + pendingMessages.size());
                
                // Skip further processing since this message is already handled
                return;
            } else if (isOwnMessage && clientTempId != null && !clientTempId.isEmpty()) {
                System.out.println("Received our own message with clientTempId: " + clientTempId + " but not found in pending messages");
                System.out.println("Pending messages keys: " + pendingMessages.keySet());
            } else if (!isOwnMessage) {
                System.out.println("Received message from another user (sender: " + message.getSenderId() + ")");
            } else {
                System.out.println("Received message with no clientTempId");
            }

            // Find the chat this message belongs to
            Chat targetChat = null;
            for (Chat c : chats) {
                if (c.getChatId() != null && c.getChatId().equals(message.getChatId())) {
                    targetChat = c;
                    break;
                }
            }
            
            // If we didn't find the chat, create a new one
            if (targetChat == null) {
                System.out.println("Message received for unknown chat ID: " + message.getChatId());
                System.out.println("Creating a new chat entry for this message");
                
                // Create a new chat object
                targetChat = new Chat();
                targetChat.setChatId(message.getChatId());
                targetChat.setMessages(new ArrayList<>());
                
                // Set the participants (at minimum, current user and message sender)
                List<String> participants = new ArrayList<>();
                participants.add(authService.getUserId()); // Current user
                
                // Add sender if it's not the current user
                if (!message.getSenderId().equals(authService.getUserId())) {
                    participants.add(message.getSenderId());
                    
                    // For private chats, set the target user as the sender
                    targetChat.setTargetUserId(message.getSenderId());
                    
                    // Will be updated later by fetchUserProfileIfNeeded
                    targetChat.setTargetUsername(message.getSenderId());
                }
                
                targetChat.setParticipants(participants);
                targetChat.setOwnerId(authService.getUserId());
                
                // Add to our chats list
                chats.add(targetChat);
                
                // Refresh the chat list to get complete information
                scheduleChatRefreshWithDelay(1);
            }
            
            // If we found the chat, add the message to it regardless of whether it's current
            if (targetChat != null) {
                // Set the chat type for proper handling
                boolean isGroupChat = targetChat.getChatType() != null && "group".equalsIgnoreCase(targetChat.getChatType());
                
                // If encrypted and not a group chat, try to decrypt
                if (message.getType() == ChatMessage.MessageType.ENCRYPTED_CHAT && !isGroupChat) {
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
                            System.out.println("Failed to decrypt message: " + e.getMessage());
                        }
                    } else {
                        message.setContent("[Encrypted message - key exchange required]");
                        System.out.println("No encryption key available for chat: " + message.getChatId());
                    }
                }
                
                // Ensure the chat has a messages list
                if (targetChat.getMessages() == null) {
                    targetChat.setMessages(new ArrayList<>());
                }
                
                // Smart duplicate detection and message handling
                boolean isUpdate = false;
                boolean isDuplicate = false;
                String actionReason = "";
                
                System.out.println("DUPLICATE CHECK: Checking message from " + message.getSenderId() + 
                    " with clientTempId: " + message.getClientTempId() + " and messageId: " + message.getId());
                System.out.println("DUPLICATE CHECK: Chat has " + targetChat.getMessages().size() + " existing messages");
                
                // First: Check if we should UPDATE an existing message (by clientTempId)
                ChatMessage messageToUpdate = null;
                if (message.getClientTempId() != null && !message.getClientTempId().isEmpty()) {
                    for (ChatMessage existingMsg : targetChat.getMessages()) {
                        if (message.getClientTempId().equals(existingMsg.getClientTempId())) {
                            messageToUpdate = existingMsg;
                            isUpdate = true;
                            actionReason = "updating existing message with clientTempId: " + message.getClientTempId();
                            System.out.println("DUPLICATE CHECK: Found message to update by clientTempId");
                            break;
                        }
                    }
                }
                
                // Second: If not an update, check if it's a duplicate (by server message ID)
                if (!isUpdate && message.getId() != null && !message.getId().isEmpty()) {
                    for (ChatMessage existingMsg : targetChat.getMessages()) {
                        if (message.getId().equals(existingMsg.getId())) {
                            isDuplicate = true;
                            actionReason = "duplicate message with ID: " + message.getId();
                            System.out.println("DUPLICATE CHECK: Found duplicate by server message ID");
                            break;
                        }
                    }
                }
                
                // Third: Additional fallback duplicate check by content and timestamp
                if (!isUpdate && !isDuplicate) {
                    for (ChatMessage existingMsg : targetChat.getMessages()) {
                        if (message.getSenderId().equals(existingMsg.getSenderId()) &&
                                message.getContent().equals(existingMsg.getContent()) &&
                                message.getTimestamp() != null && existingMsg.getTimestamp() != null &&
                                Math.abs(ChronoUnit.SECONDS.between(message.getTimestamp(), existingMsg.getTimestamp())) < 5) {
                            isDuplicate = true;
                            actionReason = "duplicate by content, sender, and timestamp within 5 seconds";
                            System.out.println("DUPLICATE CHECK: Found duplicate by content and timestamp");
                            break;
                        }
                    }
                }
                
                // Handle the message based on what we found
                if (isUpdate && messageToUpdate != null) {
                    // Update the existing message with server data
                    System.out.println("UPDATING existing message: " + actionReason);
                    messageToUpdate.setId(message.getId());
                    messageToUpdate.setTimestamp(message.getTimestamp());
                    // Don't update content in case it was encrypted/decrypted differently
                    System.out.println("Message updated in chat " + targetChat.getChatId());
                    
                    // If this is the current chat, also update the UI
                    if (currentChat != null && currentChat.getChatId().equals(targetChat.getChatId())) {
                        for (ChatMessage uiMsg : messages) {
                            if (message.getClientTempId().equals(uiMsg.getClientTempId())) {
                                uiMsg.setId(message.getId());
                                uiMsg.setTimestamp(message.getTimestamp());
                                System.out.println("Message updated in UI");
                                break;
                            }
                        }
                        messageListView.refresh();
                    }
                } else if (isDuplicate) {
                    // Skip duplicate messages
                    System.out.println("SKIPPING duplicate message: " + actionReason);
                } else {
                    // Add new message
                    System.out.println("ADDING new message to chat " + targetChat.getChatId() + ": " + message.getContent());
                    System.out.println("Message from: " + message.getSenderId() + " (is own message: " + isOwnMessage + ")");
                    System.out.println("Chat messages before add: " + targetChat.getMessages().size());
                    targetChat.addMessage(message);
                    System.out.println("Chat messages after add: " + targetChat.getMessages().size());
                    
                    // Update display if this is the current chat
                    if (currentChat != null && currentChat.getChatId().equals(targetChat.getChatId())) {
                        System.out.println("Message is for current chat. Adding to UI...");
                        System.out.println("Current UI messages count before add: " + messages.size());
                        
                        // Add to UI if not already there
                        boolean alreadyInUI = false;
                        for (ChatMessage uiMsg : messages) {
                            if ((message.getId() != null && message.getId().equals(uiMsg.getId())) ||
                                (message.getClientTempId() != null && message.getClientTempId().equals(uiMsg.getClientTempId()))) {
                                alreadyInUI = true;
                                break;
                            }
                        }
                        
                        if (!alreadyInUI) {
                            messages.add(message);
                            messageListView.scrollTo(messages.size() - 1);
                            messageListView.refresh();
                            System.out.println("Added message to UI. New UI messages count: " + messages.size());
                        } else {
                            System.out.println("Message already exists in UI, skipping add");
                        }
                    } else {
                        // This message is for a different chat than the one currently displayed
                        System.out.println("Message received for a non-current chat: " + targetChat.getChatId());
                        if (currentChat != null) {
                            System.out.println("Current chat ID: " + currentChat.getChatId());
                        } else {
                            System.out.println("No current chat selected");
                        }
                    }
                }
                
                // Update last message preview for the chat
                if (targetChat != null && !isDuplicate && !isUpdate) {
                    targetChat.setLastMessagePreview(message.getContent());
                }
            }
            
            // Handle special message types
            if (message.getType() == ChatMessage.MessageType.KEY_EXCHANGE) {
                // Handle key exchange message
                processKeyExchange(message);
            } else if (message.getType() != null && "CHAT_CREATED".equals(message.getType().toString())) {
                // Unified handling for both private and group chat creation events
                System.out.println("Received CHAT_CREATED event, refreshing chat list");
                refreshChatList();
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
            
            // Store current messages for all chats before updating
            Map<String, List<ChatMessage>> existingMessages = new HashMap<>();
            for (Chat chat : chats) {
                if (chat.getChatId() != null && chat.getMessages() != null && !chat.getMessages().isEmpty()) {
                    existingMessages.put(chat.getChatId(), new ArrayList<>(chat.getMessages()));
                    System.out.println("Saved " + chat.getMessages().size() + " messages for chat " + chat.getChatId());
                }
            }
            
            // Save current chat ID if we have one selected
            String currentChatId = currentChat != null ? currentChat.getChatId() : null;
            
            // Additional logging to debug chat types and group names
            for (Chat chat : validChats) {
                System.out.println("Chat ID: " + chat.getChatId() 
                    + ", Type: " + chat.getChatType()
                    + ", GroupName: " + chat.getGroupName()
                    + ", TargetUsername: " + chat.getTargetUsername());
            }

            chats.clear();
            chats.addAll(validChats);

            // Restore messages that might have been lost in the update
            for (Chat chat : chats) {
                if (existingMessages.containsKey(chat.getChatId())) {
                    // Preserve messages from before the refresh
                    List<ChatMessage> savedMessages = existingMessages.get(chat.getChatId());
                    if (chat.getMessages() == null) {
                        chat.setMessages(new ArrayList<>());
                    }
                    
                    // If we have saved messages, ensure they're properly merged
                    if (!savedMessages.isEmpty()) {
                        System.out.println("Found " + savedMessages.size() + " saved messages for chat " + chat.getChatId());
                        
                        // If the chat has no messages from server, use our saved ones
                        if (chat.getMessages().isEmpty()) {
                            chat.setMessages(new ArrayList<>(savedMessages));
                            System.out.println("Restored " + savedMessages.size() + " messages for chat " + chat.getChatId());
                        } else {
                            // Merge messages, avoiding duplicates
                            System.out.println("Merging " + savedMessages.size() + " saved messages with " + 
                                            chat.getMessages().size() + " existing messages for chat " + chat.getChatId());
                            
                            for (ChatMessage msg : savedMessages) {
                                boolean exists = false;
                                
                                // First check for exact matches
                                for (ChatMessage existingMsg : chat.getMessages()) {
                                    if ((msg.getId() != null && msg.getId().equals(existingMsg.getId())) ||
                                        (msg.getClientTempId() != null && msg.getClientTempId().equals(existingMsg.getClientTempId()))) {
                                        exists = true;
                                        break;
                                    }
                                }
                                
                                // Also check for content + timestamp matches (as a fallback)
                                if (!exists && msg.getContent() != null && msg.getTimestamp() != null) {
                                    for (ChatMessage existingMsg : chat.getMessages()) {
                                        if (msg.getContent().equals(existingMsg.getContent()) && 
                                            msg.getTimestamp().equals(existingMsg.getTimestamp()) &&
                                            msg.getSenderId().equals(existingMsg.getSenderId())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                }
                                
                                if (!exists) {
                                    chat.getMessages().add(msg);
                                    System.out.println("Added message to chat " + chat.getChatId() + ": " + msg.getContent());
                                }
                            }
                            
                            // Sort messages by timestamp
                            chat.getMessages().sort(Comparator.comparing(ChatMessage::getTimestamp));
                        }
                    }
                    
                    // Update last message preview
                    if (chat.getLastMessagePreview() == null || chat.getLastMessagePreview().isEmpty()) {
                        List<ChatMessage> chatMessages = chat.getMessages();
                        if (chatMessages != null && !chatMessages.isEmpty()) {
                            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
                            chat.setLastMessagePreview(lastMessage.getContent());
                        }
                    }
                }
            }

            // For each chat, ensure its targetUsername is properly set by fetching profile if needed
            for (Chat chat : chats) { // Iterate over the 'chats' collection that was just updated
                // Check if targetUsername is null, empty, or still the placeholder (targetUserId)
                if (chat.getTargetUserId() != null && 
                    (chat.getTargetUsername() == null || 
                     chat.getTargetUsername().isEmpty() || 
                     chat.getTargetUsername().equals(chat.getTargetUserId()))) {
                    fetchUserProfileIfNeeded(chat.getTargetUserId());
                }
            }
            
            // If we had a selected chat before, try to restore the selection
            if (currentChatId != null) {
                for (Chat chat : chats) {
                    if (chat.getChatId().equals(currentChatId)) {
                        chatListView.getSelectionModel().select(chat);
                        currentChat = chat;
                        
                        // Update message display
                        messages.clear();
                        if (chat.getMessages() != null) {
                            messages.addAll(chat.getMessages());
                            if (!messages.isEmpty()) {
                                messageListView.scrollTo(messages.size() - 1);
                            }
                        }
                        break;
                    }
                }
            }
            
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
                boolean isOnline = chat.getTargetUserId() != null && onlineUsers.stream()
                    .anyMatch(u -> u.getProfileId() != null && u.getProfileId().equals(chat.getTargetUserId()));
                
                // Store online status in the chat object
                chat.setTargetUserOnline(isOnline);
            }
            
            // Refresh the chat list view to update online status indicators
            chatListView.refresh();
            
            // If there's a current chat selected, update its header UI to reflect any online status changes
            if (currentChat != null) {
                updateChatHeaderUI(currentChat);
            }
        });
    }
    
    /**
     * Fetch a user profile by ID if it's not already in the cache
     * @param profileId The profile ID to fetch
     * @return True if the profile was fetched or already exists, false otherwise
     */
    private boolean fetchUserProfileIfNeeded(String profileId) {
        if (profileId == null || profileId.isEmpty()) {
            System.out.println("[FETCH_PROFILE] Cannot fetch profile for null or empty ID");
            return false;
        }

        System.out.println("[FETCH_PROFILE] Attempting to fetch user profile for ID: " + profileId);
        UserProfile profile = authService.getUserProfileById(profileId); // Network call

        if (profile != null) {
            System.out.println("[FETCH_PROFILE] Fetched profile for " + profileId + ": Username='" + profile.getUsername() + "', DisplayName='" + profile.getDisplayName() + "'");
            userProfiles.put(profileId, profile); // Cache the fetched profile

            String bestName = profile.getDisplayName();
            if (bestName == null || bestName.isEmpty() || bestName.equalsIgnoreCase("null")) {
                bestName = profile.getUsername();
            }
            // If still no good name, use profileId as the ultimate fallback.
            if (bestName == null || bestName.isEmpty() || bestName.equalsIgnoreCase("null")) {
                bestName = profileId;
            }
            System.out.println("[FETCH_PROFILE] Determined bestName for " + profileId + ": '" + bestName + "'");

            boolean changed = false;
            for (Chat chat : chats) {
                if (chat.getTargetUserId() != null && chat.getTargetUserId().equals(profileId)) {
                    if (!bestName.equals(chat.getTargetUsername())) {
                        System.out.println("[FETCH_PROFILE] Updating chat ("+chat.getChatId()+") targetUsername for " + profileId + " from '" + chat.getTargetUsername() + "' to: '" + bestName + "'");
                        chat.setTargetUsername(bestName);
                        changed = true;
                    }
                }
            }
            // If changes were made, refresh the chat list view
            if (changed) {
                Platform.runLater(() -> chatListView.refresh());
            }
            // The list view will be refreshed by the caller (e.g., updateChatList or selectChat)
            return true;
        }
        System.out.println("[FETCH_PROFILE] Failed to fetch profile for ID: " + profileId + ". UserProfile is null from authService.");
        return false;
    }
    
    private void sendMessage(String message) {
        System.out.println("\n========== SEND MESSAGE CALLED ==========");
        System.out.println("Message to send: '" + message + "'");
        System.out.println("Current chat: " + (currentChat != null ? currentChat.getChatId() : "null"));
        System.out.println("Current chat type: " + (currentChat != null ? currentChat.getChatType() : "null"));
        System.out.println("WebSocket connected: " + webSocketService.isConnected());
        
        if (currentChat == null) {
            System.err.println("Cannot send message: no chat selected");
            return;
        }
        
        if (currentChat.getChatId() == null || currentChat.getChatId().isEmpty()) {
            System.err.println("Cannot send message: current chat has no valid ID");
            System.err.println("Current chat details: " + currentChat);
            return;
        }
        
        if (message == null || message.trim().isEmpty()) {
            System.err.println("Cannot send empty message");
            return;
        }
        
        System.out.println("All checks passed. Proceeding to send message...");
        System.out.println("Sending message to chat: " + currentChat.getChatId() + " - " + message);
        
        // Unified approach for both group and private chats
        boolean isGroupChat = currentChat.getChatType() != null && "group".equalsIgnoreCase(currentChat.getChatType());
        String clientTempId;
        
        // Only encrypt for private chats with a shared key
        if (!isGroupChat && chatKeys.containsKey(currentChat.getChatId())) {
            SecretKey key = chatKeys.get(currentChat.getChatId());
            // Encrypt the message for private chats with established keys
            CryptoUtils.EncryptionResult encResult = CryptoUtils.encryptMessage(message, key);
            
            // Send encrypted message
            clientTempId = webSocketService.sendEncryptedMessage(
                currentChat.getChatId(),  // Always use chatId directly
                encResult.getEncryptedData(),
                "",  // No need to send encrypted key for each message
                encResult.getIv()
            );
        } else {
            // For group chats or private chats without keys, send plain message
            clientTempId = webSocketService.sendMessage(currentChat.getChatId(), message);
            
            // Only try to initiate key exchange for private chats
            if (!isGroupChat && currentChat.getTargetPublicKey() != null && !currentChat.getTargetPublicKey().isEmpty()) {
                sendKeyExchange(currentChat);
            }
        }
        
        // If clientTempId is null, generate a new one (should not happen if WebSocket is connected)
        if (clientTempId == null) {
            clientTempId = java.util.UUID.randomUUID().toString();
        }
        
        // Add to local display (showing unencrypted version)
        ChatMessage localMessage = new ChatMessage();
        SecretKey key = chatKeys.get(currentChat.getChatId()); // Retrieve the key for the current chat
        localMessage.setType(key != null ? ChatMessage.MessageType.ENCRYPTED_CHAT : ChatMessage.MessageType.MESSAGE);
        localMessage.setSenderId(authService.getUserId());
        localMessage.setContent(message);  // Show the unencrypted version locally
        localMessage.setTimestamp(LocalDateTime.now());
        localMessage.setOwn(true);
        localMessage.setClientTempId(clientTempId);  // Set the client temp ID
        localMessage.setChatId(currentChat.getChatId()); // IMPORTANT: Set the chatId
                
        // Track this message for deduplication
        pendingMessages.put(clientTempId, localMessage);
        System.out.println("Added message to pendingMessages with clientTempId: " + clientTempId);
        System.out.println("pendingMessages now contains " + pendingMessages.size() + " messages");
        
        // Make sure the current chat has a messages list
        if (currentChat.getMessages() == null) {
            currentChat.setMessages(new ArrayList<>());
        }
        
        // Add to current chat's message list
        currentChat.addMessage(localMessage);
        System.out.println("Added message to chat " + currentChat.getChatId() + ", now has " + 
                          (currentChat.getMessages() != null ? currentChat.getMessages().size() : 0) + " messages");
        
        // Update the last message preview
        currentChat.setLastMessagePreview(message);
                
        // Add to the UI messages list
        messages.add(localMessage);
        System.out.println("Added message to UI messages list. Total UI messages: " + messages.size());
        messageListView.scrollTo(messages.size() - 1);
        messageListView.refresh(); // Ensure the ListView updates
        System.out.println("Scrolled message list to position: " + (messages.size() - 1));

        // For group chats, we need to refresh the chat list after sending a message
        // but don't do it immediately to avoid losing our message
        if (currentChat != null && currentChat.getChatType() != null && 
            "group".equalsIgnoreCase(currentChat.getChatType())) {
            // Wait a bit before refreshing the chat list to give the server time to process
            System.out.println("Scheduling chat refresh for group chat");
            scheduleChatRefreshWithDelay(3);  // Increased delay to ensure message is processed first
        }
        
        System.out.println("========== SEND MESSAGE COMPLETE ==========\n");
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
        
        // Validate user profile ID
        if (user.getProfileId() == null || user.getProfileId().isEmpty()) {
            statusLabel.setText("Error: User profile ID is missing");
            return;
        }
        
        // Store the user profile for future reference
        userProfiles.put(user.getProfileId(), user);
        
        // Check if chat already exists
        Chat existingChat = chats.stream()
            .filter(c -> c.getTargetUserId() != null && c.getTargetUserId().equals(user.getProfileId()))
            .findFirst()
            .orElse(null);
        
        if (existingChat != null) {
            // Select existing chat
            chatListView.getSelectionModel().select(existingChat);
            String userName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
            statusLabel.setText("Chat with " + userName + " already exists");
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
    
    @FXML
    private void handleChatOptionsButton(ActionEvent event) {
        if (currentChat == null) {
            statusLabel.setText("No chat selected");
            return;
        }
        
        // Create context menu with styling
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 3);");
        
        // Check if this is a group chat
        boolean isGroupChat = currentChat.getChatType() != null && "group".equalsIgnoreCase(currentChat.getChatType());
        
        if (isGroupChat) {
            // Only show Admin Panel for group chats
            MenuItem adminPanelItem = createMenuItem("Admin Panel", "⚙️");
            adminPanelItem.setOnAction(e -> showAdminPanel(currentChat));
            contextMenu.getItems().add(adminPanelItem);

            MenuItem leaveGroupItem = createMenuItem("Leave Group", "🚪");
            leaveGroupItem.setOnAction(e -> handleLeaveGroup(currentChat));
            contextMenu.getItems().add(leaveGroupItem);
        } else {
            MenuItem viewProfileItem = createMenuItem("View Contact Info", "👤");
            viewProfileItem.setOnAction(e -> showContactInfo(currentChat.getTargetUserId()));
            MenuItem clearChatItem = createMenuItem("Clear Messages", "🗑️");
            clearChatItem.setOnAction(e -> handleClearChat(currentChat));
            contextMenu.getItems().addAll(viewProfileItem, clearChatItem);
        }
        
        // Common menu items
        MenuItem deleteChat = createMenuItem("Delete Chat", "❌");
        deleteChat.setOnAction(e -> handleDeleteChat(currentChat));
        
        contextMenu.getItems().add(new SeparatorMenuItem());
        contextMenu.getItems().add(deleteChat);
        
        // Show the context menu
        contextMenu.show(chatOptionsButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }
    
    // Helper method to create a styled menu item with an icon
    private MenuItem createMenuItem(String text, String icon) {
        MenuItem item = new MenuItem(icon + " " + text);
        item.setStyle("-fx-padding: 5 10 5 10; -fx-font-size: 13px;");
        return item;
    }
    
    private void showContactInfo(String userId) {
        if (userId == null || userId.isEmpty()) {
            statusLabel.setText("No user information available");
            return;
        }
        
        UserProfile profile = userProfiles.get(userId);
        if (profile == null) {
            // Try to fetch profile if not available
            fetchUserProfileIfNeeded(userId);
            profile = userProfiles.get(userId);
            
            if (profile == null) {
                statusLabel.setText("Could not retrieve user information");
                return;
            }
        }
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Contact Information");
        
        String displayName = profile.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = profile.getUsername();
        }
        
        // Don't use the header text, we'll create a custom header
        dialog.setHeaderText(null);
        
        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.setPrefWidth(350);
        
        // Create a custom profile info display
        VBox contentBox = new VBox();
        contentBox.setSpacing(0);
        
        // Create a WhatsApp-style header with green background
        VBox headerBox = new VBox();
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setSpacing(15);
        headerBox.setPadding(new Insets(20, 20, 25, 20));
        headerBox.setStyle("-fx-background-color: #128C7E;");
        
        // Profile picture (large circle with initials)
        StackPane profilePic = new StackPane();
        profilePic.setMinSize(100, 100);
        profilePic.setPrefSize(100, 100);
        
        // Set background color based on name hash
        int hashCode = Math.abs(displayName.hashCode()) % 8;
        String[] colors = {
            "#1E88E5", // Blue
            "#43A047", // Green
            "#E53935", // Red
            "#FB8C00", // Orange
            "#8E24AA", // Purple
            "#00897B", // Teal
            "#F4511E", // Deep Orange
            "#546E7A"  // Blue Grey
        };
        
        profilePic.setStyle(
            "-fx-background-color: " + colors[hashCode] + "; " +
            "-fx-background-radius: 50; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        );
        
        String initials = getInitials(displayName);
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold;");
        profilePic.getChildren().add(initialsLabel);
        
        // Display name label
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Online status
        Label onlineLabel = new Label(profile.isOnline() ? "Online" : "Last seen recently");
        onlineLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13px;");
        
        headerBox.getChildren().addAll(profilePic, nameLabel, onlineLabel);
        
        // Info section with white background
        VBox infoSection = new VBox();
        infoSection.setSpacing(15);
        infoSection.setPadding(new Insets(20));
        
        // Username info row
        HBox usernameRow = createInfoRow("Username", profile.getUsername(), "👤");
        
        // Status info row (if available)
        HBox statusRow = createInfoRow("Status", (profile.getStatus() != null && !profile.getStatus().isEmpty()) 
                                      ? profile.getStatus() : "No status set", "💬");
        
        // Email info row (if available)
        HBox emailRow = createInfoRow("Email", (profile.getEmail() != null && !profile.getEmail().isEmpty())
                                     ? profile.getEmail() : "Not provided", "✉️");
        
        infoSection.getChildren().addAll(usernameRow, statusRow, emailRow);
        
        // Add sections to main container
        contentBox.getChildren().addAll(headerBox, infoSection);
        
        // Set content and show dialog
        dialogPane.setContent(contentBox);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
    
    // Helper method to create an info row for contact details
    private HBox createInfoRow(String label, String value, String icon) {
        HBox row = new HBox();
        row.setSpacing(10);
        row.setAlignment(Pos.CENTER_LEFT);
        
        // Icon label
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");
        
        // Info container
        VBox infoContainer = new VBox(3);
        
        // Label (small gray text)
        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");
        
        // Value (larger black text)
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 14px;");
        
        infoContainer.getChildren().addAll(titleLabel, valueLabel);
        
        // Add to row
        row.getChildren().addAll(iconLabel, infoContainer);
        
        return row;
    }
    
    private void handleLeaveGroup(Chat chat) {
        // This would be implemented to communicate with the backend
        statusLabel.setText("Leave group functionality not implemented yet");
    }
    
    private void handleClearChat(Chat chat) {
        if (chat == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Chat");
        alert.setHeaderText("Clear all messages?");
        alert.setContentText("Are you sure you want to clear all messages in this chat? This cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Clear messages
            chat.getMessages().clear();
            messages.clear();
            chat.setLastMessagePreview("");
            
            // Update UI
            chatListView.refresh();
            
            statusLabel.setText("Chat cleared");
        }
    }
    
    private void handleDeleteChat(Chat chat) {
        if (chat == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Chat");
        alert.setHeaderText("Delete this chat?");
        alert.setContentText("Are you sure you want to delete this chat? This will remove the chat from your list but not for other participants.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Remove chat from list
            chats.remove(chat);
            
            // Clear current selection if this was the selected chat
            if (currentChat != null && currentChat.getChatId().equals(chat.getChatId())) {
                currentChat = null;
                messages.clear();
                currentChatLabel.setText("No chat selected");
                memberStatusLabel.setText("No members");
                messageField.setDisable(true);
                sendButton.setDisable(true);
                
                // Reset profile picture
                profileInitialsLabel.setText("");
                profilePicContainer.setStyle("-fx-background-color: #DDD; -fx-background-radius: 20;");
            }
            
            statusLabel.setText("Chat deleted");
        }
    }
    
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        
        String[] parts = name.split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(part.charAt(0));
                if (initials.length() >= 2) {
                    break;
                }
            }
        }
        
        return initials.toString().toUpperCase();
    }
    
    /**
     * Show an alert dialog with the given title and message.
     * @param title The title of the alert dialog.
     * @param message The message to display.
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Refresh the chat list by requesting the latest chats from the backend via WebSocket.
     * This is used after group chat creation or when a GROUP_CHAT_CREATED event is received.
     */
    private void refreshChatList() {
        // Use the no-arg version of requestChatList, which uses the stored userId
        webSocketService.requestChatList();
    }
    
    // Helper method to schedule a chat list refresh with a delay
    private void scheduleChatRefreshWithDelay(int delaySeconds) {
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(delaySeconds));
        pause.setOnFinished(e -> {
            System.out.println("Refreshing chat list after " + delaySeconds + " seconds delay");
            refreshChatList();
        });
        pause.play();
    }
    
    // Custom cell for chat list
    private class ChatListCell extends ListCell<Chat> {
        private HBox content;
        private StackPane avatarContainer;
        private Label initialsLabel;
        private VBox textContainer;
        private Label nameLabel;
        private HBox previewContainer;
        private javafx.scene.shape.Circle statusIndicator;
        private Label previewLabel;
        private Label timeLabel;
        
        public ChatListCell() {
            super();
            
            // Avatar container (circle with initials)
            avatarContainer = new StackPane();
            avatarContainer.setMinSize(45, 45);
            avatarContainer.setPrefSize(45, 45);
            avatarContainer.setMaxSize(45, 45);
            avatarContainer.setStyle("-fx-background-radius: 22.5;");
            
            initialsLabel = new Label();
            initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            avatarContainer.getChildren().add(initialsLabel);
            
            // Status indicator
            statusIndicator = new javafx.scene.shape.Circle(5);
            statusIndicator.setStroke(javafx.scene.paint.Color.WHITE);
            statusIndicator.setStrokeWidth(1);
            
            // Text container
            textContainer = new VBox(3);
            textContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            // Name label
            nameLabel = new Label();
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            // Preview container with message preview and time
            previewContainer = new HBox();
            previewContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            // Preview label
            previewLabel = new Label();
            previewLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            previewLabel.setMaxWidth(180);  // Limit width to ensure time is visible
            previewLabel.setMinHeight(16);  // Ensure consistent height
            HBox.setHgrow(previewLabel, Priority.ALWAYS);
            
            // Time label
            timeLabel = new Label();
            timeLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
            timeLabel.setMinWidth(40);  // Reserve space for time
            timeLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            
            previewContainer.getChildren().addAll(previewLabel, timeLabel);
            
            textContainer.getChildren().addAll(nameLabel, previewContainer);
            HBox.setHgrow(textContainer, Priority.ALWAYS);
            
            // Create main container
            content = new HBox(10);
            content.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            content.getChildren().addAll(avatarContainer, textContainer);
            
            // Set a little space between cells
            setStyle("-fx-border-color: #F2F2F2; -fx-border-width: 0 0 1 0;");
        }
        
        @Override
        protected void updateItem(Chat item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String displayName;
                boolean isGroupChat = item.getChatType() != null && "group".equalsIgnoreCase(item.getChatType());
                
                // Set avatar background color and initials
                if (isGroupChat) {
                    // Group chat avatar
                    avatarContainer.setStyle("-fx-background-color: #0084FF; -fx-background-radius: 22.5;");
                    initialsLabel.setText("👥");
                    
                    // Set group name
                    displayName = item.getGroupName();
                    if (displayName == null || displayName.trim().isEmpty() || "null".equalsIgnoreCase(displayName.trim())) {
                        displayName = "Group Chat";
                    }
                    
                    // Add participant count if available
                    if (item.getParticipants() != null && !item.getParticipants().isEmpty()) {
                        displayName += " (" + item.getParticipants().size() + ")";
                    }
                } else {
                    // Individual chat avatar
                    displayName = item.getTargetUsername();
                    String targetId = item.getTargetUserId();
                    
                    // Handle missing or placeholder targetUsername
                    if (displayName == null || displayName.trim().isEmpty() || 
                        "null".equalsIgnoreCase(displayName.trim()) || 
                        (targetId != null && displayName.equals(targetId))) {
                        
                        // Try to get from cached user profiles first
                        if (targetId != null && userProfiles.containsKey(targetId)) {
                            UserProfile profile = userProfiles.get(targetId);
                            if (profile.getDisplayName() != null && !profile.getDisplayName().isEmpty()) {
                                displayName = profile.getDisplayName();
                            } else if (profile.getUsername() != null && !profile.getUsername().isEmpty()) {
                                displayName = profile.getUsername();
                            }
                        }
                        
                        // Still no good name? Use fallback and trigger async fetch
                        if (displayName == null || displayName.trim().isEmpty() || 
                            "null".equalsIgnoreCase(displayName.trim()) || 
                            (targetId != null && displayName.equals(targetId))) {
                            displayName = (targetId != null && !targetId.isEmpty()) ? targetId : "Unknown User";
                            
                            // Trigger async profile fetch to improve future display
                            if (targetId != null && !targetId.isEmpty()) {
                                new Thread(() -> {
                                    fetchUserProfileIfNeeded(targetId);
                                }).start();
                            }
                        }
                    }
                    
                    // Set initials and background color
                    String initials = getInitials(displayName);
                    initialsLabel.setText(initials);
                    
                    // Set background color based on name hash
                    int hashCode = Math.abs(displayName.hashCode()) % 8;
                    String[] colors = {
                        "#1E88E5", // Blue
                        "#43A047", // Green
                        "#E53935", // Red
                        "#FB8C00", // Orange
                        "#8E24AA", // Purple
                        "#00897B", // Teal
                        "#F4511E", // Deep Orange
                        "#546E7A"  // Blue Grey
                    };
                    avatarContainer.setStyle("-fx-background-color: " + colors[hashCode] + "; -fx-background-radius: 22.5;");
                }
                
                // Set name and message preview
                nameLabel.setText(displayName);
                previewLabel.setText(item.getLastMessagePreview() != null ? item.getLastMessagePreview() : "");
                
                // Set time of last message
                LocalDateTime lastTimestamp = null;
                if (item.getMessages() != null && !item.getMessages().isEmpty()) {
                    ChatMessage lastMsg = item.getMessages().get(item.getMessages().size() - 1);
                    lastTimestamp = lastMsg.getTimestamp();
                }
                timeLabel.setText(lastTimestamp != null ? formatTimestamp(lastTimestamp) : "");
                
                // Set online status indicator
                if (isGroupChat) {
                    // No status indicator for groups
                    statusIndicator.setVisible(false);
                } else {
                    statusIndicator.setVisible(true);
                    statusIndicator.setFill(item.isTargetUserOnline() ? 
                                          javafx.scene.paint.Color.web("#43A047") : // Green when online
                                          javafx.scene.paint.Color.LIGHTGRAY);      // Gray when offline
                    
                    // Create a container for avatar and status indicator
                    StackPane avatarWithStatus = new StackPane();
                    avatarWithStatus.getChildren().addAll(avatarContainer);
                    
                    // Position status indicator at bottom-right of avatar
                    StackPane.setAlignment(statusIndicator, javafx.geometry.Pos.BOTTOM_RIGHT);
                    avatarWithStatus.getChildren().add(statusIndicator);
                    
                    // Update content layout
                    content.getChildren().clear();
                    content.getChildren().addAll(avatarWithStatus, textContainer);
                }
                
                setGraphic(content);
            }
        }
        
        // Format timestamp for chat list (e.g., "10:30 AM" or "Yesterday" or "May 20")
        private String formatTimestamp(LocalDateTime timestamp) {
            if (timestamp == null) return "";
            
            LocalDateTime now = LocalDateTime.now();
            
            // Today: show time
            if (timestamp.toLocalDate().equals(now.toLocalDate())) {
                return timestamp.format(DateTimeFormatter.ofPattern("h:mm a"));
            }
            
            // Yesterday: show "Yesterday"
            if (timestamp.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
                return "Yesterday";
            }
            
            // This week: show day name
            if (timestamp.isAfter(now.minusDays(7))) {
                return timestamp.format(DateTimeFormatter.ofPattern("E"));
            }
            
            // Older: show date
            return timestamp.format(DateTimeFormatter.ofPattern("MMM d"));
        }
    }
    
    // Custom cell for message list with WhatsApp-style bubbles
    private class MessageListCell extends ListCell<ChatMessage> {
        private HBox container;
        private VBox messageBox;
        private Label messageLabel;
        private Label timeLabel;
        private final String CURRENT_USER_BUBBLE_STYLE = "-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);";
        private final String OTHER_USER_BUBBLE_STYLE = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);";
        
        public MessageListCell() {
            super();
            container = new HBox(10);
            container.setPadding(new Insets(5, 10, 5, 10));
            
            messageBox = new VBox(3);
            messageBox.setPadding(new Insets(8, 12, 8, 12));
            messageBox.setMaxWidth(300);  // Limit width of message bubbles
            
            messageLabel = new Label();
            messageLabel.setWrapText(true);
            messageLabel.setStyle("-fx-font-size: 14px;");
            
            timeLabel = new Label();
            timeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
            timeLabel.setAlignment(Pos.BOTTOM_RIGHT);
            
            messageBox.getChildren().addAll(messageLabel, timeLabel);
        }
        
        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                // Clear previous content to prevent duplication
                messageBox.getChildren().clear();
                container.getChildren().clear();
                
                messageLabel.setText(item.getContent());
                
                // Format and display timestamp
                if (item.getTimestamp() != null) {
                    timeLabel.setText(item.getTimestamp().format(DateTimeFormatter.ofPattern("h:mm a")));
                } else {
                    timeLabel.setText("");
                }
                
                // Check if message is from current user
                boolean isCurrentUser = authService.getUserId().equals(item.getSenderId());
                
                // Style based on sender (right-aligned green bubbles for current user)
                if (isCurrentUser) {
                    container.setAlignment(Pos.CENTER_RIGHT);
                    messageBox.setStyle(CURRENT_USER_BUBBLE_STYLE);
                    
                    // Create status box with time and check mark
                    HBox statusBox = new HBox(3);
                    statusBox.setAlignment(Pos.BOTTOM_RIGHT);
                    
                    Label statusLabel = new Label("✓");
                    statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
                    statusBox.getChildren().addAll(timeLabel, statusLabel);
                    
                    // Add content: message + status
                    messageBox.getChildren().addAll(messageLabel, statusBox);
                } else {
                    container.setAlignment(Pos.CENTER_LEFT);
                    messageBox.setStyle(OTHER_USER_BUBBLE_STYLE);
                    
                    // Add sender name for group chats
                    if (currentChat != null && "group".equalsIgnoreCase(currentChat.getChatType())) {
                        UserProfile sender = userProfiles.get(item.getSenderId());
                        String senderName = "Unknown";
                        
                        if (sender != null) {
                            senderName = sender.getDisplayName();
                            if (senderName == null || senderName.isEmpty()) {
                                senderName = sender.getUsername();
                            }
                        }
                        
                        Label senderLabel = new Label(senderName);
                        senderLabel.setStyle("-fx-text-fill: #1565C0; -fx-font-weight: bold; -fx-font-size: 12px;");
                        
                        // Add content: sender name + message + time
                        messageBox.getChildren().addAll(senderLabel, messageLabel, timeLabel);
                    } else {
                        // For private chats, just message + time
                        messageBox.getChildren().addAll(messageLabel, timeLabel);
                    }
                }
                
                container.getChildren().add(messageBox);
                setGraphic(container);
            }
        }
    }
    
    /**
     * Search for users by username and update the given ListView with results.
     * @param query The username query string.
     * @param userResults The ListView to update with found usernames.
     */
    private void searchUsers(String query, ListView<String> userResults) {
        new Thread(() -> {
            UserProfile[] results = authService.searchUsers(query);
            Platform.runLater(() -> {
                userResults.getItems().clear();
                // Clear the stored user profiles map for this search
                searchResults.clear();
                
                if (results != null && results.length > 0) {
                    for (UserProfile user : results) {
                        // Avoid showing current user
                        if (!user.getUsername().equals(authService.getCurrentUsername())) {
                            // Display format: "DisplayName (username)" or just "username" if no display name
                            String displayText = user.getDisplayName() != null && !user.getDisplayName().isEmpty() 
                                ? user.getDisplayName() + " (" + user.getUsername() + ")"
                                : user.getUsername();
                            userResults.getItems().add(displayText);
                            
                            // Store the user profile for later ID lookup
                            searchResults.add(user);
                            
                            System.out.println("Added user to search results: " + displayText + " with ID: " + user.getProfileId());
                        }
                    }
                }
            });
        }).start();
    }
    
    /**
     * Updates the chat header UI with profile picture, status, and member information
     * @param chat The currently selected chat
     */
    private void updateChatHeaderUI(Chat chat) {
        if (chat == null) {
            // Reset UI elements
            profileInitialsLabel.setText("");
            profilePicContainer.setStyle("-fx-background-color: #DDD; -fx-background-radius: 22.5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);");
            memberStatusLabel.setText("No members");
            return;
        }
        
        boolean isGroupChat = chat.getChatType() != null && "group".equalsIgnoreCase(chat.getChatType());
        
        // Update profile picture / avatar
        if (isGroupChat) {
            // Group chat icon with blue background
            profileInitialsLabel.setText("👥");
            profilePicContainer.setStyle("-fx-background-color: #0084FF; -fx-background-radius: 22.5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);");
            
            // Update member status for group chats
            int totalMembers = (chat.getParticipants() != null) ? chat.getParticipants().size() : 0;
            int onlineMembers = 0;
            
            if (chat.getParticipants() != null) {
                for (String memberId : chat.getParticipants()) {
                    UserProfile profile = userProfiles.get(memberId);
                    if (profile != null && profile.isOnline()) {
                        onlineMembers++;
                    }
                }
            }
            
            // Format: "5 members, 2 online" (if there are online members)
            // or simply "5 members" (if no one is online)
            if (onlineMembers > 0) {
                memberStatusLabel.setText(totalMembers + " members, " + onlineMembers + " online");
            } else {
                memberStatusLabel.setText(totalMembers + " members");
            }
        } else {
            // Individual chat - get the other user's profile
            String targetUserId = chat.getTargetUserId();
            UserProfile targetProfile = userProfiles.get(targetUserId);
            
            // Default values
            String displayName = chat.getTargetUsername();
            if (displayName == null || displayName.isEmpty() || displayName.equals(targetUserId)) {
                displayName = targetUserId;
            }
            
            // Set initials in the profile circle
            String initials = getInitials(displayName);
            profileInitialsLabel.setText(initials);
            
            // Set background color based on initial - consistent color for same user
            int hashCode = Math.abs(displayName.hashCode()) % 8;
            String[] colors = {
                "#1E88E5", // Blue
                "#43A047", // Green
                "#E53935", // Red
                "#FB8C00", // Orange
                "#8E24AA", // Purple
                "#00897B", // Teal
                "#F4511E", // Deep Orange
                "#546E7A"  // Blue Grey
            };
            
            profilePicContainer.setStyle(
                "-fx-background-color: " + colors[hashCode] + "; " +
                "-fx-background-radius: 22.5; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);"
            );
            
            // Update online status
            boolean isOnline = false;
            if (targetProfile != null) {
                isOnline = targetProfile.isOnline();
                if (targetProfile.getAvatarUrl() != null && !targetProfile.getAvatarUrl().isEmpty()) {
                    // TODO: Load avatar image if available
                    // For now we're just using initials
                }
            }
            if (isOnline) {
                profileInitialsLabel.setText(initials);
                memberStatusLabel.setText("Online");
                memberStatusLabel.setStyle("-fx-text-fill: #43A047;"); // Green color for online
            } else {
                memberStatusLabel.setText("Offline");
                memberStatusLabel.setStyle("-fx-text-fill: #757575;"); // Gray color for offline
            }
        }
    }
    
    private void showAdminPanel(Chat chat) {
        if (chat == null || !"group".equalsIgnoreCase(chat.getChatType())) {
            statusLabel.setText("Admin panel is only available for group chats");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Admin Panel");
        dialog.setHeaderText(chat.getGroupName() + " - Admin Controls");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.setPrefWidth(450);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Participants Management Tab
        Tab participantsTab = new Tab("Participants");
        VBox participantsContent = new VBox(10);
        participantsContent.setPadding(new Insets(10));

        ListView<HBox> participantsListView = new ListView<>();
        ObservableList<HBox> participantItems = FXCollections.observableArrayList();

        for (String participantId : chat.getParticipants()) {
            UserProfile profile = userProfiles.get(participantId);
            if (profile != null) {
                String displayName = profile.getDisplayName();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = profile.getUsername();
                }
                boolean isCurrentUser = participantId.equals(authService.getUserId());
                if (isCurrentUser) {
                    displayName += " (You)";
                }
                final String finalDisplayName = displayName;
                HBox participantRow = createAdminParticipantRow(
                    displayName,
                    profile.isOnline(),
                    isCurrentUser,
                    chat.isAdmin(participantId),
                    e -> handlePromoteToAdmin(chat, participantId, finalDisplayName),
                    e -> handleRemoveParticipant(chat, participantId, finalDisplayName)
                );
                participantItems.add(participantRow);
            }
        }
        participantsListView.setItems(participantItems);
        participantsContent.getChildren().add(participantsListView);
        participantsTab.setContent(participantsContent);

        // Settings Tab
        Tab settingsTab = new Tab("Settings");
        VBox settingsContent = new VBox(15);
        settingsContent.setPadding(new Insets(15));

        // Group Name Setting
        Label groupNameLabel = new Label("Group Name");
        TextField groupNameField = new TextField(chat.getGroupName());
        Button updateNameButton = new Button("Update Name");
        updateNameButton.setOnAction(e -> handleUpdateGroupName(chat, groupNameField.getText()));

        // Chat Image Setting (placeholder)
        Label chatImgLabel = new Label("Chat Image");
        Button changeImgButton = new Button("Change Image");
        changeImgButton.setOnAction(e -> {
            // TODO: Implement image picker and backend update
            showAlert("Not implemented", "Chat image change is not implemented yet.");
        });

        // Message Retention Setting
        Label retentionLabel = new Label("Message Retention");
        ComboBox<String> retentionComboBox = new ComboBox<>();
        retentionComboBox.getItems().addAll(
            "Forever",
            "1 Week",
            "1 Month",
            "3 Months"
        );
        retentionComboBox.setValue("Forever");
        retentionComboBox.setDisable(true); // TODO: Implement in future

        // Encryption Setting
        Label encryptionLabel = new Label("End-to-End Encryption");
        CheckBox encryptionCheckBox = new CheckBox();
        encryptionCheckBox.setSelected(true);
        encryptionCheckBox.setDisable(true); // Always enabled

        // Add settings to the content
        settingsContent.getChildren().addAll(
            groupNameLabel,
            new HBox(10, groupNameField, updateNameButton),
            new HBox(10, chatImgLabel, changeImgButton),
            new Separator(),
            retentionLabel,
            retentionComboBox,
            new Separator(),
            new HBox(10, encryptionLabel, encryptionCheckBox)
        );
        settingsTab.setContent(settingsContent);

        // Add tabs to the pane
        tabPane.getTabs().addAll(participantsTab, settingsTab);
        dialogPane.setContent(tabPane);

        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private HBox createAdminParticipantRow(String displayName, boolean isOnline, boolean isCurrentUser, 
                                         boolean isAdmin, EventHandler<ActionEvent> onPromote, 
                                         EventHandler<ActionEvent> onRemove) {
        HBox rowContainer = new HBox();
        rowContainer.setAlignment(Pos.CENTER_LEFT);
        rowContainer.setSpacing(10);
        rowContainer.setPadding(new Insets(8, 10, 8, 10));

        // Create avatar circle with initials
        StackPane avatar = new StackPane();
        avatar.setMinSize(35, 35);
        avatar.setPrefSize(35, 35);
        avatar.setMaxSize(35, 35);

        String initials = getInitials(displayName);
        int hashCode = Math.abs(displayName.hashCode()) % 8;
        String[] colors = {
            "#1E88E5", "#43A047", "#E53935", "#FB8C00",
            "#8E24AA", "#00897B", "#F4511E", "#546E7A"
        };

        avatar.setStyle("-fx-background-color: " + colors[hashCode] + "; -fx-background-radius: 17.5;");

        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        avatar.getChildren().add(initialsLabel);

        // User info section
        VBox userInfo = new VBox();
        userInfo.setSpacing(2);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        Label nameLabel = new Label(displayName + (isAdmin ? " 👑" : ""));
        nameLabel.setStyle("-fx-font-weight: " + (isCurrentUser ? "bold" : "normal") + ";");

        Label statusLabel = new Label(isOnline ? "Online" : "Offline");
        statusLabel.setStyle("-fx-text-fill: " + (isOnline ? "#43A047" : "#757575") + "; -fx-font-size: 11px;");

        userInfo.getChildren().addAll(nameLabel, statusLabel);

        // Action buttons
        HBox actionButtons = new HBox(5);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        if (!isCurrentUser && !isAdmin) {
            Button promoteButton = new Button("Promote");
            promoteButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            promoteButton.setOnAction(onPromote);
            actionButtons.getChildren().add(promoteButton);
        }

        if (!isCurrentUser) {
            Button removeButton = new Button("Remove");
            removeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            removeButton.setOnAction(onRemove);
            actionButtons.getChildren().add(removeButton);
        }

        rowContainer.getChildren().addAll(avatar, userInfo);
        if (!actionButtons.getChildren().isEmpty()) {
            rowContainer.getChildren().add(actionButtons);
        }

        return rowContainer;
    }

    private void handlePromoteToAdmin(Chat chat, String userId, String displayName) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Promote to Admin");
        confirm.setHeaderText("Promote " + displayName + " to Admin?");
        confirm.setContentText("This user will have full administrative privileges in the group.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // TODO: Implement backend call to promote user
                webSocketService.promoteToAdmin(chat.getChatId(), userId);
                statusLabel.setText("Promoted " + displayName + " to admin");
            }
        });
    }

    private void handleRemoveParticipant(Chat chat, String userId, String displayName) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Participant");
        confirm.setHeaderText("Remove " + displayName + " from group?");
        confirm.setContentText("This user will no longer be able to send or receive messages in this group.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // TODO: Implement backend call to remove user
                webSocketService.removeFromGroup(chat.getChatId(), userId);
                statusLabel.setText("Removed " + displayName + " from group");
            }
        });
    }

    private void handleUpdateGroupName(Chat chat, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            statusLabel.setText("Group name cannot be empty");
            return;
        }

        // TODO: Implement backend call to update group name
        webSocketService.updateGroupName(chat.getChatId(), newName.trim());
        statusLabel.setText("Group name updated to: " + newName.trim());
    }
}