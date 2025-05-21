# Backend Architecture and Description

This document provides an overview of the backend architecture for the E2EE Chat application. For implementation details, see [Backend Implementation](backend_impl.md).

> **Related Documentation**
> - For implementation details, see [Backend Implementation](backend_impl.md)
> - For encryption details, see [Encryption Implementation](encryption.md)
> - For frontend interaction, see [Integration Guide](integration.md)

## Overview

The backend of the E2EE Chat application is built with Java and Spring Boot, providing a secure and scalable foundation for end-to-end encrypted messaging.

## Technologies Used

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

### Additional Libraries

- **Jackson**: JSON processing for API requests/responses and object serialization
- **Lombok**: Reduces boilerplate code with annotations for getters, setters, and more
- **Java HTTP Client**: For backend-to-backend API calls
- **Validation API**: Bean validation for request data

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

## Security Considerations

- All sensitive data is encrypted
- Passwords are hashed using BCrypt
- Communication is secured with end-to-end encryption
- JWTs have configurable expiration times
- Public keys are verified before encryption
- Request logging for security auditing
