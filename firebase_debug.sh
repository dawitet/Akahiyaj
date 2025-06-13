#!/bin/bash

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

# Clear logcat
$ADB logcat -c

# Launch the app
$ADB shell am start -n com.dawitf.akahidegn/.SplashActivity

# Wait longer for app to initialize and authenticate
echo "Waiting 10 seconds for app to initialize..."
sleep 10

# Capture logs with focus on Firebase errors
echo "========== CAPTURING FIREBASE LOGS =========="
$ADB logcat -d | grep -E "Firebase|AUTH_TAG|CREATE_GROUP|permission|denied|DATABASE|SyncTree|RepoOperation" | tail -n 30

echo ""
echo "========== INSPECTING ANONYMOUS AUTH =========="
$ADB logcat -d | grep -E "AUTH_TAG|User already authenticated|UID" | tail -n 10

echo ""
echo "========== DATABASE PERMISSION ISSUES =========="
$ADB logcat -d | grep -E "permission|denied|DENIED|Permission" | tail -n 10

echo ""
echo "========== FCM & TOKEN ISSUES =========="
$ADB logcat -d | grep -E "FCM_TAG|FIREBASE_INSTALL|token" | tail -n 10
