package com.e2ee.chat;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.service.WebSocketService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
    private final ScrollPane messageScrollPane;
    private final Map<String, ChatListItem> chatItems;
    private String selectedChat;
    private WebSocketService webSocketService;
    private final TextField searchField;

    public ChatView() {
        chatItems = new HashMap<>();
        
        // Create chat list (left side)
        VBox leftSide = new VBox(10);
        leftSide.setPadding(new Insets(10));
        leftSide.setPrefWidth(300);
        leftSide.setStyle("-fx-background-color: #f0f0f0;");

        // Create search field
        searchField = new TextField();
        searchField.setPromptText("Search chats...");
        searchField.setPrefHeight(40);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterChats(newVal));

        chatList = new VBox(10);
        chatList.setPadding(new Insets(10));

        leftSide.getChildren().addAll(searchField, chatList);

        // Create message area (right side)
        messageArea = new VBox(10);
        messageArea.setPadding(new Insets(10));
        messageArea.setStyle("-fx-background-color: white;");

        // Create message input area
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(10));
        inputArea.setStyle("-fx-background-color: #f0f0f0;");

        messageInput = new TextField();
        messageInput.setPromptText("Type a message...");
        messageInput.setPrefHeight(40);
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        sendButton = new Button("Send");
        sendButton.setPrefHeight(40);
        sendButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white;");

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

    private void filterChats(String searchText) {
        chatList.getChildren().clear();
        chatItems.values().stream()
            .filter(item -> item.getName().toLowerCase().contains(searchText.toLowerCase()))
            .forEach(item -> chatList.getChildren().add(item));
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
        webSocketService.sendMessage(message, selectedChat);
        messageInput.clear();
    }

    public void setWebSocketService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
        
        // Listen for new messages
        webSocketService.getMessages().addListener((javafx.collections.ListChangeListener.Change<? extends ChatMessage> change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ChatMessage message : change.getAddedSubList()) {
                        handleNewMessage(message);
                    }
                }
            }
        });

        // Listen for user status changes
        webSocketService.getUserStatus().addListener((javafx.collections.MapChangeListener.Change<? extends String, ? extends Boolean> change) -> {
            if (change.wasAdded() || change.wasRemoved()) {
                String username = change.getKey();
                ChatListItem chatItem = chatItems.get(username);
                if (chatItem != null) {
                    chatItem.setOnline(change.getValueAdded());
                }
            }
        });
    }

    private void handleNewMessage(ChatMessage message) {
        Platform.runLater(() -> {
            String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            
            switch (message.getType()) {
                case CHAT:
                    boolean isSent = message.getSender().equals(webSocketService.getCurrentUser());
                    MessageBubble bubble = new MessageBubble(message.getContent(), currentTime, isSent);
                    messageArea.getChildren().add(bubble);
                    
                    // Update chat list item
                    String chatPartner = isSent ? message.getRecipient() : message.getSender();
                    ChatListItem chatItem = chatItems.get(chatPartner);
                    if (chatItem != null) {
                        chatItem.setLastMessage(message.getContent());
                        chatItem.setTime(currentTime);
                    }
                    break;
                    
                case JOIN:
                    // Handle user joined notification
                    break;
                    
                case LEAVE:
                    // Handle user left notification
                    break;
            }
            
            // Scroll to bottom
            messageScrollPane.setVvalue(1.0);
        });
    }

    public void addChat(String username, String lastMessage, String time) {
        ChatListItem chatItem = new ChatListItem(username, lastMessage, time);
        chatItem.setOnMouseClicked(e -> selectChat(username));
        chatItems.put(username, chatItem);
        chatList.getChildren().add(chatItem);
    }

    private void selectChat(String username) {
        selectedChat = username;
        chatHeader.setText(username);
        messageArea.getChildren().clear();
        
        // Update selected chat style
        chatItems.values().forEach(item -> 
            item.setStyle("-fx-background-color: white; -fx-cursor: hand;"));
        chatItems.get(username).setStyle("-fx-background-color: #e0e0e0; -fx-cursor: hand;");
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
} 