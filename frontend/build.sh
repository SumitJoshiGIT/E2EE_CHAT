#!/bin/bash

# Build script for E2EE Chat Frontend

echo "Building E2EE Chat Frontend..."

# Make sure we're in the frontend directory
cd "$(dirname "$0")" || exit

# Clean previous builds
echo "Cleaning previous builds..."
mvn clean

# Build with Maven
echo "Compiling with Maven..."
mvn compile

# Package the application
echo "Packaging application..."
mvn package

# Create a convenience script to run the application
cat > run.sh << EOF
#!/bin/bash

# Run E2EE Chat Frontend
java --module-path "\$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml -jar target/e2ee-chat-frontend-1.0.0.jar
EOF

chmod +x run.sh

echo "Build complete! Run the application with ./run.sh"
echo "Make sure to set JAVAFX_HOME environment variable to your JavaFX SDK path"
