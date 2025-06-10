#!/bin/bash

# Manual Firebase Groups Cleanup Script
# This script helps you manually delete all groups from Firebase Realtime Database

echo "🔥 Manual Firebase Groups Cleanup Script"
echo "========================================="
echo
echo "This script will help you clean up Firebase groups manually."
echo
echo "Option 1: Firebase Console (Recommended)"
echo "----------------------------------------"
echo "1. Go to: https://console.firebase.google.com/"
echo "2. Select your 'Akahidegn' project"
echo "3. Click 'Realtime Database' in the sidebar"
echo "4. Find the 'groups' node"
echo "5. Click the three dots (⋮) next to 'groups'"
echo "6. Select 'Delete' to remove all groups"
echo
echo "Option 2: Firebase CLI"
echo "----------------------"
echo "If you have Firebase CLI installed, you can run:"
echo "firebase database:remove /groups --project YOUR_PROJECT_ID"
echo
echo "Option 3: Manual Android Debug"
echo "------------------------------"
echo "Use the debug helper in the app to trigger cleanup:"
echo "1. Open the app"
echo "2. Go to Debug section"
echo "3. Click 'Trigger Immediate Cleanup'"
echo "4. Check logs for results"
echo
echo "Option 4: Age-based Cleanup Script"
echo "-----------------------------------"

# Check if Firebase CLI is installed
if command -v firebase &> /dev/null; then
    echo "Firebase CLI is installed. You can run manual commands."
    echo
    echo "To delete all groups (BE CAREFUL - THIS IS PERMANENT):"
    echo "firebase database:remove /groups --project YOUR_PROJECT_ID"
    echo
    echo "To view current groups first:"
    echo "firebase database:get /groups --project YOUR_PROJECT_ID"
else
    echo "Firebase CLI is not installed."
    echo "Install it with: npm install -g firebase-tools"
fi

echo
echo "After cleanup, test the app's group cleanup functionality:"
echo "1. Create a few test groups"
echo "2. Wait 30+ minutes or use debug helper"
echo "3. Verify groups are automatically cleaned up"
echo
echo "🚨 WARNING: Manual deletion is permanent! Make sure you want to delete all groups."
