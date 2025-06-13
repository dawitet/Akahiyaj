#!/bin/bash

echo "Building updated debug APK with Firebase fixes..."

# Change to the project directory
cd /Users/dawitsahle/AndroidStudioProjects/Akahidegn

# Clean the project
./gradlew clean

# Build debug APK
./gradlew assembleDebug

echo ""
echo "Checking if build was successful..."

# Check if APK exists
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "✅ Build successful! APK created at: $APK_PATH"
    echo ""
    echo "To install on the emulator, run:"
    echo "~/Library/Android/sdk/platform-tools/adb install -r $APK_PATH"
else
    echo "❌ Build failed. APK not found at: $APK_PATH"
    echo "Check the build output for errors."
fi
