# Frontend Implementation Tutorial

This guide provides step-by-step instructions for implementing the frontend of the E2EE Chat application using JavaFX. For architecture overview, see [Frontend Description](frontend_desc.md).

> **Related Documentation**
> - For architecture overview, see [Frontend Description](frontend_desc.md)
> - For encryption implementation, see [Encryption Implementation](encryption.md)
> - For backend integration, see [Integration Guide](integration.md)
> - For backend implementation, see [Backend Implementation](backend_impl.md)

## 1. Application Main Class

Start with the JavaFX application main class:

```java
// E2EEChatFrontendApplication.java
public class E2EEChatFrontendApplication extends Application {

    private static AuthService authService;
    private static WebSocketService webSocketService;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        authService = new AuthService();
        webSocketService = new WebSocketService(authService);

        showLoginScreen();
        
        primaryStage.setTitle("E2EE Chat");
        primaryStage.show();
    }
    
    public static void showLoginScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(E2EEChatFrontendApplication.class.getResource("/com/e2ee/chat/frontend/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 400, 500);
        primaryStage.setScene(scene);
        
        // Set controller
        LoginController controller = loader.getController();
        controller.initialize(authService);
    }
    
    public static void showMainScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(E2EEChatFrontendApplication.class.getResource("/com/e2ee/chat/frontend/main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        
        // Set controller
        MainController controller = loader.getController();
        controller.initialize(authService, webSocketService);
        
        // Connect WebSocket
        webSocketService.connect();
    }
    
    public static AuthService getAuthService() {
        return authService;
    }
    
    public static WebSocketService getWebSocketService() {
        return webSocketService;
    }
    
    @Override
    public void stop() {
        // Disconnect WebSocket
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
```

## 2. Frontend Services

### Authentication Service

```java
// AuthService.java (Frontend)
public class AuthService {
    private static final String API_BASE_URL = "http://localhost:8080/api";
    private static final String LOGIN_URL = API_BASE_URL + "/auth/login";
    private static final String REGISTER_URL = API_BASE_URL + "/auth/register";
    private static final String PROFILE_URL = API_BASE_URL + "/profile";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private String authToken;
    private String currentUsername;
    private KeyPair keyPair;
    
    public AuthService() {
        // Generate RSA key pair for E2EE
        this.keyPair = CryptoUtils.generateKeyPair();
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
            
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }
    
    public boolean login(String username, String password) {
        try {
            // Create JSON data
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("password", password);
            
            String jsonData = objectMapper.writeValueAsString(data);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();
                
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                this.authToken = root.get("token").asText();
                this.currentUsername = username;
                
                // Store the public key in the user's profile
                updateUserPublicKey();
                
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Other methods for register, profile update, etc.
}
```

### WebSocket Service

```java
// WebSocketService.java
public class WebSocketService {
    private static final String WS_URL = "ws://localhost:8080/ws";
    
    private final AuthService authService;
    private StompSession stompSession;
    private Map<String, Consumer<ChatMessage>> messageHandlers = new HashMap<>();
    
    public WebSocketService(AuthService authService) {
        this.authService = authService;
    }
    
    public void connect() {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        String url = WS_URL + "?token=" + authService.getAuthToken();
        
        stompClient.connect(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                
                // Subscribe to user queue for private messages
                session.subscribe("/user/queue/messages", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return ChatMessage.class;
                    }
                    
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        onMessageReceived((ChatMessage) payload);
                    }
                });
                
                // Subscribe to user status updates
                session.subscribe("/topic/status", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return StatusUpdate.class;
                    }
                    
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        // Handle status updates
                    }
                });
            }
        });
    }
    
    public void sendMessage(ChatMessage message) {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/chat.sendMessage", message);
        }
    }
    
    public void registerMessageHandler(String chatId, Consumer<ChatMessage> handler) {
        messageHandlers.put(chatId, handler);
    }
    
    private void onMessageReceived(ChatMessage message) {
        Consumer<ChatMessage> handler = messageHandlers.get(message.getChatId());
        if (handler != null) {
            Platform.runLater(() -> handler.accept(message));
        }
    }
    
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }
}
```

## 3. FXML Views

### Login View

```xml
<!-- login.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<VBox xmlns:fx="http://javafx.com/fxml" 
      fx:controller="com.e2ee.chat.frontend.controller.LoginController"
      spacing="10" alignment="CENTER" styleClass="login-container">
    
    <padding>
        <Insets top="20" right="20" bottom="20" left="20"/>
    </padding>

    <Label text="E2EE Chat Login" styleClass="title-label"/>
    
    <TextField fx:id="usernameField" 
               promptText="Username"
               styleClass="input-field"/>
               
    <PasswordField fx:id="passwordField"
                   promptText="Password"
                   styleClass="input-field"/>
                   
    <Button text="Login"
            onAction="#handleLogin"
            styleClass="login-button"/>
            
    <Hyperlink text="Create Account"
               onAction="#handleCreateAccount"
               styleClass="create-account-link"/>
               
    <Label fx:id="errorLabel"
           styleClass="error-label"
           visible="false"/>
</VBox>
```

### Main Chat View

```xml
<!-- main.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.Insets?>

<HBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.e2ee.chat.frontend.controller.MainController"
      styleClass="main-container">
    
    <!-- Left sidebar with chat list -->
    <VBox styleClass="sidebar">
        <ListView fx:id="chatListView" VBox.vgrow="ALWAYS"/>
        
        <Button text="New Chat"
                onAction="#handleNewChat"
                styleClass="new-chat-button"/>
    </VBox>
    
    <!-- Main chat area -->
    <VBox HBox.hgrow="ALWAYS" styleClass="chat-area">
        <!-- Chat header -->
        <HBox styleClass="chat-header">
            <Label fx:id="chatTitleLabel"/>
            <Region HBox.hgrow="ALWAYS"/>
            <Label fx:id="statusLabel"/>
        </HBox>
        
        <!-- Message list -->
        <ListView fx:id="messageListView" 
                  VBox.vgrow="ALWAYS"
                  styleClass="message-list"/>
        
        <!-- Message input area -->
        <HBox styleClass="message-input-area">
            <TextArea fx:id="messageField"
                     promptText="Type a message..."
                     HBox.hgrow="ALWAYS"/>
                     
            <Button text="Send"
                    onAction="#handleSendMessage"
                    styleClass="send-button"/>
        </HBox>
    </VBox>
</HBox>
```

## 4. Custom List Cells

### Chat List Cell

```java
public class ChatListCell extends ListCell<Chat> {
    
    private final VBox container;
    private final Label titleLabel;
    private final Label lastMessageLabel;
    
    public ChatListCell() {
        container = new VBox(5);
        titleLabel = new Label();
        lastMessageLabel = new Label();
        
        titleLabel.getStyleClass().add("chat-title");
        lastMessageLabel.getStyleClass().add("last-message");
        
        container.getChildren().addAll(titleLabel, lastMessageLabel);
    }
    
    @Override
    protected void updateItem(Chat chat, boolean empty) {
        super.updateItem(chat, empty);
        
        if (empty || chat == null) {
            setGraphic(null);
        } else {
            titleLabel.setText(chat.getTitle());
            lastMessageLabel.setText(chat.getLastMessage());
            setGraphic(container);
        }
    }
}
```

### Message List Cell

```java
public class MessageListCell extends ListCell<ChatMessage> {
    
    private final VBox container;
    private final Label messageLabel;
    private final Label timestampLabel;
    
    public MessageListCell() {
        container = new VBox(2);
        messageLabel = new Label();
        timestampLabel = new Label();
        
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("message-text");
        timestampLabel.getStyleClass().add("message-timestamp");
        
        container.getChildren().addAll(messageLabel, timestampLabel);
    }
    
    @Override
    protected void updateItem(ChatMessage message, boolean empty) {
        super.updateItem(message, empty);
        
        if (empty || message == null) {
            setGraphic(null);
        } else {
            messageLabel.setText(message.getContent());
            timestampLabel.setText(formatTimestamp(message.getTimestamp()));
            
            // Style based on message sender
            getStyleClass().removeAll("sent-message", "received-message");
            getStyleClass().add(message.isSentByMe() ? "sent-message" : "received-message");
            
            setGraphic(container);
        }
    }
    
    private String formatTimestamp(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(timestamp);
    }
}
```

## 5. Controllers

### Login Controller

```java
public class LoginController {
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;
    
    private AuthService authService;
    
    public void initialize(AuthService authService) {
        this.authService = authService;
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }
        
        try {
            if (authService.login(username, password)) {
                E2EEChatFrontendApplication.showMainScreen();
            } else {
                showError("Invalid username or password");
            }
        } catch (Exception e) {
            showError("Login failed: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
```

### Main Controller

```java
public class MainController {
    
    @FXML
    private ListView<Chat> chatListView;
    
    @FXML
    private ListView<ChatMessage> messageListView;
    
    @FXML
    private TextArea messageField;
    
    @FXML
    private Label chatTitleLabel;
    
    @FXML
    private Label statusLabel;
    
    private AuthService authService;
    private WebSocketService webSocketService;
    private Chat currentChat;
    
    public void initialize(AuthService authService, WebSocketService webSocketService) {
        this.authService = authService;
        this.webSocketService = webSocketService;
        
        // Set up chat list
        chatListView.setCellFactory(listView -> new ChatListCell());
        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldChat, newChat) -> {
            if (newChat != null) {
                loadChat(newChat);
            }
        });
        
        // Set up message list
        messageListView.setCellFactory(listView -> new MessageListCell());
        
        // Load chats
        loadChats();
    }
    
    private void loadChats() {
        // Load user's chats from the backend
        // This would typically be done through a service call
    }
    
    private void loadChat(Chat chat) {
        currentChat = chat;
        chatTitleLabel.setText(chat.getTitle());
        
        // Register message handler for this chat
        webSocketService.registerMessageHandler(chat.getId(), this::onMessageReceived);
        
        // Load chat messages
        // This would typically be done through a service call
    }
    
    @FXML
    private void handleSendMessage() {
        if (currentChat == null || messageField.getText().trim().isEmpty()) {
            return;
        }
        
        String content = messageField.getText().trim();
        
        // Create and send message
        ChatMessage message = new ChatMessage(
            currentChat.getId(),
            content,
            new Date()
        );
        
        webSocketService.sendMessage(message);
        
        // Clear input field
        messageField.clear();
    }
    
    private void onMessageReceived(ChatMessage message) {
        if (message.getChatId().equals(currentChat.getId())) {
            messageListView.getItems().add(message);
            // Scroll to bottom
            messageListView.scrollTo(messageListView.getItems().size() - 1);
        }
    }
}
```

## 6. Styling

Create a CSS file for styling your application:

```css
/* styles.css */
.root {
    -fx-font-family: "Segoe UI", Arial, sans-serif;
    -fx-background-color: #f5f5f5;
}

/* Login Screen */
.login-container {
    -fx-background-color: white;
    -fx-padding: 20px;
    -fx-max-width: 300px;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);
}

.title-label {
    -fx-font-size: 24px;
    -fx-font-weight: bold;
}

.input-field {
    -fx-pref-width: 250px;
    -fx-pref-height: 35px;
    -fx-background-radius: 5px;
}

.login-button {
    -fx-background-color: #2196f3;
    -fx-text-fill: white;
    -fx-pref-width: 250px;
    -fx-pref-height: 35px;
    -fx-background-radius: 5px;
}

.error-label {
    -fx-text-fill: #f44336;
}

/* Main Screen */
.main-container {
    -fx-background-color: white;
}

.sidebar {
    -fx-min-width: 250px;
    -fx-background-color: #f5f5f5;
    -fx-padding: 10px;
}

.chat-area {
    -fx-background-color: white;
    -fx-padding: 10px;
}

.chat-header {
    -fx-padding: 10px;
    -fx-background-color: #f5f5f5;
    -fx-border-width: 0 0 1 0;
    -fx-border-color: #e0e0e0;
}

.message-list {
    -fx-background-color: transparent;
}

.message-input-area {
    -fx-padding: 10px;
    -fx-spacing: 10px;
    -fx-background-color: #f5f5f5;
}

/* Message Cells */
.sent-message {
    -fx-background-color: #e3f2fd;
    -fx-padding: 10px;
    -fx-background-radius: 10px;
    -fx-alignment: center-right;
}

.received-message {
    -fx-background-color: #f5f5f5;
    -fx-padding: 10px;
    -fx-background-radius: 10px;
    -fx-alignment: center-left;
}

.message-timestamp {
    -fx-text-fill: #757575;
    -fx-font-size: 11px;
}

/* Chat List Cells */
.chat-title {
    -fx-font-weight: bold;
}

.last-message {
    -fx-text-fill: #757575;
}
```
