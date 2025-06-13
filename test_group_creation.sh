#!/bin/bash

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

echo "=== Firebase Group Creation Debugging ==="
echo "This script will trigger the app to create a test group with additional debugging"
echo ""
echo "Make sure you have the app open and are logged in"
echo ""
echo "Starting detailed logging..."

# Clear old logs
$ADB logcat -c

# Start logcat capturing in background
$ADB logcat -v time | grep -E "CREATE_GROUP_TAG|FIREBASE_DEBUG|setValue|Permission|RepoOperation|validateData" > group_creation_debug.log &
LOGCAT_PID=$!

# Wait for logging to start
sleep 1

echo "Sending a broadcast to trigger group creation with enhanced debugging..."
$ADB shell am broadcast -a com.dawitf.akahidegn.DEBUG_CREATE_TEST_GROUP --es "destination" "TestLocation" --ez "verbose" "true"

echo "Waiting for operation to complete..."
sleep 5

# Kill the logcat process
kill $LOGCAT_PID

echo ""
echo "Debug log saved to group_creation_debug.log"
echo "Showing last 20 lines of log:"
tail -n 20 group_creation_debug.log
