package com.e2ee.chat.frontend.controller;

import com.e2ee.chat.frontend.model.UserProfile;
import com.e2ee.chat.frontend.service.AuthService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class ProfileEditorController {
    
    @FXML private TextField displayNameField;
    @FXML private TextField emailField;
    @FXML private TextArea bioField;
    @FXML private TextField avatarUrlField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Label statusLabel;
    
    private AuthService authService;
    private UserProfile currentProfile;
    private Stage dialogStage;
    private Runnable onSaveCallback;
    
    private static final List<String> STATUS_OPTIONS = Arrays.asList(
        "Online", "Away", "Busy", "Offline"
    );
    
    @FXML
    private void initialize() {
        // Initialize status options
        statusComboBox.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
    }
    
    public void setup(AuthService authService, UserProfile profile, Stage dialogStage, Runnable onSaveCallback) {
        this.authService = authService;
        this.dialogStage = dialogStage;
        this.onSaveCallback = onSaveCallback;
        
        // Safety check - if profile is null, create a default one
        if (profile == null) {
            System.out.println("Warning: Null profile passed to ProfileEditorController.setup()");
            String username = authService.getCurrentUsername();
            this.currentProfile = new UserProfile();
            this.currentProfile.setUsername(username != null ? username : "user");
            this.currentProfile.setDisplayName(username != null ? username : "");
            this.currentProfile.setStatus("Online");
            
            // Initialize fields with default values
            displayNameField.setText(this.currentProfile.getDisplayName());
            emailField.setText("");
            bioField.setText("");
            avatarUrlField.setText("");
            statusComboBox.setValue("Online");
        } else {
            this.currentProfile = profile;
            
            // Populate fields with current profile data
            displayNameField.setText(profile.getDisplayName());
            emailField.setText(profile.getEmail() != null ? profile.getEmail() : "");
            bioField.setText(profile.getBio() != null ? profile.getBio() : "");
            avatarUrlField.setText(profile.getAvatarUrl() != null ? profile.getAvatarUrl() : "");
            
            // Set status in combo box
            if (profile.getStatus() != null && !profile.getStatus().isEmpty()) {
                statusComboBox.setValue(profile.getStatus());
            } else {
                statusComboBox.setValue("Online");
            }
        }
    }
    
    @FXML
    private void handleSaveButton(ActionEvent event) {
        try {
            statusLabel.setText("Updating profile...");
            
            // Create updated profile from field values
            UserProfile updatedProfile = new UserProfile();
            updatedProfile.setUsername(currentProfile.getUsername());
            updatedProfile.setDisplayName(displayNameField.getText());
            updatedProfile.setEmail(emailField.getText());
            updatedProfile.setBio(bioField.getText());
            updatedProfile.setAvatarUrl(avatarUrlField.getText());
            updatedProfile.setStatus(statusComboBox.getValue());
            updatedProfile.setPublicKey(currentProfile.getPublicKey());
            
            // Update profile via service
            authService.updateProfile(updatedProfile, profile -> {
                Platform.runLater(() -> {
                    // Show success message
                    statusLabel.setText("Profile updated successfully!");
                    statusLabel.getStyleClass().remove("error-label");
                    
                    // Wait a moment, then close dialog
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(() -> {
                                if (onSaveCallback != null) {
                                    onSaveCallback.run();
                                }
                                dialogStage.close();
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                });
            }, error -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Error updating profile: " + error);
                    statusLabel.getStyleClass().add("error-label");
                });
            });
            
        } catch (Exception e) {
            statusLabel.setText("Error updating profile: " + e.getMessage());
            statusLabel.getStyleClass().add("error-label");
        }
    }
    
    @FXML
    private void handleCancelButton(ActionEvent event) {
        dialogStage.close();
    }
}
