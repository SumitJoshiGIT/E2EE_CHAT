package com.e2ee.chat.frontend.controller;

import com.e2ee.chat.frontend.E2EEChatFrontendApplication;
import com.e2ee.chat.frontend.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Hyperlink;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Hyperlink registerLink;
    
    private AuthService authService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = E2EEChatFrontendApplication.getAuthService();

        // Set default username and password
        usernameField.setText(E2EEChatFrontendApplication.getDefaultUsername());
        passwordField.setText(E2EEChatFrontendApplication.getDefaultPassword());

        // Add listener to enable/disable login button based on field values
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> checkFields());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> checkFields());

        // Initialize button state
        checkFields();
    }
    
    @FXML
    public void handleLoginButton(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // Disable the button during login attempt
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");
        
        // Run login in a separate thread to avoid freezing the UI
        new Thread(() -> {
            boolean success = authService.login(username, password);
            
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    E2EEChatFrontendApplication.showMainScreen(username);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Login Failed");
                    alert.setHeaderText("Invalid username or password");
                    alert.setContentText("Please check your credentials and try again.");
                    alert.showAndWait();
                    
                    // Reset the button
                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                }
            });
        }).start();
    }
    
    @FXML
    public void handleRegisterLink(ActionEvent event) {
        // Show registration screen
        E2EEChatFrontendApplication.showRegistrationScreen();
    }
    
    private void checkFields() {
        boolean fieldsValid = !usernameField.getText().trim().isEmpty() && 
                            !passwordField.getText().isEmpty();
        
        loginButton.setDisable(!fieldsValid);
    }
}
