#!/bin/bash

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

echo "=== Firebase Group Data Testing ==="
echo "This script will monitor the data being sent during group creation"

# Clear old logs
$ADB logcat -c

# Start the monitoring
echo "Monitoring Firebase operations..."
$ADB logcat | grep -E "toMap|GROUP_DEBUG|CREATE_GROUP_TAG|RepoOperation|validateData" &
LOGCAT_PID=$!

echo ""
echo "Please try to create a group in the app now."
echo "Press Ctrl+C when finished to stop monitoring."
echo ""

# Wait for user to press Ctrl+C
trap "kill $LOGCAT_PID; exit" INT
while true; do sleep 1; done
