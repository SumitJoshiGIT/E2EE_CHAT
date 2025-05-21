# Backend Implementation Tutorial

This guide provides step-by-step instructions for implementing the backend of the E2EE Chat application. For architecture overview, see [Backend Description](backend_desc.md).

> **Related Documentation**
> - For architecture overview, see [Backend Description](backend_desc.md)
> - For encryption implementation, see [Encryption Implementation](encryption.md)
> - For frontend integration, see [Integration Guide](integration.md)
> - For frontend implementation, see [Frontend Implementation](frontend_impl.md)

## 1. Setting Up the Spring Boot Backend

### Project Setup

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

### Application Properties

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

## 2. Domain Models

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

## 3. Security Configuration

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

## 4. JWT Authentication

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

## 5. WebSocket Configuration

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

## 6. Authentication Controller

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
```

## 7. Request/Response DTOs

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

## 8. Encryption Utilities

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
