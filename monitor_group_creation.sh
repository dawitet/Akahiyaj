#!/bin/bash

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

echo "Monitoring for group creation attempts..."
echo "Try to create a group in the app now."
echo "Press Ctrl+C to stop monitoring."

$ADB logcat -c
$ADB logcat | grep -E "CREATE_GROUP|Group created|setValue|setValue at /groups|Failed to create group"
