#!/bin/bash

echo "This script will help you manually clean up test groups from Firebase."
echo "It will list all groups and allow you to delete them individually."
echo ""

# List groups
echo "Fetching current groups from Firebase..."
GROUPS=$(firebase database:get --project akahidegn-79376 /groups)

if [[ "$GROUPS" == "null" ]]; then
    echo "No groups found in the database. Nothing to clean up."
    exit 0
fi

# Parse and show groups
echo "Groups found in the database:"
echo "$GROUPS" | grep -o '"id": "[^"]*"' || true

echo ""
echo "To delete a specific group, run:"
echo "firebase database:remove --project akahidegn-79376 /groups/GROUP_ID"
echo ""
echo "For example:"
echo "firebase database:remove --project akahidegn-79376 /groups/-OSahs2T2Ov2BjZTYiNB"
echo ""
echo "To delete ALL groups (dangerous!):"
echo "firebase database:remove --project akahidegn-79376 /groups"
