# E2EE Chat Frontend

This is the frontend for an End-to-End Encrypted Chat application built with JavaFX. It provides a secure messaging platform where messages are encrypted on the sender's device and can only be decrypted on the recipient's device.

## Features

- User registration and login
- Contact search
- Real-time messaging
- End-to-End encryption with RSA and AES
- Secure key exchange
- Online/offline status indicators

## Architecture

The frontend is built with:
- JavaFX for UI
- Spring WebSocket for real-time communication
- Jackson for JSON serialization
- RSA and AES for encryption

## Building and Running

### Prerequisites

- JDK 17 or higher
- Maven

### Build

```bash
mvn clean package
```

### Run

```bash
mvn javafx:run
```

Or run the packaged JAR file:

```bash
java --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -jar target/e2ee-chat-frontend-1.0.0.jar
```

## Security Features

1. **End-to-End Encryption**: All messages are encrypted on the sender's device and can only be decrypted on the recipient's device.

2. **RSA Key Exchange**: Each user has an RSA key pair. The public key is stored on the server and shared with contacts.

3. **AES Session Keys**: For each chat, a unique AES key is generated and encrypted with the recipient's public key.

4. **No Server Decryption**: The server never has access to the decryption keys, ensuring true end-to-end encryption.

## Development

### Package Structure

- `com.e2ee.chat.frontend`: Main application class
- `com.e2ee.chat.frontend.controller`: JavaFX controllers
- `com.e2ee.chat.frontend.model`: Data models
- `com.e2ee.chat.frontend.service`: Service layer for WebSocket and authentication
- `com.e2ee.chat.frontend.crypto`: Cryptography utilities
- `com.e2ee.chat.frontend.utils`: Helper utilities
