#!/bin/bash

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

echo "============================================="
echo "Akahidegn App Status Check"
echo "============================================="

# Check if app is running
APP_PID=$($ADB shell pidof com.dawitf.akahidegn)
if [ -z "$APP_PID" ]; then
    echo "❌ App is not currently running"
    echo "Starting app..."
    $ADB shell am start -n com.dawitf.akahidegn/.SplashActivity
    sleep 3
else
    echo "✅ App is running with PID: $APP_PID"
fi

echo ""
echo "============================================="
echo "Authentication Status"
echo "============================================="
$ADB logcat -d | grep -E "AUTH_TAG|authenticated|UID" | tail -n 5

echo ""
echo "============================================="
echo "Firebase Permission Issues"
echo "============================================="
$ADB logcat -d | grep -E "permission denied|Permission denied|SyncTree|RepoOperation" | tail -n 10

echo ""
echo "============================================="
echo "Location Updates"
echo "============================================="
$ADB logcat -d | grep "LOCATION_TAG" | tail -n 5

echo ""
echo "============================================="
echo "Database Operation Status"
echo "============================================="
$ADB logcat -d | grep -E "groups|setValue|database" | tail -n 10

echo ""
echo "============================================="
echo "RECOMMENDATIONS:"
echo "============================================="
echo "1. Update Firebase rules to add:"
echo "   - \".read\": \"true\" at /groups level"
echo "   - Rules for /user_fcm_tokens/$uid path"
echo ""
echo "2. In Firebase Console, add these rules:"
echo ""
echo "{
  \"rules\": {
    \"groups\": {
      \".indexOn\": [\"createdAt\", \"from\", \"to\"],
      \".read\": \"true\",
      // ...existing rules...
    },
    \"user_fcm_tokens\": {
      \"\$uid\": {
        \".read\": \"\$uid == auth.uid\",
        \".write\": \"\$uid == auth.uid\"
      }
    }
    // ...other existing rules...
  }
}"
