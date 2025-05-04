package com.e2ee.chat;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;

public class ChatListItem extends HBox {
    private final Circle avatar;
    private final Circle statusIndicator;
    private final Label nameLabel;
    private final Label lastMessageLabel;
    private final Label timeLabel;

    public ChatListItem(String name, String lastMessage, String time) {
        setPadding(new Insets(10));
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: white; -fx-cursor: hand;");
        
        // Create avatar with status indicator
        StackPane avatarContainer = new StackPane();
        avatar = new Circle(25);
        avatar.setFill(Color.LIGHTGRAY);
        
        statusIndicator = new Circle(8);
        statusIndicator.setFill(Color.GRAY);
        statusIndicator.setStroke(Color.WHITE);
        statusIndicator.setStrokeWidth(2);
        StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);
        
        avatarContainer.getChildren().addAll(avatar, statusIndicator);
        
        // Create name and last message labels
        nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        lastMessageLabel = new Label(lastMessage);
        lastMessageLabel.setFont(Font.font("System", 12));
        lastMessageLabel.setTextFill(Color.GRAY);
        
        timeLabel = new Label(time);
        timeLabel.setFont(Font.font("System", 10));
        timeLabel.setTextFill(Color.GRAY);
        
        // Create right side content
        VBox rightContent = new VBox(5);
        rightContent.getChildren().addAll(nameLabel, lastMessageLabel);
        
        // Add all components
        getChildren().addAll(avatarContainer, rightContent, timeLabel);
        
        // Add hover effect
        setOnMouseEntered(e -> setStyle("-fx-background-color: #f5f5f5; -fx-cursor: hand;"));
        setOnMouseExited(e -> setStyle("-fx-background-color: white; -fx-cursor: hand;"));
    }

    public void setAvatarColor(Color color) {
        avatar.setFill(color);
    }

    public void setLastMessage(String message) {
        lastMessageLabel.setText(message);
    }

    public void setTime(String time) {
        timeLabel.setText(time);
    }

    public String getName() {
        return nameLabel.getText();
    }

    public void setOnline(boolean online) {
        statusIndicator.setFill(online ? Color.GREEN : Color.GRAY);
    }
} 