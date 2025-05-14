package com.e2ee.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ChatApplication extends Application {
    private ConfigurableApplicationContext springContext;
    private static String[] args;
    
    public static void main(String[] args) {
        ChatApplication.args = args;
        launch(args);
    }
    
    @Override
    public void init() {
        springContext = SpringApplication.run(ChatApplication.class, args);
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Parent root = fxmlLoader.load();
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        
        String port = springContext.getEnvironment().getProperty("local.server.port", "unknown");
        primaryStage.setTitle("E2EE Chat - Port " + port);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    @Override
    public void stop() {
        springContext.close();
    }
} 