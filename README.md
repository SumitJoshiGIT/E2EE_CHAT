# E2EE Chat Application

A secure, end-to-end encrypted chat application that allows users to communicate privately with one-on-one messaging.

## Features

- 🔒 End-to-End Encryption using Signal Protocol
- 👤 User Authentication
- 👥 One-on-One Chat
- 📝 Profile Management
- 💬 Real-time Messaging
- 🔐 Secure Key Exchange

## Tech Stack

### Frontend
- JavaFX
- FXML for UI
- CSS for styling
- Signal Protocol Library for Java

### Backend
- Spring Boot
- Spring Security
- Spring WebSocket
- MongoDB
- JWT Authentication

## Prerequisites

- Java 17 or higher
- MongoDB (4.4 or higher)
- Maven 3.6+
- A modern web browser (for WebSocket support)

## Getting Started

### Installation and Setup

1. Clone the repository
   ```
   git clone https://github.com/yourusername/e2ee-chat.git
   cd e2ee-chat
   ```

2. Make sure MongoDB is running
   ```
   # Check status
   sudo systemctl status mongodb
   
   # If not running, start MongoDB
   sudo systemctl start mongodb
   ```

3. Build and run the backend
   ```
   cd backend
   ./mvnw clean package
   java -jar target/e2ee-chat-0.0.1-SNAPSHOT.jar
   ```

4. Build and run the frontend (in a separate terminal)
   ```
   cd frontend
   ./build.sh
   ```

Alternatively, you can use the provided startup script:
```
chmod +x start.sh
./start.sh
```

### Configuration

The default configuration is stored in `backend/src/main/resources/application.properties`. You can override settings by:

1. Creating an `application-dev.properties` file in the same directory
2. Running the app with: `java -jar target/e2ee-chat-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev`
- Maven
- MongoDB
- JavaFX SDK

## Setup Instructions

1. Clone the repository
2. Install dependencies:
   ```bash
   # Install backend dependencies
   cd backend
   mvn install

   # Install frontend dependencies
   cd ../frontend
   mvn install
   ```

3. Set up environment variables:
   - Create `application.properties` in the backend directory
   - Configure the necessary properties (see application.properties.example)

4. Start the development servers:
   ```bash
   # Start backend server
   cd backend
   mvn spring-boot:run

   # Start frontend application
   cd ../frontend
   mvn javafx:run
   ```

## Security Features

- End-to-End Encryption using Signal Protocol
- Secure key exchange
- Message authentication
- Perfect forward secrecy
- Double ratchet algorithm

## License

MIT

## Project Setup

### A note on Maven
Choice of Dependency management tool: Maven
Why?
- Doesn't require much setup
- Convention over configuration
- Easier to read

### Choice of Database
For now, we're using MongoDB.
Why?
- Great for prototyping

Later we can modify the code to accommodate PostgreSQL
Why?
- PostgreSQL is more scalable

### Responsibilities

## Sumit Joshi
Building the backend API