#!/bin/bash

# Comprehensive Firebase Group Operations Test Script
# This script tests various Firebase group operations for Akahidegn

echo "🔥 Firebase Group Operations Tester"
echo "===================================="

PACKAGE_NAME="com.dawitf.akahidegn"
ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"
TIMESTAMP=$(date +%s)
TEST_GROUP_NAME="test_group_$TIMESTAMP"
TEST_GROUP_FILE="$TEST_GROUP_NAME.json"

# Function to check emulator
check_emulator() {
    DEVICE_COUNT=$($ADB devices | grep -c "emulator")
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "❌ No emulator detected. Please start an Android emulator first."
        return 1
    else
        echo "✅ Emulator detected"
        return 0
    fi
}

# Function to create test group data
create_test_group_json() {
    echo "📝 Creating test group data: $TEST_GROUP_FILE"
    cat > "$TEST_GROUP_FILE" << EOF
{
  "groupId": "$TEST_GROUP_NAME",
  "name": "Test Group $TIMESTAMP",
  "description": "Automatically created test group",
  "created": "$TIMESTAMP",
  "members": {
    "test_user_id": {
      "userId": "test_user_id",
      "name": "Test User",
      "role": "admin",
      "joinedAt": "$TIMESTAMP"
    }
  },
  "location": {
    "latitude": 9.005401,
    "longitude": 38.763611
  },
  "chatEnabled": true,
  "public": true
}
EOF
    echo "✅ Test group data created"
}

# Function to check if app is installed
check_app_installed() {
    APP_INSTALLED=$($ADB shell pm list packages | grep -c "$PACKAGE_NAME")
    if [ "$APP_INSTALLED" -eq 0 ]; then
        echo "❌ Akahidegn app is not installed on the emulator"
        return 1
    else
        echo "✅ Akahidegn app is installed on the emulator"
        return 0
    fi
}

# Function to test group creation through app
test_app_group_creation() {
    echo "📱 Testing group creation through the app..."
    # This will just launch the app - actual group creation would need UI automation
    $ADB shell am start -n "$PACKAGE_NAME/.MainActivity"
    echo "🔍 Starting log monitor for group creation (Ctrl+C to stop after testing)..."
    $ADB logcat | grep -E "(Firebase|GroupCreation|Database)"
}

# Function to test Firebase CLI group operations
test_firebase_cli_group() {
    echo "🔥 Testing Firebase CLI group operations..."
    
    if command -v firebase &> /dev/null; then
        echo "✅ Firebase CLI detected"
        
        # Test database operations if user has Firebase CLI configured
        echo "Would you like to test Firebase CLI database operations? (y/n)"
        read -p "Option: " firebase_option
        
        if [ "$firebase_option" = "y" ]; then
            create_test_group_json
            
            echo "📤 Uploading test group to Firebase..."
            firebase database:set "/groups/$TEST_GROUP_NAME" "$TEST_GROUP_FILE"
            
            echo "📥 Reading test group from Firebase..."
            firebase database:get "/groups/$TEST_GROUP_NAME"
            
            echo "Would you like to delete the test group? (y/n)"
            read -p "Option: " delete_option
            
            if [ "$delete_option" = "y" ]; then
                echo "🗑️  Deleting test group from Firebase..."
                firebase database:remove "/groups/$TEST_GROUP_NAME"
            fi
        fi
    else
        echo "❌ Firebase CLI not installed. Install with: npm install -g firebase-tools"
        echo "Then login with: firebase login"
    fi
}

# Main execution
echo "Select a test option:"
echo "1) Verify emulator and app installation"
echo "2) Create test group JSON file"
echo "3) Test app group creation (launches app)"
echo "4) Test Firebase CLI group operations"
echo "5) Run all tests"
echo "6) Exit"

read -p "Option: " main_option

case $main_option in
    1)
        check_emulator && check_app_installed
        ;;
    2)
        create_test_group_json
        ;;
    3)
        check_emulator && check_app_installed && test_app_group_creation
        ;;
    4)
        test_firebase_cli_group
        ;;
    5)
        check_emulator && check_app_installed && create_test_group_json && test_app_group_creation && test_firebase_cli_group
        ;;
    6)
        echo "Exiting."
        exit 0
        ;;
    *)
        echo "Invalid option. Exiting."
        exit 1
        ;;
esac
