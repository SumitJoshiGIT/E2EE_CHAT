package com.e2ee.chat.util;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for E2EE encryption operations.
 * Note: In a real E2EE system, most encryption logic would happen on the client side.
 * This class is provided for completeness and server-side validation.
 */
@Slf4j
public class EncryptionUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /**
     * Generate a new RSA key pair for asymmetric encryption
     * @return KeyPair containing public and private keys
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate key pair", e);
            throw new RuntimeException("Encryption setup failed", e);
        }
    }
    
    /**
     * Convert public key to Base64 string for transmission
     * @param publicKey The public key to convert
     * @return Base64 encoded public key string
     */
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * Convert Base64 string back to public key
     * @param keyString Base64 encoded public key string
     * @return PublicKey object
     */
    public static PublicKey stringToPublicKey(String keyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Failed to convert string to public key", e);
            throw new RuntimeException("Public key conversion failed", e);
        }
    }
    
    /**
     * Encrypt a message with recipient's public key
     * @param message Message to encrypt
     * @param publicKey Recipient's public key
     * @return Base64 encoded encrypted message
     */
    public static String encryptWithPublicKey(String message, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Failed to encrypt message", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt a message with user's private key
     * @param encryptedMessage Base64 encoded encrypted message
     * @param privateKey User's private key
     * @return Decrypted message string
     */
    public static String decryptWithPrivateKey(String encryptedMessage, PrivateKey privateKey) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Failed to decrypt message", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
