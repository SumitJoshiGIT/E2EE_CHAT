# E2EE Chat Application

## Overview

E2EE Chat is a secure end-to-end encrypted chat application built with Java, Spring Boot, and JavaFX. It provides a robust platform for private messaging with strong encryption to ensure that only the intended recipients can read the messages.

## Backend Technologies

### Spring Boot Framework

- **Spring Boot (Core)**: Provides the foundation for the application with auto-configuration and standalone capability
- **Spring MVC**: Handles HTTP requests and RESTful API endpoints
- **Spring WebSocket**: Enables real-time bidirectional communication for instant messaging
- **Spring Security**: Manages authentication, authorization, and security aspects
- **Spring Data MongoDB**: Facilitates interaction with MongoDB database

### Database

- **MongoDB**: NoSQL document database used for storing user profiles, chat messages, and application data
- **MongoDB Replica Set**: Configuration for high availability and data redundancy

### Authentication & Security

- **JWT (JSON Web Tokens)**: Used for stateless authentication and secure information exchange
- **BCrypt**: Password hashing algorithm for secure storage of user credentials
- **OAuth2**: Integration for third-party authentication providers (Google)

### WebSocket Communication

- **STOMP (Simple Text Oriented Messaging Protocol)**: Used as the messaging protocol over WebSockets
- **SockJS**: Provides WebSocket emulation for browsers that don't support WebSockets

### Encryption

- **RSA**: Public-key cryptography for secure key exchange
- **AES**: Symmetric encryption for message content
- **Base64**: Encoding for binary data transmission

### Logging & Monitoring

- **SLF4J + Logback**: Logging framework for application events and errors
- **Spring Boot Actuator**: Provides monitoring endpoints for application health and metrics
- **MDC (Mapped Diagnostic Context)**: For correlating log messages with request IDs

### Other Libraries

- **Jackson**: JSON processing for API requests/responses and object serialization
- **Lombok**: Reduces boilerplate code with annotations for getters, setters, and more
- **Java HTTP Client**: For backend-to-backend API calls
- **Validation API**: Bean validation for request data

## Frontend Technologies

### JavaFX

- **JavaFX Core**: UI toolkit for desktop application development
- **JavaFX FXML**: XML-based UI layout definition
- **JavaFX CSS**: Styling for the user interface
- **JavaFX Controls**: UI components like buttons, text fields, and list views

### Encryption

- **JavaSE Security**: Java's security API for cryptographic operations
- **RSA Key Generation**: For creating encryption key pairs
- **AES Encryption**: For end-to-end encrypted messages

### Networking

- **WebSocket Client**: For real-time communication with the backend
- **HTTP Client**: For REST API calls to the backend
- **STOMP Client**: For structured messaging over WebSockets

## Key Components

### Backend Beans & Services

#### Configuration Beans

- **WebSecurityConfig**: Configures security, CORS, CSRF, and authentication
- **WebSocketConfig**: Sets up WebSocket endpoints and message broker
- **MongoConfig**: Configures MongoDB connection and repositories
- **JwtConfig**: Manages JWT token creation, validation, and settings

#### Controllers

- **AuthController**: Handles user authentication and registration
- **ProfileController**: Manages user profile operations
- **ChatController**: Endpoints for chat management
- **MessageController**: Handles sending and receiving messages
- **WebSocketController**: Manages WebSocket connections and notifications

#### Services

- **AuthService**: Authentication logic and token management
- **ProfileService**: User profile management and updates
- **ChatService**: Chat room creation and management
- **MessageService**: Message storage, retrieval, and encryption
- **NotificationService**: Real-time notifications via WebSockets
- **CryptoService**: Encryption and decryption utilities

#### Repositories

- **UserRepository**: Data access for user entities
- **ProfileRepository**: Data access for user profiles
- **ChatRepository**: Data access for chat rooms
- **MessageRepository**: Data access for chat messages

#### Security Filters

- **JwtAuthenticationFilter**: Validates JWT tokens in HTTP requests
- **RequestLoggingFilter**: Adds request/response logging with unique identifiers

### Frontend Components

#### Controllers

- **LoginController**: Handles user authentication UI
- **MainController**: Main application window and chat interface
- **ProfileEditorController**: Manages user profile editing
- **ChatListCell**: Custom UI component for chat room display
- **MessageListCell**: Custom UI component for message display

#### Services

- **AuthService**: Manages authentication with the backend
- **WebSocketService**: Handles real-time communication
- **CryptoUtils**: Frontend encryption utilities

#### Models

- **UserProfile**: Represents user profile data
- **Chat**: Represents a chat conversation
- **ChatMessage**: Represents a single message in a chat

## Features

1. **Secure Authentication**
   - Username/password authentication
   - OAuth2 integration (Google)
   - JWT token-based session management

2. **End-to-End Encryption**
   - RSA key pair generation for each user
   - Public key exchange for secure communication
   - AES encryption for message content
   - Secure storage of encrypted messages

3. **User Profiles**
   - Customizable display name, bio, and avatar
   - Status indicators (Online, Away, Busy, Offline)
   - Public key management

4. **Real-time Messaging**
   - Instant delivery of messages via WebSockets
   - Message status indicators (sent, delivered, read)
   - Support for text messages
   - Typing indicators

5. **Chat Management**
   - One-on-one private chats
   - Chat history persistence
   - User search functionality

6. **Security Features**
   - Request tracing with unique IDs
   - Comprehensive error handling
   - Logging for security audits
   - Session management

7. **Resilience**
   - MongoDB replica set for data redundancy
   - Connection retry mechanisms
   - Error recovery strategies

## Architecture

The application follows a client-server architecture with:

- **Backend**: Spring Boot application serving as the API and WebSocket server
- **Frontend**: JavaFX desktop client application
- **Database**: MongoDB for data persistence
- **Communication**: REST APIs for CRUD operations and WebSockets for real-time events

The architecture implements the MVC (Model-View-Controller) pattern:
- **Models**: Data entities in both backend and frontend
- **Views**: JavaFX FXML layouts and controllers
- **Controllers**: Spring MVC controllers and JavaFX controllers

## Security Considerations

- All sensitive data is encrypted
- Passwords are hashed using BCrypt
- Communication is secured with end-to-end encryption
- JWTs have configurable expiration times
- Public keys are verified before encryption
- Request logging for security auditing

## Implementation Tutorial

This section provides a step-by-step guide on how to build a similar secure chat application with code examples.

### 1. Setting Up the Spring Boot Backend

#### Project Setup

Start by setting up a Spring Boot project with Maven:

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    
    <!-- JWT Support -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
    </dependency>
    
    <!-- Lombok for reducing boilerplate -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### Application Properties

Configure your application properties:

```properties
# application.properties
spring.application.name=E2EE-Chat

# Server Configuration
server.port=8080

# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=e2ee_chat
spring.data.mongodb.auto-index-creation=true

# JWT Configuration
jwt.expiration=86400000

# WebSocket Configuration
spring.websocket.max-text-message-size=8192
spring.websocket.max-binary-message-size=8192

# Logging Configuration
logging.level.org.springframework.web=INFO
logging.level.com.e2ee.chat=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [%X{requestId}] - %msg%n
```

### 2. Domain Models

Create your domain models:

```java
// User.java
@Document(collection = "users")
@Data
@NoArgsConstructor
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String password; // Hashed
    private String email;
    private Date createdAt;
    private Date updatedAt;
    private boolean active;
    
    // Constructor for new users
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.active = true;
    }
}

// UserProfile.java
@Document(collection = "profiles")
@Data
@NoArgsConstructor
public class UserProfile {
    @Id
    private String id;
    @Indexed
    private String userId; // Reference to User
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String status;
    private String publicKey; // RSA public key for E2EE
    private boolean online;
    private Date lastSeen;
}

// Chat.java
@Document(collection = "chats")
@Data
@NoArgsConstructor
public class Chat {
    @Id
    private String id;
    private List<String> participants;
    private Date createdAt;
    private Date updatedAt;
    private boolean active;
    private Map<String, Object> metadata;
}

// Message.java
@Document(collection = "messages")
@Data
@NoArgsConstructor
public class Message {
    @Id
    private String id;
    private String chatId;
    private String senderId;
    private Map<String, String> encryptedContent; // Recipient ID -> encrypted content
    private Date timestamp;
    private boolean delivered;
    private Map<String, Date> readTimestamps;
    private String clientTempId; // For message deduplication
}
```

### 3. Security Configuration

Set up the security configuration:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/api/auth/**").permitAll();
                auth.requestMatchers("/ws/**").permitAll();
                auth.anyRequest().authenticated();
            })
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### 4. JWT Authentication

Implement JWT authentication:

```java
// JwtService.java
@Service
public class JwtService {

    @Value("${jwt.secret:defaultSecretKey}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    public String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }
    
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }
    
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

// JwtAuthenticationFilter.java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 5. WebSocket Configuration

Configure WebSockets for real-time messaging:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024)  // 64 KB
                    .setSendBufferSizeLimit(512 * 1024)  // 512 KB
                    .setSendTimeLimit(10 * 1000);  // 10 seconds
    }
}
```

### 6. Authentication Controller

Implement the authentication endpoints:

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}

// AuthService.java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Create new user
        var user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail()
        );
        
        userRepository.save(user);
        
        // Create user profile
        var profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setDisplayName(user.getUsername());
        profile.setStatus("Online");
        
        profileRepository.save(profile);
        
        // Generate JWT token
        var token = jwtService.generateToken(
                Map.of("profileId", profile.getId()),
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.emptyList()
                )
        );
        
        return new AuthResponse(token, user.getUsername());
    }
    
    public AuthResponse authenticate(AuthRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        
        // Get user data
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        var profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        
        // Generate JWT token
        var token = jwtService.generateToken(
                Map.of("profileId", profile.getId()),
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.emptyList()
                )
        );
        
        return new AuthResponse(token, user.getUsername());
    }
}
```

### 7. Request/Response DTOs

Create DTOs for your API:

```java
// AuthRequest.java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
    private String username;
    private String password;
}

// RegisterRequest.java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
}

// AuthResponse.java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
}

// UserProfileDTO.java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String status;
    private String publicKey;
    private boolean online;
}
```

### 8. Encryption Utilities

Implement encryption utilities for end-to-end encryption:

```java
// CryptoUtils.java
public class CryptoUtils {

    private static final int KEY_SIZE = 2048;
    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    
    // Generate RSA key pair
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }
    
    // Convert public key to Base64 string
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    // Convert Base64 string to public key
    public static PublicKey stringToPublicKey(String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
        return kf.generatePublic(spec);
    }
    
    // Generate AES key for session
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }
    
    // Encrypt AES key with RSA for key exchange
    public static String encryptAESKey(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(aesKey.getEncoded()));
    }
    
    // Encrypt message with AES
    public static String encryptMessage(String message, SecretKey key) throws Exception {
        byte[] iv = new byte[12]; // GCM recommended IV size
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        
        byte[] cipherText = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }
    
    // Decrypt message with AES
    public static String decryptMessage(String encryptedMessage, SecretKey key) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedMessage);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
        
        byte[] iv = new byte[12];
        byteBuffer.get(iv);
        
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);
        
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        
        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }
}
```

### 9. Frontend with JavaFX

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

### 10. Frontend Services

Implement the frontend services:

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

### 11. FXML Views

Create the JavaFX views:

```xml
<!-- login.fxml -->
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<VBox alignment="CENTER" spacing="20.0" xmlns="http://javafx.com/javafx"
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.e2ee.chat.frontend.controller.LoginController"
      stylesheets="@styles.css">
    <padding>
        <Insets top="20.0" right="20.0" bottom="20.0" left="20.0"/>
    </padding>

    <Label text="E2EE Chat" styleClass="app-title"/>
    
    <TextField fx:id="usernameField" promptText="Username" />
    <PasswordField fx:id="passwordField" promptText="Password" />
    
    <HBox spacing="10" alignment="CENTER">
        <Button text="Login" onAction="#handleLoginButton" />
        <Button text="Register" onAction="#handleRegisterButton" />
    </HBox>
    
    <Label fx:id="statusLabel" styleClass="status-label"/>
</VBox>

<!-- main.fxml -->
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns="http://javafx.com/javafx"
            xmlns:fx="http://javafx.com/fxml"
            fx:controller="com.e2ee.chat.frontend.controller.MainController"
            stylesheets="@styles.css">
            
    <left>
        <VBox spacing="10" styleClass="sidebar">
            <padding>
                <Insets top="10" right="10" bottom="10" left="10"/>
            </padding>
            
            <HBox alignment="CENTER_LEFT" spacing="10">
                <Label text="Chats" styleClass="sidebar-title"/>
                <Region HBox.hgrow="ALWAYS"/>
                <Button mnemonicParsing="false" onAction="#handleNewChatButton" text="+"/>
            </HBox>
            
            <TextField fx:id="searchField" promptText="Search users..."/>
            <Button mnemonicParsing="false" onAction="#handleSearchButton" text="Search"/>
            
            <ListView fx:id="chatListView" VBox.vgrow="ALWAYS"/>
            
            <HBox alignment="CENTER" spacing="10">
                <Button mnemonicParsing="false" onAction="#handleEditProfileButton" text="Edit Profile"/>
                <Button mnemonicParsing="false" onAction="#handleLogoutButton" text="Logout"/>
            </HBox>
        </VBox>
    </left>
    
    <center>
        <VBox>
            <HBox alignment="CENTER_LEFT" styleClass="chat-header">
                <padding>
                    <Insets top="10" right="10" bottom="10" left="10"/>
                </padding>
                
                <Label fx:id="currentChatLabel" text="Select a chat"/>
            </HBox>
            
            <ListView fx:id="messageListView" VBox.vgrow="ALWAYS"/>
            
            <HBox spacing="10" alignment="CENTER_LEFT" styleClass="message-input">
                <padding>
                    <Insets top="10" right="10" bottom="10" left="10"/>
                </padding>
                
                <TextField fx:id="messageField" promptText="Type a message..." HBox.hgrow="ALWAYS" onKeyPressed="#handleMessageFieldKeyPress"/>
                <Button fx:id="sendButton" mnemonicParsing="false" onAction="#handleSendButton" text="Send"/>
            </HBox>
        </VBox>
    </center>
    
    <bottom>
        <HBox alignment="CENTER_LEFT" styleClass="status-bar">
            <padding>
                <Insets top="5" right="10" bottom="5" left="10"/>
            </padding>
            
            <Label fx:id="statusLabel" text=""/>
        </HBox>
    </bottom>
</BorderPane>
```

### 12. Running the Application

To run both the backend and frontend:

1. Start MongoDB:
```bash
# Start MongoDB with replica set for transactions
mongod --replSet rs0 --dbpath ~/data/db --port 27017
```

2. Initialize the replica set (if first time):
```bash
mongo --eval "rs.initiate()"
```

3. Start the Spring Boot backend:
```bash
# From the backend directory
./mvnw spring-boot:run
```

4. Start the JavaFX frontend:
```bash
# From the frontend directory
java --module-path $JAVAFX_HOME/lib --add-modules javafx.controls,javafx.fxml -jar target/e2ee-chat-frontend-1.0.0.jar
```

### 13. Testing End-to-End Encryption

To verify the encryption is working:

1. Register two users
2. Send messages between them
3. Verify that messages in the database are encrypted
4. Verify that only the intended recipient can decrypt messages

```java
// Example for manual testing of encryption
public class EncryptionTest {
    public static void main(String[] args) throws Exception {
        // Generate key pairs for Alice and Bob
        KeyPair aliceKeyPair = CryptoUtils.generateKeyPair();
        KeyPair bobKeyPair = CryptoUtils.generateKeyPair();
        
        // Generate a session key
        SecretKey sessionKey = CryptoUtils.generateAESKey();
        
        // Alice encrypts the session key with Bob's public key
        String encryptedKey = CryptoUtils.encryptAESKey(sessionKey, bobKeyPair.getPublic());
        
        // Alice sends a message encrypted with the session key
        String message = "Hello Bob, this is a secret message!";
        String encryptedMessage = CryptoUtils.encryptMessage(message, sessionKey);
        
        System.out.println("Original message: " + message);
        System.out.println("Encrypted message: " + encryptedMessage);
        
        // Bob decrypts the session key with his private key
        // In real app, this happens on the other side
        
        // Bob decrypts the message with the session key
        String decryptedMessage = CryptoUtils.decryptMessage(encryptedMessage, sessionKey);
        System.out.println("Decrypted message: " + decryptedMessage);
    }
}
```

## Frontend Implementation Tutorial

This section provides a step-by-step guide on implementing the JavaFX frontend for the E2EE Chat application.

### 1. Setting Up the JavaFX Project

#### Maven Configuration

```xml
<!-- pom.xml -->
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <javafx.version>19</javafx.version>
    <jackson.version>2.14.2</jackson.version>
    <java.version>17</java.version>
</properties>

<dependencies>
    <!-- JavaFX Dependencies -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>${javafx.version}</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>${javafx.version}</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-web</artifactId>
        <version>${javafx.version}</version>
    </dependency>

    <!-- WebSocket Dependencies -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-websocket</artifactId>
        <version>6.0.6</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-messaging</artifactId>
        <version>6.0.6</version>
    </dependency>

    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>${jackson.version}</version>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.6</version>
    </dependency>
</dependencies>
```

### 2. Project Structure

```
frontend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java
│   │   │   └── com/e2ee/chat/frontend/
│   │   │       ├── E2EEChatFrontendApplication.java
│   │   │       ├── controller/
│   │   │       │   ├── LoginController.java
│   │   │       │   ├── MainController.java
│   │   │       │   └── ProfileEditorController.java
│   │   │       ├── model/
│   │   │       │   ├── UserProfile.java
│   │   │       │   ├── Chat.java
│   │   │       │   └── ChatMessage.java
│   │   │       └── service/
│   │   │           ├── AuthService.java
│   │   │           └── WebSocketService.java
│   │   └── resources/
│   │       └── com/e2ee/chat/frontend/
│   │           ├── login.fxml
│   │           ├── main.fxml
│   │           ├── profile_editor.fxml
│   │           └── styles.css
└── pom.xml
```

### 3. Module Configuration

```java
// module-info.java
module com.e2ee.chat.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires spring.websocket;
    requires spring.messaging;
    requires org.slf4j;
    
    opens com.e2ee.chat.frontend to javafx.fxml;
    opens com.e2ee.chat.frontend.controller to javafx.fxml;
    opens com.e2ee.chat.frontend.model to com.fasterxml.jackson.databind;
    
    exports com.e2ee.chat.frontend;
    exports com.e2ee.chat.frontend.controller;
    exports com.e2ee.chat.frontend.model;
    exports com.e2ee.chat.frontend.service;
}
```

### 4. Application Models

```java
// UserProfile.java
@Data
public class UserProfile {
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String status;
    private String publicKey;
    private boolean online;
    private Date lastSeen;
    
    public String getDisplayName() {
        return displayName != null && !displayName.isEmpty() ? displayName : username;
    }
}

// Chat.java
@Data
public class Chat {
    private String id;
    private List<String> participants;
    private String lastMessage;
    private Date lastMessageTime;
    private String targetUserId;
    private String targetUsername;
    private boolean hasUnreadMessages;
    
    public Chat() {
        this.participants = new ArrayList<>();
    }
}

// ChatMessage.java
@Data
public class ChatMessage {
    private String id;
    private String chatId;
    private String senderId;
    private String senderUsername;
    private Map<String, String> encryptedContent;
    private Date timestamp;
    private boolean delivered;
    private Map<String, Date> readTimestamps;
    private String clientTempId;
    
    public ChatMessage() {
        this.encryptedContent = new HashMap<>();
        this.readTimestamps = new HashMap<>();
        this.timestamp = new Date();
    }
}
```

### 5. Custom UI Components

```java
// ChatListCell.java
public class ChatListCell extends ListCell<Chat> {
    
    private final HBox content;
    private final Label nameLabel;
    private final Label lastMessageLabel;
    private final Circle statusIndicator;
    
    public ChatListCell() {
        content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(5, 10, 5, 10));
        
        VBox textContainer = new VBox(5);
        nameLabel = new Label();
        nameLabel.getStyleClass().add("chat-name");
        
        lastMessageLabel = new Label();
        lastMessageLabel.getStyleClass().add("last-message");
        
        statusIndicator = new Circle(5);
        statusIndicator.getStyleClass().add("status-indicator");
        
        textContainer.getChildren().addAll(nameLabel, lastMessageLabel);
        content.getChildren().addAll(statusIndicator, textContainer);
    }
    
    @Override
    protected void updateItem(Chat chat, boolean empty) {
        super.updateItem(chat, empty);
        
        if (empty || chat == null) {
            setGraphic(null);
        } else {
            nameLabel.setText(chat.getTargetUsername());
            lastMessageLabel.setText(chat.getLastMessage());
            
            // Update status indicator
            statusIndicator.getStyleClass().removeAll("online", "offline");
            statusIndicator.getStyleClass().add(chat.isHasUnreadMessages() ? "online" : "offline");
            
            setGraphic(content);
        }
    }
}

// MessageListCell.java
public class MessageListCell extends ListCell<ChatMessage> {
    
    private final VBox messageContainer;
    private final Label messageLabel;
    private final Label timestampLabel;
    
    public MessageListCell(String currentUserId) {
        messageContainer = new VBox(5);
        messageContainer.setPadding(new Insets(5, 10, 5, 10));
        
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        
        timestampLabel = new Label();
        timestampLabel.getStyleClass().add("timestamp");
        
        messageContainer.getChildren().addAll(messageLabel, timestampLabel);
    }
    
    @Override
    protected void updateItem(ChatMessage message, boolean empty) {
        super.updateItem(message, empty);
        
        if (empty || message == null) {
            setGraphic(null);
            setAlignment(Pos.CENTER_LEFT);
        } else {
            messageLabel.setText(message.getDecryptedContent());
            timestampLabel.setText(formatTimestamp(message.getTimestamp()));
            
            // Align messages based on sender
            boolean isSentByMe = message.getSenderId().equals(getCurrentUserId());
            setAlignment(isSentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            
            messageContainer.getStyleClass().removeAll("sent-message", "received-message");
            messageContainer.getStyleClass().add(isSentByMe ? "sent-message" : "received-message");
            
            setGraphic(messageContainer);
        }
    }
    
    private String formatTimestamp(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(timestamp);
    }
}
```

### 6. Styling

```css
/* styles.css */
.root {
    -fx-font-family: 'System';
    -fx-background-color: #f5f5f5;
}

.sidebar {
    -fx-background-color: white;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 0);
}

.chat-header {
    -fx-background-color: white;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 0);
}

.message-input {
    -fx-background-color: white;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 0);
}

.sent-message {
    -fx-background-color: #007AFF;
    -fx-text-fill: white;
    -fx-background-radius: 15;
    -fx-padding: 10;
}

.received-message {
    -fx-background-color: #E9ECEF;
    -fx-text-fill: black;
    -fx-background-radius: 15;
    -fx-padding: 10;
}

.status-indicator.online {
    -fx-fill: #28a745;
}

.status-indicator.offline {
    -fx-fill: #dc3545;
}

.chat-name {
    -fx-font-weight: bold;
    -fx-font-size: 14px;
}

.last-message {
    -fx-text-fill: #6c757d;
    -fx-font-size: 12px;
}

.timestamp {
    -fx-text-fill: #6c757d;
    -fx-font-size: 10px;
}

.button {
    -fx-background-color: #007AFF;
    -fx-text-fill: white;
    -fx-background-radius: 5;
}

.button:hover {
    -fx-background-color: #0056b3;
}
```

### 7. Starting Scripts

Create shell scripts for development and production:

```bash
#!/bin/bash
# run.sh - Development script

# Check if JAVAFX_HOME is set
if [ -z "$JAVAFX_HOME" ]; then
    echo "JAVAFX_HOME is not set"
    exit 1
fi

# Run the application with JavaFX modules
java --module-path "$JAVAFX_HOME/lib" \
     --add-modules javafx.controls,javafx.fxml,javafx.web \
     -jar target/e2ee-chat-frontend-1.0.0.jar
```

### 8. Building the Project

Create a build script:

```bash
#!/bin/bash
# build.sh

# Clean and build with Maven
mvn clean package -DskipTests

# Create a distributable package
mkdir -p dist/lib
cp target/e2ee-chat-frontend-1.0.0.jar dist/
cp -r $JAVAFX_HOME/lib/* dist/lib/

# Create launcher script
cat > dist/run.sh << 'EOF'
#!/bin/bash
java --module-path lib \
     --add-modules javafx.controls,javafx.fxml,javafx.web \
     -jar e2ee-chat-frontend-1.0.0.jar
EOF

chmod +x dist/run.sh

# Create distribution archive
tar -czf e2ee-chat-frontend.tar.gz dist/
```

### 9. Running the Frontend

To run the frontend application:

1. Set up JavaFX environment:
```bash
# Set JAVAFX_HOME
export JAVAFX_HOME=/path/to/javafx-sdk

# Add JavaFX libs to library path
export LD_LIBRARY_PATH=$JAVAFX_HOME/lib:$LD_LIBRARY_PATH
```

2. Build the project:
```bash
./build.sh
```

3. Run the application:
```bash
./run.sh
```

### 10. Development Tips

1. **Scene Builder**: Use Scene Builder to design FXML layouts
2. **Hot Reload**: Enable automatic reloading during development
3. **CSS Live Preview**: Use JavaFX Scene Builder's CSS preview
4. **Debugging**: Enable JavaFX debug logs:
```bash
java -Djavafx.verbose=true --module-path...
```

### 11. Common Issues and Solutions

1. **JavaFX Not Found**:
   - Ensure JAVAFX_HOME is set
   - Verify JavaFX SDK installation
   - Check module path in run command

2. **FXML Loading Errors**:
   - Verify file paths in resource loading
   - Check controller class names
   - Ensure proper module exports

3. **WebSocket Connection Issues**:
   - Verify backend URL configuration
   - Check authentication token
   - Enable WebSocket debug logs

4. **UI Thread Violations**:
   - Use Platform.runLater() for UI updates
   - Avoid long operations on JavaFX thread
   - Use Task/Service for background operations
