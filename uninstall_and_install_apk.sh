#!/bin/bash

echo "🔄 Uninstalling existing Akahidegn app and installing fresh APK"
echo "=============================================================="

# Define paths
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"
PACKAGE_NAME="com.dawitf.akahidegn"

# Check if ADB exists
if [ ! -f "$ADB" ]; then
    echo "❌ ADB not found at: $ADB"
    echo "Please update the script with the correct path to your ADB executable."
    exit 1
fi

# Check if emulator is running
DEVICE_COUNT=$($ADB devices | grep -c "emulator")
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "❌ No emulator detected. Please start an Android emulator first."
    exit 1
fi

echo "📱 Connected devices:"
$ADB devices

# Uninstall existing app
echo "🗑️  Uninstalling existing app from emulator..."
$ADB uninstall $PACKAGE_NAME
echo "✅ Uninstallation complete (if app was previously installed)"

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at: $APK_PATH"
    echo "Run ./build_updated_apk.sh first to build the APK."
    exit 1
fi

# Install fresh APK
echo "📲 Installing fresh APK to emulator..."
$ADB install "$APK_PATH"

# Check installation success
if [ $? -eq 0 ]; then
    echo "✅ Installation successful! App has been installed."
    echo "📱 Launching app..."
    $ADB shell am start -n "$PACKAGE_NAME/.MainActivity"
else
    echo "❌ Installation failed. Check the error message above."
fi

echo "🔍 Starting logcat to monitor app startup..."
echo "Press Ctrl+C to stop the logcat."
$ADB logcat -v color | grep -E "($PACKAGE_NAME|Firebase|GoogleApiClient|FCM)"
