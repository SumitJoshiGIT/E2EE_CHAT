#!/bin/bash

# Direct run script for E2EE Chat Frontend

# Set JavaFX path if not already set
if [ -z "$JAVAFX_HOME" ]; then
  # Try to find JavaFX in common locations
  if [ -d "/usr/share/openjfx" ]; then
    export JAVAFX_HOME="/usr/share/openjfx"
  elif [ -d "/opt/javafx-sdk-19" ]; then
    export JAVAFX_HOME="/opt/javafx-sdk-19"
  else
    echo "Please set JAVAFX_HOME to your JavaFX SDK path"
    exit 1
  fi
fi

# Check for presence of JavaFX jars
if [ ! -d "$JAVAFX_HOME/lib" ]; then
  echo "JavaFX libraries not found at $JAVAFX_HOME/lib"
  exit 1
fi

# Set classpath
CLASSPATH="target/classes"
for jar in target/lib/*.jar; do
  CLASSPATH="$CLASSPATH:$jar"
done

echo "Starting E2EE Chat Frontend..."
echo "Using JavaFX from: $JAVAFX_HOME"
echo "Using classpath: $CLASSPATH"

# Add all JavaFX JARs to classpath
for jar in "$JAVAFX_HOME"/lib/*.jar; do
  CLASSPATH="$CLASSPATH:$jar"
done

# Run application
java \
  --module-path "$JAVAFX_HOME/lib" \
  --add-modules javafx.controls,javafx.fxml \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  -classpath "$CLASSPATH" \
  com.e2ee.chat.frontend.Launcher
