package com.e2ee.chat.service;

import com.e2ee.chat.crypto.CryptoUtil;
import com.e2ee.chat.model.ChatMessage;
import lombok.Getter;

import java.security.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

public class EncryptionService {
    private final KeyPair keyPair;
    @Getter
    private final String publicKeyBase64;
    private final ConcurrentHashMap<String, SecretKey> sessionKeys;

    public EncryptionService() throws NoSuchAlgorithmException {
        this.keyPair = CryptoUtil.generateKeyPair();
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        this.sessionKeys = new ConcurrentHashMap<>();
    }

    public ChatMessage prepareKeyExchange(String recipient) throws Exception {
        SecretKey sessionKey = CryptoUtil.generateAESKey();
        sessionKeys.put(recipient, sessionKey);

        ChatMessage keyExchange = new ChatMessage();
        keyExchange.setType(ChatMessage.MessageType.KEY_EXCHANGE);
        keyExchange.setPublicKeyBase64(publicKeyBase64);
        keyExchange.setRecipient(recipient);
        
        return keyExchange;
    }

    public void handleKeyExchange(ChatMessage message) throws Exception {
        if (message.getType() != ChatMessage.MessageType.KEY_EXCHANGE) {
            throw new IllegalArgumentException("Invalid message type for key exchange");
        }

        // Store the session key for this sender
        SecretKey sessionKey = CryptoUtil.decryptKey(message.getEncryptedKey(), keyPair.getPrivate());
        sessionKeys.put(message.getSender(), sessionKey);
    }

    public ChatMessage encryptMessage(String recipient, String content) throws Exception {
        SecretKey sessionKey = sessionKeys.get(recipient);
        if (sessionKey == null) {
            throw new IllegalStateException("No session key available for recipient: " + recipient);
        }

        String encryptedContent = CryptoUtil.encrypt(content, sessionKey);

        ChatMessage encryptedMessage = new ChatMessage();
        encryptedMessage.setType(ChatMessage.MessageType.ENCRYPTED_CHAT);
        encryptedMessage.setContent(encryptedContent);
        encryptedMessage.setRecipient(recipient);

        return encryptedMessage;
    }

    public String decryptMessage(ChatMessage message) throws Exception {
        if (message.getType() != ChatMessage.MessageType.ENCRYPTED_CHAT) {
            throw new IllegalArgumentException("Invalid message type for decryption");
        }

        SecretKey sessionKey = sessionKeys.get(message.getSender());
        if (sessionKey == null) {
            throw new IllegalStateException("No session key available for sender: " + message.getSender());
        }

        return CryptoUtil.decrypt(message.getContent(), sessionKey);
    }

    public boolean hasSessionKey(String user) {
        return sessionKeys.containsKey(user);
    }
} 