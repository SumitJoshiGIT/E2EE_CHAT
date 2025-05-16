package com.e2ee.chat.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;
import com.e2ee.chat.frontend.service.WebSocketService;
import com.e2ee.chat.frontend.service.AuthService;
import com.e2ee.chat.frontend.controller.*;

import java.io.IOException;

public class E2EEChatFrontendApplication extends Application {
    private static final String LOGIN_FXML = "/com/e2ee/chat/frontend/login.fxml";
    private static final String REGISTER_FXML = "/com/e2ee/chat/frontend/register.fxml";
    private static final String MAIN_FXML = "/com/e2ee/chat/frontend/main.fxml";
    private static Stage primaryStage;
    private static AuthService authService;
    private static WebSocketService webSocketService;
    private static String userId;
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        authService = new AuthService();
        webSocketService = new WebSocketService();

        showLoginScreen();

        stage.setTitle("E2EE Chat");
        stage.setOnCloseRequest(event -> {
            if (webSocketService.isConnected()) {
                webSocketService.disconnect();
            }
        });
    }

    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(E2EEChatFrontendApplication.class.getResource(LOGIN_FXML));
            Parent root = loader.load();
            Scene scene = new Scene(root, 400, 500);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showRegistrationScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(E2EEChatFrontendApplication.class.getResource(REGISTER_FXML));
            Parent root = loader.load();
            Scene scene = new Scene(root, 450, 550);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showMainScreen(String username) {
        try {
            // Set authentication token from auth service to websocket service
            webSocketService.setAuthToken(authService.getAuthToken());
            userId = authService.getUserIdFromToken(authService.getAuthToken());
            // Set chat list handler before connecting
            webSocketService.setChatListHandler(chatList -> {
                System.out.println("Chat list received: " + chatList);
                Platform.runLater(() -> {
                    MainController mainController = (MainController) primaryStage.getScene().getUserData();
                    if (mainController != null) {
                        mainController.updateChatList(chatList);
                    }
                });
            });
            
            // Connect to WebSocket after successful login
            if (!webSocketService.isConnected()) {
                webSocketService.connect(username,userId);
            }

            FXMLLoader loader = new FXMLLoader(E2EEChatFrontendApplication.class.getResource(MAIN_FXML));
            Parent root = loader.load();
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle(username);
            primaryStage.show();

            // Store the controller for later use
            MainController controller = loader.getController();
            primaryStage.getScene().setUserData(controller);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static AuthService getAuthService() {
        return authService;
    }

    public static WebSocketService getWebSocketService() {
        return webSocketService;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static String getDefaultUsername() {
        return "johndoe";
    }

    public static String getDefaultPassword() {
        return "password123";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
