#!/bin/bash

# Build script to create APK and copy to desktop
set -e

echo "Building APK for Akahidegn..."

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Build debug APK
echo "Building debug APK..."
./gradlew assembleDebug

# Check if APK was created successfully
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "APK built successfully!"

    # Copy to desktop with a meaningful name
    DESKTOP_PATH="$HOME/Desktop/Akahidegn-debug.apk"
    cp "$APK_PATH" "$DESKTOP_PATH"

    echo "APK copied to desktop: $DESKTOP_PATH"
    echo "File size: $(du -h "$DESKTOP_PATH" | cut -f1)"

    # Show APK info
    echo "APK Details:"
    echo "- Location: $DESKTOP_PATH"
    echo "- Build Type: Debug"
    echo "- App Version: 1.1.0-compatible"

else
    echo "ERROR: APK was not created successfully"
    echo "Check the build output above for errors"
    exit 1
fi

echo "Build complete!"
