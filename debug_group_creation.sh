#!/bin/bash

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

# Clear logcat
$ADB logcat -c

# Capture detailed info about the auth state and user ID
echo "=============== CURRENT USER STATE ==============="
$ADB logcat -d | grep -E "AUTH_TAG|authenticated|UID" | tail -n 5

# Preemptively looking for group creation attempts
echo -e "\n=============== WAITING FOR GROUP CREATION ATTEMPT ==============="
echo "Please try to create a group in the app now..."

$ADB logcat | grep -E "CREATE_GROUP_TAG|Failed to create|Permission denied|createdBy" --color
