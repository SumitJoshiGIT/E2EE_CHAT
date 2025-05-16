package com.e2ee.chat.frontend.controller;

import com.e2ee.chat.frontend.E2EEChatFrontendApplication;
import com.e2ee.chat.frontend.service.AuthService;
import com.e2ee.chat.frontend.crypto.CryptoUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.net.URL;
import java.security.KeyPair;
import java.util.Base64;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private Button registerButton;
    
    @FXML
    private Hyperlink loginLink;
    
    @FXML
    private Label statusLabel;
    
    private AuthService authService;
    private KeyPair keyPair;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = E2EEChatFrontendApplication.getAuthService();
        
        // Generate RSA key pair for E2EE
        keyPair = CryptoUtils.generateKeyPair();
        
        // Add listeners to validate form fields
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> checkFields());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> checkFields());
        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> checkFields());
        emailField.textProperty().addListener((observable, oldValue, newValue) -> checkFields());
        
        // Initialize button state
        checkFields();
        
        // Hide status label initially
        statusLabel.setVisible(false);
    }
    
    @FXML
    public void handleRegisterButton(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        // Disable the button during registration attempt
        registerButton.setDisable(true);
        registerButton.setText("Registering...");
        
        // Convert public key to Base64 for storage
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        
        // Run registration in a separate thread to avoid freezing the UI
        new Thread(() -> {
            boolean success = authService.register(username, password, email);
            
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    showSuccess("Registration successful! Logging in...");
                    
                    // Automatically log in after registration
                    authService.login(username, password);
                    
                    // Short delay before switching to main screen
                    new javafx.animation.PauseTransition(Duration.seconds(1.5)).onFinishedProperty()
                        .set(event2 -> E2EEChatFrontendApplication.showMainScreen(username));
                } else {
                    showError("Registration failed. Username may be taken.");
                    
                    // Reset the button
                    registerButton.setDisable(false);
                    registerButton.setText("Register");
                }
            });
        }).start();
    }
    
    @FXML
    public void handleLoginLink(ActionEvent event) {
        // Return to login screen
        E2EEChatFrontendApplication.showLoginScreen();
    }
    
    private void checkFields() {
        boolean fieldsValid = !usernameField.getText().trim().isEmpty() && 
                            passwordField.getText().length() >= 6 &&
                            passwordField.getText().equals(confirmPasswordField.getText()) &&
                            isValidEmail(emailField.getText().trim());
        
        registerButton.setDisable(!fieldsValid);
        
        // Show validation errors
        if (!fieldsValid) {
            if (usernameField.getText().trim().isEmpty()) {
                statusLabel.setText("Username cannot be empty");
                statusLabel.setVisible(true);
            } else if (passwordField.getText().length() < 6) {
                statusLabel.setText("Password must be at least 6 characters");
                statusLabel.setVisible(true);
            } else if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                statusLabel.setText("Passwords do not match");
                statusLabel.setVisible(true);
            } else if (!isValidEmail(emailField.getText().trim())) {
                statusLabel.setText("Please enter a valid email address");
                statusLabel.setVisible(true);
            }
        } else {
            statusLabel.setVisible(false);
        }
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        statusLabel.setVisible(true);
    }
    
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #2ecc71;");
        statusLabel.setVisible(true);
    }
    
    private boolean isValidEmail(String email) {
        // Simple email validation
        return email != null && email.matches("[^@]+@[^@]+\\.[^@]+");
    }
}
