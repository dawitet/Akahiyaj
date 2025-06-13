#!/bin/bash

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

echo "Clearing logs..."
$ADB logcat -c

echo "Monitoring Firebase Database operations and security rule evaluation..."
echo "Try to create a group in the app now."
echo "Press Ctrl+C to stop monitoring."

$ADB logcat | grep -E "FIREBASE_DEBUG|CREATE_GROUP_TAG|Firebase Database|RepoOperation|Permission denied|newData|validateData"
