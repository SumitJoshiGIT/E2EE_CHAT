#!/bin/bash

# Run script for E2EE Chat Frontend using direct class execution

# Try to locate JavaFX in standard locations if JAVAFX_HOME is not set
if [ -z "$JAVAFX_HOME" ]; then
  echo "JAVAFX_HOME environment variable is not set. Looking for JavaFX in standard locations..."
  
  # Check for JavaFX in target/lib directory (might be bundled)
  JAVAFX_CONTROLS=$(find target/lib -name "javafx-controls-*.jar" 2>/dev/null | head -1)
  
  if [ -n "$JAVAFX_CONTROLS" ]; then
    JAVAFX_HOME=$(dirname "$JAVAFX_CONTROLS")
    echo "Found JavaFX in $JAVAFX_HOME"
  else
    # Try common system locations
    for dir in /usr/share/openjfx /usr/lib/jvm/*/lib/javafx-sdk /opt/javafx-sdk* /usr/lib/jvm/java*/lib; do
      if [ -d "$dir" ] && [ -f "$dir/lib/javafx.controls.jar" -o -f "$dir/javafx.controls.jar" ]; then
        JAVAFX_HOME="$dir"
        echo "Found JavaFX in $JAVAFX_HOME"
        break
      fi
    done
  fi
  
  if [ -z "$JAVAFX_HOME" ]; then
    echo "Error: Could not locate JavaFX SDK."
    echo "Please install JavaFX and set JAVAFX_HOME to its installation directory."
    echo "For example: export JAVAFX_HOME=/path/to/javafx-sdk-19"
    exit 1
  fi
fi

# Check if JavaFX SDK exists
if [ ! -d "$JAVAFX_HOME" ]; then
  echo "Error: JavaFX SDK not found at $JAVAFX_HOME"
  echo "Please install JavaFX and set JAVAFX_HOME correctly."
  exit 1
fi

echo "Starting E2EE Chat Frontend..."
echo "Using JavaFX from: $JAVAFX_HOME"

# Set classpath with all dependencies
CLASSPATH="target/classes"

# Add all JARs in the lib directory to classpath
for jar in target/lib/*.jar; do
  if [ -f "$jar" ]; then
    CLASSPATH="$CLASSPATH:$jar"
  fi
done

# Run the application
java --module-path "$JAVAFX_HOME/lib" \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics \
     --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.util=ALL-UNNAMED \
     -cp "$CLASSPATH" \
     com.e2ee.chat.frontend.E2EEChatFrontendApplication
