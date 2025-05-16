#!/bin/bash

# E2EE Chat Application Startup Script
echo "Starting E2EE Chat Application..."

# Check if MongoDB is running
echo "Checking MongoDB status..."
if pgrep -x mongod >/dev/null; then
    echo "MongoDB is already running."
else
    echo "MongoDB is not running. Starting MongoDB..."
    if command -v mongod &>/dev/null; then
        # Start MongoDB if it's installed
        mongod --dbpath /var/lib/mongodb --fork --logpath /var/log/mongodb/mongod.log || {
            echo "Failed to start MongoDB! Please make sure it's properly installed."
            echo "You can install MongoDB with: sudo apt-get install -y mongodb"
            exit 1
        }
        echo "MongoDB started successfully."
    else
        echo "MongoDB is not installed. Please install MongoDB first."
        echo "You can install it with: sudo apt-get install -y mongodb"
        exit 1
    fi
fi

# Navigate to the backend directory
cd "$(dirname "$0")/backend" || {
    echo "Failed to navigate to backend directory!"
    exit 1
}

# Build the application
echo "Building application..."
./mvnw clean package -DskipTests || {
    echo "Failed to build the application!"
    exit 1
}

# Run the backend application
echo "Starting E2EE Chat backend..."
java -jar target/e2ee-chat-backend-1.0.0.jar &
BACKEND_PID=$!
echo "Backend started with PID: $BACKEND_PID"

# Navigate to the frontend directory
cd ../frontend || {
    echo "Failed to navigate to frontend directory!"
    exit 1
}

# Run the frontend application
echo "Starting E2EE Chat frontend..."
./run.sh

# If frontend closes, kill the backend
echo "Frontend closed, terminating backend (PID: $BACKEND_PID)..."
kill $BACKEND_PID
