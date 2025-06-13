#!/bin/bash

# Script to monitor Akahidegn app status and logs on the emulator
PACKAGE_NAME="com.dawitf.akahidegn"
ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"

# Check if ADB exists
if [ ! -f "$ADB" ]; then
    echo "❌ ADB not found at: $ADB"
    echo "Please update the script with the correct path to your ADB executable."
    exit 1
fi

# Check if emulator is running
DEVICE_COUNT=$($ADB devices | grep -c "emulator")
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "❌ No emulator detected. Please start an Android emulator first."
    exit 1
fi

# Function to check if the app is running
check_app_running() {
    APP_RUNNING=$($ADB shell "ps | grep $PACKAGE_NAME" | wc -l)
    if [ "$APP_RUNNING" -gt 0 ]; then
        echo "✅ Akahidegn app is currently running"
        return 0
    else
        echo "❌ Akahidegn app is not running"
        return 1
    fi
}

# Check app installation
APP_INSTALLED=$($ADB shell pm list packages | grep -c "$PACKAGE_NAME")
if [ "$APP_INSTALLED" -eq 0 ]; then
    echo "❌ Akahidegn app is not installed on the emulator"
    echo "Run ./uninstall_and_install_apk.sh to install the app"
    exit 1
else
    echo "✅ Akahidegn app is installed on the emulator"
fi

# Check if app is running
check_app_running

# Monitor options
echo ""
echo "Select an option:"
echo "1) Start the app"
echo "2) Monitor app logs (Firebase focus)"
echo "3) Monitor app logs (full)"
echo "4) Check Firebase connections"
echo "5) Exit"
read -p "Option: " option

case $option in
    1)
        echo "📱 Starting Akahidegn app..."
        $ADB shell am start -n "$PACKAGE_NAME/.MainActivity"
        sleep 2
        check_app_running
        ;;
    2)
        echo "📊 Monitoring Firebase-related logs (Ctrl+C to stop)..."
        $ADB logcat -v color | grep -E "($PACKAGE_NAME|Firebase|GoogleApiClient|FCM)"
        ;;
    3)
        echo "📊 Monitoring all app logs (Ctrl+C to stop)..."
        $ADB logcat -v color | grep "$PACKAGE_NAME"
        ;;
    4)
        echo "🔍 Checking Firebase connections..."
        $ADB logcat -d | grep -E "(Firebase|GoogleApiClient|Authentication|Database|FCM)" | tail -n 50
        ;;
    5)
        echo "Exiting."
        exit 0
        ;;
    *)
        echo "Invalid option. Exiting."
        exit 1
        ;;
esac
