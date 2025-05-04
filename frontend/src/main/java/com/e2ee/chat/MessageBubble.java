package com.e2ee.chat;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class MessageBubble extends HBox {
    private final Label messageLabel;
    private final Label timeLabel;
    private final boolean isSent;

    public MessageBubble(String message, String time, boolean isSent) {
        this.isSent = isSent;
        setPadding(new Insets(5));
        setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        // Create message container
        VBox messageContainer = new VBox(5);
        messageContainer.setPadding(new Insets(10));
        messageContainer.setStyle(String.format(
            "-fx-background-color: %s; -fx-background-radius: 10;",
            isSent ? "#DCF8C6" : "white"
        ));
        
        // Create message label
        messageLabel = new Label(message);
        messageLabel.setFont(Font.font("System", 14));
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        
        // Create time label
        timeLabel = new Label(time);
        timeLabel.setFont(Font.font("System", 10));
        timeLabel.setTextFill(Color.GRAY);
        
        // Add components to container
        messageContainer.getChildren().addAll(messageLabel, timeLabel);
        
        // Add container to bubble
        getChildren().add(messageContainer);
        
        // Set max width
        setMaxWidth(Double.MAX_VALUE);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setTime(String time) {
        timeLabel.setText(time);
    }
} 