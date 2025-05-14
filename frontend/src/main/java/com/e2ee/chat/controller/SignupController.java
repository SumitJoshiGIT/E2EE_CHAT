package com.e2ee.chat.controller;

import com.e2ee.chat.dto.AuthRequest;
import com.e2ee.chat.dto.AuthResponse;
import com.e2ee.chat.service.WebSocketService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

@Controller
public class SignupController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signupButton;
    @FXML private Hyperlink loginLink;
    
    @Autowired private WebSocketService webSocketService;
    
    @FXML
    public void initialize() {
        signupButton.setOnAction(e -> handleSignUp());
        loginLink.setOnAction(e -> handleLogin());
    }
    
    private void handleSignUp() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill in all fields");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showAlert("Error", "Passwords do not match");
            return;
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            AuthRequest authRequest = new AuthRequest();
            authRequest.setUsername(username);
            authRequest.setEmail(email);
            authRequest.setPassword(password);
            
            HttpEntity<AuthRequest> request = new HttpEntity<>(authRequest, headers);
            
            AuthResponse response = restTemplate.postForObject(
                "http://localhost:8080/api/auth/register",
                request,
                AuthResponse.class
            );
            
            if (response != null && response.getToken() != null) {
                String token = response.getToken();
                webSocketService.setToken(token);
                webSocketService.connect();
                
                // Load chat view
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
                Parent root = loader.load();
                ChatController chatController = loader.getController();
                chatController.setWebSocketService(webSocketService);
                
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
                
                Stage stage = (Stage) signupButton.getScene().getWindow();
                stage.setScene(scene);
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to create account: " + e.getMessage());
        }
    }
    
    private void handleLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            Stage stage = (Stage) loginLink.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            showAlert("Error", "Failed to load login screen: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 