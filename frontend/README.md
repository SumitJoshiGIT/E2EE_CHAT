# E2EE Chat Frontend

A JavaFX-based frontend for the E2EE Chat application.

## Prerequisites

- Java 17 or later
- Maven 3.6.0 or later

## Building the Application

To build the application, run:

```bash
mvn clean install
```

## Running the Application

To run the application, use:

```bash
mvn javafx:run
```

## Features

- User registration and login
- Real-time chat with online users
- End-to-end encryption (coming soon)
- Modern and responsive UI

## Configuration

The application configuration can be found in `src/main/resources/application.properties`. Key settings include:

- Server port: 8081
- WebSocket endpoint: /ws
- Backend URL: http://localhost:8080

## Dependencies

- JavaFX 21.0.1
- Spring Boot 3.1.5
- Spring Security
- Spring WebSocket
- Jackson
- Lombok

## Project Structure

```
src/main/java/com/e2ee/chat/
├── ChatApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── WebSocketEventListener.java
│   ├── WebSocketInterceptor.java
│   └── WebSocketSecurityConfig.java
├── controller/
│   ├── ChatController.java
│   ├── LoginController.java
│   ├── SignupController.java
│   └── WebSocketController.java
└── service/
    └── WebSocketService.java
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request 