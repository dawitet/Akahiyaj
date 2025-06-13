#!/bin/bash

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

if [ -f "$APK_PATH" ]; then
    echo "Installing updated APK to emulator..."
    $ADB install -r "$APK_PATH"
else
    echo "❌ APK not found at: $APK_PATH"
    echo "Run ./build_updated_apk.sh first to build the APK."
fi
