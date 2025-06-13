#!/bin/bash

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

echo "================================================="
echo "  Firebase Group Creation Test - After Rule Update"
echo "================================================="

# Clear logcat
$ADB logcat -c

echo "Monitoring for group creation success/failure..."
echo "Please try to create a group in the app."
echo "Press Ctrl+C to stop monitoring."
echo ""

# Start monitoring logcat for success/failure
$ADB logcat | grep -E "CREATE_GROUP_TAG|FIREBASE_DEBUG|RepoOperation|Permission denied|Group created|setValue"
