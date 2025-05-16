package com.e2ee.chat.frontend;

import javafx.application.Application;

/**
 * Launcher class to handle JavaFX module system requirements
 * This provides a non-JavaFX entry point that calls the JavaFX application
 */
public class Launcher {
    public static void main(String[] args) {
        // Launch the JavaFX application
        Application.launch(E2EEChatFrontendApplication.class, args);
    }
}
