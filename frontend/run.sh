#!/bin/bash

# Run E2EE Chat Frontend
java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml -jar target/e2ee-chat-frontend-1.0.0.jar
