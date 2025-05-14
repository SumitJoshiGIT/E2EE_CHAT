package com.e2ee.chat.controller;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.service.WebSocketService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class ChatController {
    @FXML private ListView<String> userList;
    @FXML private TextField searchField;
    @FXML private ListView<ChatMessage> messageList;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private Label chatHeaderLabel;
    @FXML private VBox messageInputPanel;

    private WebSocketService webSocketService;
    private String currentUser;
    private String selectedUser;
    private final ObservableList<String> allUsers = FXCollections.observableArrayList();
    private final FilteredList<String> filteredUsers = new FilteredList<>(allUsers);
    private final Map<String, List<ChatMessage>> chatHistory = new HashMap<>();
    private final Set<String> onlineUsers = new HashSet<>();
    private final RestTemplate restTemplate = new RestTemplate();

    @FXML
    public void initialize() {
        // Initially hide message input panel
        messageInputPanel.setVisible(false);
        
        // Set up user list with filtered users
        userList.setItems(filteredUsers);
        userList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setText(null);
                } else {
                    String status = onlineUsers.contains(username) ? "🟢 " : "⚪ ";
                    setText(status + username);
                }
            }
        });

        // Set up search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                filteredUsers.setPredicate(null); // Show all users when search is empty
            } else {
                String searchText = newVal.toLowerCase();
                filteredUsers.setPredicate(username -> 
                    username != null && username.toLowerCase().contains(searchText));
            }
        });

        // Set up user selection
        userList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                updateChatHeader();
                loadChatHistory();
                messageInputPanel.setVisible(true);
                messageInput.requestFocus();
            }
        });

        // Set up message input
        messageInput.setOnAction(e -> sendMessage());
        sendButton.setOnAction(e -> sendMessage());

        // Set up message list
        messageList.setCellFactory(lv -> new ListCell<ChatMessage>() {
            @Override
            protected void updateItem(ChatMessage message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null || message.getSender() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    boolean isCurrentUser = currentUser != null && currentUser.equals(message.getSender());
                    String content = message.getContent() != null ? message.getContent() : "";
                    setText(String.format("%s: %s", message.getSender(), content));
                    setStyle(isCurrentUser ? "-fx-alignment: center-right;" : "-fx-alignment: center-left;");
                }
            }
        });
    }

    public void setWebSocketService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
        
        // Listen for new messages
        webSocketService.getMessages().addListener((javafx.collections.ListChangeListener.Change<? extends ChatMessage> change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ChatMessage message : change.getAddedSubList()) {
                        if (message != null && message.getSender() != null) {
                            Platform.runLater(() -> {
                                // For received messages, use sender as chat partner
                                // For sent messages, use recipient as chat partner
                                String chatPartner;
                                if (message.getSender().equals(currentUser)) {
                                    chatPartner = message.getRecipient();
                                } else {
                                    chatPartner = message.getSender();
                                }
                                
                                // Initialize chat history for this partner if needed
                                if (!chatHistory.containsKey(chatPartner)) {
                                    chatHistory.put(chatPartner, new ArrayList<>());
                                    if (!allUsers.contains(chatPartner)) {
                                        allUsers.add(chatPartner);
                                    }
                                }
                                
                                // Add message to chat history
                                chatHistory.get(chatPartner).add(message);
                                
                                // Update UI if this is the currently selected chat
                                if (chatPartner.equals(selectedUser)) {
                                    messageList.getItems().add(message);
                                    messageList.scrollTo(messageList.getItems().size() - 1);
                                }
                            });
                        }
                    }
                }
            }
        });

        // Listen for online users
        webSocketService.getOnlineUsers().addListener((javafx.collections.ListChangeListener.Change<? extends String> change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (String user : change.getAddedSubList()) {
                        if (user != null && !user.equals(currentUser)) {
                            Platform.runLater(() -> {
                                onlineUsers.add(user);
                                if (!allUsers.contains(user)) {
                                    allUsers.add(user);
                                }
                                userList.refresh();
                            });
                        }
                    }
                }
                if (change.wasRemoved()) {
                    for (String user : change.getRemoved()) {
                        if (user != null) {
                            Platform.runLater(() -> {
                                onlineUsers.remove(user);
                                userList.refresh();
                            });
                        }
                    }
                }
            }
        });
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
        try {
            // Connect to WebSocket first
            webSocketService.connect();
            // Set username in the header
            chatHeaderLabel.setText("Logged in as: " + username);
            // Then load users
            loadUsers();
        } catch (Exception e) {
            showAlert("Error", "Failed to connect to chat server: " + e.getMessage());
        }
    }

    private void loadUsers() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(webSocketService.getToken());
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String[]> response = restTemplate.exchange(
                "http://localhost:8080/api/users",
                HttpMethod.GET,
                entity,
                String[].class
            );

            if (response.getBody() != null) {
                Platform.runLater(() -> {
                    allUsers.clear();
                    for (String user : response.getBody()) {
                        if (user != null && !user.equals(currentUser)) {
                            allUsers.add(user);
                        }
                    }
                    // Trigger search filter update if there's any search text
                    String currentSearch = searchField.getText();
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        searchField.setText(currentSearch);
                    }
                    userList.refresh();
                });
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void loadChatHistory() {
        if (selectedUser != null) {
            List<ChatMessage> messages = chatHistory.getOrDefault(selectedUser, new ArrayList<>());
            messageList.setItems(FXCollections.observableArrayList(messages));
            if (!messages.isEmpty()) {
                messageList.scrollTo(messages.size() - 1);
            }
        }
    }

    private void updateChatHeader() {
        if (selectedUser != null) {
            String status = onlineUsers.contains(selectedUser) ? "🟢 " : "⚪ ";
            chatHeaderLabel.setText(status + selectedUser);
        }
    }

    private void sendMessage() {
        if (selectedUser == null || messageInput.getText().trim().isEmpty()) {
            return;
        }

        String content = messageInput.getText().trim();
        
        try {
            // Create message object
            ChatMessage message = new ChatMessage();
            message.setType(ChatMessage.MessageType.CHAT);
            message.setSender(currentUser);
            message.setRecipient(selectedUser);
            message.setContent(content);
            
            // Send the message
            webSocketService.sendMessage(content, selectedUser);
            
            // Update local chat history
            if (!chatHistory.containsKey(selectedUser)) {
                chatHistory.put(selectedUser, new ArrayList<>());
            }
            chatHistory.get(selectedUser).add(message);
            
            // Update UI
            messageList.getItems().add(message);
            messageList.scrollTo(messageList.getItems().size() - 1);
            
            messageInput.clear();
        } catch (Exception e) {
            showAlert("Error", "Failed to send message: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
} 