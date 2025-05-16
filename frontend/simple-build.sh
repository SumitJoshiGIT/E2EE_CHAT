#!/bin/bash

# Build script for E2EE Chat Frontend

echo "Building E2EE Chat Frontend..."

# Make sure we're in the frontend directory
cd "$(dirname "$0")" || exit

# Clean and compile
echo "Compiling Java classes..."
mvn clean compile

# Success message
echo "Build completed. You can run the application with:"
echo "./run-app.sh"
