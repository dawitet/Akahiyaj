#!/bin/bash

# Set Android SDK paths
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/emulator
ADB=~/Library/Android/sdk/platform-tools/adb

echo "🔍 Debugging App Crash - Starting logcat monitoring..."

# Clear logcat
$ADB logcat -c

# Install the app 
echo "📱 Installing debug APK..."
./gradlew installDebug --no-daemon

echo "🚀 Starting app and monitoring logcat..."
echo "===================================="

# Start the app and monitor logcat for crashes
$ADB shell am start -n com.dawitf.akahidegn/.SplashActivity

# Monitor logcat for crash-related logs
$ADB logcat -v time | grep -E "(FATAL|AndroidRuntime|CrashHandler|APP_INIT|FIREBASE|Hilt|DexLoader|ClassLoader|OutOfMemoryError|StackOverflowError|com.dawitf.akahidegn)"
