#!/bin/bash

# Firebase CLI Group Cleanup Script
# This script uses Firebase CLI to manually clean up groups

echo "🔥 Firebase CLI Group Cleanup"
echo "=============================="

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI is not installed!"
    echo "Install it with: npm install -g firebase-tools"
    echo "Then login with: firebase login"
    exit 1
fi

echo "✅ Firebase CLI is available"
echo

# Get project ID (you may need to modify this)
PROJECT_ID="your-project-id"  # Replace with your actual project ID

echo "Select cleanup option:"
echo "1. View all groups (safe - just shows data)"
echo "2. Delete ALL groups (⚠️  PERMANENT - deletes everything)"
echo "3. Export groups to backup file first"
echo "4. Cancel"
echo

read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        echo "📋 Viewing all groups in Firebase..."
        firebase database:get /groups --project $PROJECT_ID --pretty
        ;;
    2)
        echo "⚠️  WARNING: This will DELETE ALL GROUPS permanently!"
        echo "Type 'DELETE_ALL_GROUPS' to confirm:"
        read -p "> " confirmation
        
        if [ "$confirmation" = "DELETE_ALL_GROUPS" ]; then
            echo "🔥 Deleting ALL groups from Firebase..."
            firebase database:remove /groups --project $PROJECT_ID
            echo "✅ All groups deleted!"
        else
            echo "❌ Deletion cancelled. Confirmation text didn't match."
        fi
        ;;
    3)
        echo "💾 Exporting groups to backup file..."
        firebase database:get /groups --project $PROJECT_ID > "groups_backup_$(date +%Y%m%d_%H%M%S).json"
        echo "✅ Groups exported to backup file"
        echo "Now you can safely delete them if needed"
        ;;
    4)
        echo "👋 Cancelled"
        ;;
    *)
        echo "❌ Invalid choice"
        ;;
esac

echo
echo "💡 Tips:"
echo "- Use option 1 to see current groups"
echo "- Use option 3 to backup before deletion"
echo "- After cleanup, test the app's automatic cleanup functionality"
echo "- Check app logs with tag 'GroupCleanupDebug'"
