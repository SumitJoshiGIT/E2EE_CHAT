#!/bin/bash

# Script to set up JavaFX environment for E2EE_CHAT application

echo "Setting up JavaFX environment..."

# Create JAVAFX_HOME structure
mkdir -p ~/.javafx/lib
mkdir -p ~/.javafx/bin

# Create symbolic links to JavaFX libraries
echo "Creating symlinks to JavaFX libraries..."
ln -sf /usr/share/java/javafx-base-11.jar ~/.javafx/lib/javafx-base.jar
ln -sf /usr/share/java/javafx-controls-11.jar ~/.javafx/lib/javafx-controls.jar
ln -sf /usr/share/java/javafx-fxml-11.jar ~/.javafx/lib/javafx-fxml.jar
ln -sf /usr/share/java/javafx-graphics-11.jar ~/.javafx/lib/javafx-graphics.jar
ln -sf /usr/share/java/javafx-media-11.jar ~/.javafx/lib/javafx-media.jar
ln -sf /usr/share/java/javafx-swing-11.jar ~/.javafx/lib/javafx-swing.jar
ln -sf /usr/share/java/javafx-web-11.jar ~/.javafx/lib/javafx-web.jar

# Create symbolic links to native libraries
echo "Creating symlinks to JavaFX native libraries..."
mkdir -p ~/.javafx/lib/native
ln -sf /usr/lib/x86_64-linux-gnu/jni/*.so ~/.javafx/lib/native/

# Add JAVAFX_HOME to user's environment
echo "Adding JAVAFX_HOME to environment..."
if ! grep -q "JAVAFX_HOME" ~/.bashrc; then
    echo '# JavaFX configuration' >> ~/.bashrc
    echo 'export JAVAFX_HOME=~/.javafx' >> ~/.bashrc
    echo 'export PATH=$JAVAFX_HOME/bin:$PATH' >> ~/.bashrc
fi

# Set JAVAFX_HOME for current session
export JAVAFX_HOME=~/.javafx
export PATH=$JAVAFX_HOME/bin:$PATH

echo "JavaFX environment has been set up!"
echo "JAVAFX_HOME=$JAVAFX_HOME"

# Source the updated .bashrc
echo "To load the new environment variables in your current shell, run:"
echo "source ~/.bashrc"
