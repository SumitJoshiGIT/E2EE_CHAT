# Frontend Architecture and Description

This document provides an overview of the frontend architecture for the E2EE Chat application. For implementation details, see [Frontend Implementation](frontend_impl.md).

> **Related Documentation**
> - For implementation details, see [Frontend Implementation](frontend_impl.md)
> - For backend interaction, see [Integration Guide](integration.md)
> - For encryption handling, see [Encryption Implementation](encryption.md)

## Overview

The frontend of the E2EE Chat application is built with JavaFX, providing a desktop client application with a modern user interface and secure end-to-end encryption capabilities.

## Technologies Used

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

## Application Structure

The application follows a client-server architecture with:

- **Frontend**: JavaFX desktop client application
- **Communication**: REST APIs for CRUD operations and WebSockets for real-time events

The architecture implements the MVC (Model-View-Controller) pattern:
- **Models**: Data entities in the frontend
- **Views**: JavaFX FXML layouts
- **Controllers**: JavaFX controllers for handling user interactions
