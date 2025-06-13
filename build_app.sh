#!/bin/bash

# Enhanced build script for Akahidegn app
# Supports both debug and release builds

# Set default build type to debug if not specified
BUILD_TYPE=${1:-"debug"}

echo "🛠️  Building Akahidegn ($BUILD_TYPE build)..."
echo "================================================"

# Change to the project directory
PROJECT_DIR="/Users/dawitsahle/AndroidStudioProjects/Akahidegn"
cd "$PROJECT_DIR"

# Clean the project
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build the app based on build type
if [ "$BUILD_TYPE" = "release" ]; then
    echo "🔒 Building release APK (signed)..."
    ./gradlew assembleRelease
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
    echo "🔧 Building debug APK..."
    ./gradlew assembleDebug
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

echo ""
echo "Checking if build was successful..."

# Check if APK exists
if [ -f "$APK_PATH" ]; then
    echo "✅ Build successful! APK created at: $APK_PATH"
    
    # Copy APK to root for easy access
    if [ "$BUILD_TYPE" = "release" ]; then
        cp "$APK_PATH" "Akahidegn-1.0.0-updated-release.apk"
        echo "✅ Release APK copied to: Akahidegn-1.0.0-updated-release.apk"
    else
        cp "$APK_PATH" "Akahidegn-1.0.0-updated-debug.apk"
        echo "✅ Debug APK copied to: Akahidegn-1.0.0-updated-debug.apk"
    fi
    
    echo ""
    echo "To install on the emulator, run:"
    echo "./uninstall_and_install_apk.sh"
else
    echo "❌ Build failed. APK not found at: $APK_PATH"
    echo "Check the build output for errors."
    exit 1
fi
