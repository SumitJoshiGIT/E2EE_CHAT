#!/bin/bash

# Check if JAVAFX_HOME is set
if [ -z "$JAVAFX_HOME" ]; then
    echo "JAVAFX_HOME is not set. Setting up JavaFX environment..."
    
    # Set default JavaFX home location
    export JAVAFX_HOME=$HOME/.javafx
    
    # Create JavaFX home directory structure if it doesn't exist
    if [ ! -d "$JAVAFX_HOME" ]; then
        mkdir -p $JAVAFX_HOME/lib
        mkdir -p $JAVAFX_HOME/lib/native
        
        # Create symbolic links to JavaFX jars
        ln -sf /usr/share/java/javafx-base-11.jar $JAVAFX_HOME/lib/javafx-base.jar
        ln -sf /usr/share/java/javafx-controls-11.jar $JAVAFX_HOME/lib/javafx-controls.jar
        ln -sf /usr/share/java/javafx-fxml-11.jar $JAVAFX_HOME/lib/javafx-fxml.jar
        ln -sf /usr/share/java/javafx-graphics-11.jar $JAVAFX_HOME/lib/javafx-graphics.jar
        ln -sf /usr/share/java/javafx-media-11.jar $JAVAFX_HOME/lib/javafx-media.jar
        ln -sf /usr/share/java/javafx-swing-11.jar $JAVAFX_HOME/lib/javafx-swing.jar
        ln -sf /usr/share/java/javafx-web-11.jar $JAVAFX_HOME/lib/javafx-web.jar
        
        # Create symbolic links to native libraries
        ln -sf /usr/lib/x86_64-linux-gnu/jni/*.so $JAVAFX_HOME/lib/native/
    fi
    
    echo "JAVAFX_HOME set to $JAVAFX_HOME"
fi

# Set path to JavaFX modules
JAVAFX_MODULES="javafx.controls,javafx.fxml,javafx.graphics,javafx.web,javafx.swing"

# Run E2EE Chat Frontend with JavaFX configurations
java --module-path "$JAVAFX_HOME/lib" \
     --add-modules $JAVAFX_MODULES \
     -Djava.library.path="$JAVAFX_HOME/lib/native" \
     -Djavafx.verbose=true \
     -jar target/e2ee-chat-frontend-1.0.0.jar
