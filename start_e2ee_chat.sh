#!/bin/bash

# E2EE Chat Application Startup Script
# This script sets up the environment and starts both the backend and frontend

# Define colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print status messages
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if a port is in use
is_port_in_use() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        return 0
    else
        return 1
    fi
}

# Step 1: Check MongoDB status and ensure correct replica set configuration
print_status "Checking MongoDB replica set status..."

# Check if MongoDB is running
if ! pgrep -x "mongod" > /dev/null; then
    print_error "MongoDB is not running. Please start MongoDB first."
    exit 1
fi

# Check MongoDB replica set status
mongo_status=$(mongo --quiet --eval "rs.status()" | grep -q "PRIMARY" && echo "OK" || echo "NOT_CONFIGURED")

if [ "$mongo_status" != "OK" ]; then
    print_warning "MongoDB replica set is not properly configured. Attempting to configure..."
    
    # Stop any running MongoDB instances
    print_status "Stopping MongoDB services..."
    sudo systemctl stop mongod

    # Start MongoDB with replica set configuration
    print_status "Starting MongoDB with replica set 'res0'..."
    mkdir -p ~/data/db/primary
    mkdir -p ~/data/db/secondary
    
    # Start primary node on port 27018
    mongod --replSet res0 --port 27018 --dbpath ~/data/db/primary --fork --logpath ~/data/db/primary/mongodb.log
    
    # Start secondary node on port 27017
    mongod --replSet res0 --port 27017 --dbpath ~/data/db/secondary --fork --logpath ~/data/db/secondary/mongodb.log
    
    # Initialize replica set
    print_status "Initializing replica set..."
    sleep 3
    mongo --port 27018 --eval 'rs.initiate({_id: "res0", members: [{_id: 0, host: "localhost:27018", priority: 10}, {_id: 1, host: "localhost:27017", priority: 1}]})'
    
    # Wait for replica set to initialize
    print_status "Waiting for replica set to initialize..."
    sleep 5
    
    # Verify replica set status
    mongo_check=$(mongo --port 27018 --quiet --eval "rs.status()" | grep -q "PRIMARY" && echo "OK" || echo "FAILED")
    
    if [ "$mongo_check" != "OK" ]; then
        print_error "Failed to configure MongoDB replica set. Please check MongoDB logs."
        exit 1
    fi
    
    print_status "MongoDB replica set 'res0' configured successfully!"
else
    print_status "MongoDB replica set is properly configured."
fi

# Step 2: Set up JavaFX environment
print_status "Setting up JavaFX environment..."

# Check if JavaFX is installed
if [ ! -f "/usr/share/java/javafx-base-11.jar" ]; then
    print_warning "JavaFX not found. Installing..."
    sudo apt-get update && sudo apt-get install -y openjfx
fi

# Set up JavaFX home if not already set
if [ -z "$JAVAFX_HOME" ]; then
    print_status "Setting up JAVAFX_HOME..."
    
    # Create JavaFX home directory structure if it doesn't exist
    if [ ! -d "$HOME/.javafx" ]; then
        mkdir -p $HOME/.javafx/lib
        mkdir -p $HOME/.javafx/lib/native
        
        # Create symbolic links to JavaFX jars
        ln -sf /usr/share/java/javafx-base-11.jar $HOME/.javafx/lib/javafx-base.jar
        ln -sf /usr/share/java/javafx-controls-11.jar $HOME/.javafx/lib/javafx-controls.jar
        ln -sf /usr/share/java/javafx-fxml-11.jar $HOME/.javafx/lib/javafx-fxml.jar
        ln -sf /usr/share/java/javafx-graphics-11.jar $HOME/.javafx/lib/javafx-graphics.jar
        ln -sf /usr/share/java/javafx-media-11.jar $HOME/.javafx/lib/javafx-media.jar
        ln -sf /usr/share/java/javafx-swing-11.jar $HOME/.javafx/lib/javafx-swing.jar
        ln -sf /usr/share/java/javafx-web-11.jar $HOME/.javafx/lib/javafx-web.jar
        
        # Create symbolic links to native libraries
        ln -sf /usr/lib/x86_64-linux-gnu/jni/*.so $HOME/.javafx/lib/native/
    fi
    
    # Set JAVAFX_HOME for current session
    export JAVAFX_HOME=$HOME/.javafx
    
    # Add JAVAFX_HOME to .bashrc for future sessions if not already there
    if ! grep -q "JAVAFX_HOME" $HOME/.bashrc; then
        echo '# JavaFX configuration' >> $HOME/.bashrc
        echo "export JAVAFX_HOME=$HOME/.javafx" >> $HOME/.bashrc
    fi
    
    print_status "JAVAFX_HOME set to $JAVAFX_HOME"
else
    print_status "JAVAFX_HOME already set to $JAVAFX_HOME"
fi

# Step 3: Start the backend server
print_status "Starting E2EE Chat backend server..."

# Check if backend server is already running
if is_port_in_use 8080; then
    print_warning "Port 8080 is already in use. Backend may already be running."
else
    # Change to the backend directory
    cd /home/sumit/E2EE_CHAT/backend
    
    # Start the backend
    java -jar target/e2ee-chat-backend-1.0.0.jar > backend.log 2>&1 &
    
    # Store the backend PID
    BACKEND_PID=$!
    print_status "Backend started with PID: $BACKEND_PID"
    
    # Wait for backend to initialize
    print_status "Waiting for backend to initialize..."
    sleep 10
fi

# Step 4: Start the frontend application
print_status "Starting E2EE Chat frontend application..."

# Change to the frontend directory
cd /home/sumit/E2EE_CHAT/frontend

# Start the frontend with JavaFX configuration
java --module-path $JAVAFX_HOME/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.web,javafx.swing \
     -Djavafx.verbose=true \
     -jar target/e2ee-chat-frontend-1.0.0.jar &

FRONTEND_PID=$!
print_status "Frontend started with PID: $FRONTEND_PID"

print_status "E2EE Chat application is now running!"
print_status "Backend PID: $BACKEND_PID (if started by this script)"
print_status "Frontend PID: $FRONTEND_PID"
print_status "To stop the application, use: kill $BACKEND_PID $FRONTEND_PID"