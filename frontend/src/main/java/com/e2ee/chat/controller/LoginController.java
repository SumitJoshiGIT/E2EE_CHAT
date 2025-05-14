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
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Controller
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signupLink;
    
    @Autowired private WebSocketService webSocketService;
    
    @FXML
    public void initialize() {
        // Set default credentials
        usernameField.setText("janesmith");
        passwordField.setText("password123");
        
        loginButton.setOnAction(e -> handleLogin());
        signupLink.setOnAction(e -> handleSignUp());
    }
    
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter username and password");
            return;
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            AuthRequest authRequest = new AuthRequest();
            authRequest.setUsername(username);
            authRequest.setPassword(password);
            
            HttpEntity<AuthRequest> request = new HttpEntity<>(authRequest, headers);
            
            ResponseEntity<AuthResponse> responseEntity = restTemplate.exchange(
                "http://localhost:8080/api/auth/login",
                HttpMethod.POST,
                request,
                AuthResponse.class
            );
            
            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                AuthResponse response = responseEntity.getBody();
                String token = response.getToken();
                webSocketService.setToken(token);
                webSocketService.setCurrentUser(username);
                webSocketService.connect();
                
                // Load chat view
                FXMLLoader loader = new FXMLLoader(ChatController.class.getResource("/fxml/chat.fxml"));
                Parent root = loader.load();
                ChatController chatController = loader.getController();
                chatController.setWebSocketService(webSocketService);
                
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
                
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(scene);
            }
        } catch (HttpClientErrorException.Forbidden e) {
            showAlert("Error", e.getMessage());
            showAlert("Error", "Invalid username or password");
        } catch (HttpClientErrorException.Unauthorized e) {
            showAlert("Error", "Unauthorized access");
        } catch (Exception e) {
            showAlert("Error", "Failed to login: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        }
    }
    
    private void handleSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signup.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            Stage stage = (Stage) signupLink.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            showAlert("Error", "Failed to load signup screen: " + e.getMessage());
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