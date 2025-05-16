package com.e2ee.chat.frontend.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtils {
    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_SIZE = 256;
    
    /**
     * Generate a new RSA key pair for asymmetric encryption
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }
    
    /**
     * Generate a new AES key for symmetric encryption
     */
    public static SecretKey generateAESKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }
    
    /**
     * Encrypt a message using AES-GCM
     * 
     * @param message The plaintext message to encrypt
     * @param key The AES key
     * @return Tuple containing the encrypted message and the IV
     */
    public static EncryptionResult encryptMessage(String message, SecretKey key) {
        try {
            byte[] messageBytes = message.getBytes();
            
            // Generate random IV (initialization vector)
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[12]; // 12 bytes (96 bits) for GCM
            secureRandom.nextBytes(iv);
            
            // Initialize AES-GCM encryption
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            // Encrypt the message
            byte[] encryptedBytes = cipher.doFinal(messageBytes);
            
            // Encode to Base64 for transmission
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            
            return new EncryptionResult(encryptedBase64, ivBase64);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt message", e);
        }
    }
    
    /**
     * Decrypt a message using AES-GCM
     * 
     * @param encryptedMessage The encrypted message (Base64 encoded)
     * @param iv The initialization vector (Base64 encoded)
     * @param key The AES key
     * @return The decrypted plaintext message
     */
    public static String decryptMessage(String encryptedMessage, String iv, SecretKey key) {
        try {
            // Decode from Base64
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            
            // Initialize AES-GCM decryption
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            // Decrypt the message
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message", e);
        }
    }
    
    /**
     * Encrypt an AES key with RSA public key
     * 
     * @param aesKey The AES key to encrypt
     * @param publicKey The RSA public key
     * @return Base64 encoded encrypted AES key
     */
    public static String encryptAESKey(SecretKey aesKey, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            
            byte[] encryptedKeyBytes = cipher.doFinal(aesKey.getEncoded());
            return Base64.getEncoder().encodeToString(encryptedKeyBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt AES key", e);
        }
    }
    
    /**
     * Decrypt an AES key with RSA private key
     * 
     * @param encryptedKey Base64 encoded encrypted AES key
     * @param privateKey The RSA private key
     * @return The decrypted AES key
     */
    public static SecretKey decryptAESKey(String encryptedKey, PrivateKey privateKey) {
        try {
            byte[] encryptedKeyBytes = Base64.getDecoder().decode(encryptedKey);
            
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            
            byte[] decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes);
            return new SecretKeySpec(decryptedKeyBytes, "AES");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt AES key", e);
        }
    }
    
    /**
     * Convert a Base64 encoded public key string to a PublicKey object
     * 
     * @param publicKeyBase64 Base64 encoded public key
     * @return PublicKey object
     */
    public static PublicKey decodePublicKey(String publicKeyBase64) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode public key", e);
        }
    }
    
    /**
     * Inner class to hold encryption results
     */
    public static class EncryptionResult {
        private final String encryptedData;
        private final String iv;
        
        public EncryptionResult(String encryptedData, String iv) {
            this.encryptedData = encryptedData;
            this.iv = iv;
        }
        
        public String getEncryptedData() {
            return encryptedData;
        }
        
        public String getIv() {
            return iv;
        }
    }
}
