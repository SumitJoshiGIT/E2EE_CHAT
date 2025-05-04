package com.e2ee.chat.controller;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.service.WebSocketService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private ListView<ChatMessage> messageList;
    @FXML private ListView<String> userList;
    
    @Autowired
    private WebSocketService webSocketService;
    private String selectedUser;
    
    @FXML
    public void initialize() {
        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());
        
        userList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                messageList.getItems().clear();
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
                        if (message.getSender().equals(selectedUser) || message.getRecipient().equals(selectedUser)) {
                            messageList.getItems().add(message);
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
                        if (!user.equals(webSocketService.getCurrentUser())) {
                            userList.getItems().add(user);
                        }
                    }
                }
                if (change.wasRemoved()) {
                    for (String user : change.getRemoved()) {
                        userList.getItems().remove(user);
                    }
                }
            }
        });
    }
    
    @FXML
    public void sendMessage() {
        if (selectedUser == null) {
            showAlert("Error", "Please select a user to chat with");
            return;
        }
        
        String message = messageInput.getText();
        if (!message.isEmpty()) {
            webSocketService.sendMessage(message, selectedUser);
            messageInput.clear();
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 