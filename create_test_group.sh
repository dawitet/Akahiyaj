#!/bin/bash

# This script contains the exact data structure that works with Firebase security rules
# You can adapt the fields to create any test group

ADB="/Users/dawitsahle/Library/Android/sdk/platform-tools/adb"
GROUP_ID=$(date +%s)
DESTINATION="Test Destination"
USER_UID="test_user"

echo "Creating a test group with ID: $GROUP_ID"
echo "Using user ID: $USER_UID"

# Create test data
JSON_FILE="test_group_$GROUP_ID.json"
cat > $JSON_FILE << EOF
{
  "id": "$GROUP_ID",
  "from": "Current Location",
  "to": "$DESTINATION",
  "departureTime": "$(date +%s000)", 
  "availableSeats": 4,
  "pricePerPerson": 0,
  "createdAt": $(date +%s000),
  "createdBy": "$USER_UID",
  "members": {
    "$USER_UID": true
  },
  "pickupLat": 9.005401,
  "pickupLng": 38.763611,
  "maxMembers": 4,
  "memberCount": 1,
  "imageUrl": "https://picsum.photos/seed/$GROUP_ID/400/300"
}
EOF

echo "Writing test group to Firebase..."
firebase database:set --project akahidegn-79376 /groups/$GROUP_ID --force $JSON_FILE

echo ""
echo "Group created! It should appear in the app's group list."
echo ""
echo "To clean up this test group later, run:"
echo "firebase database:remove --project akahidegn-79376 /groups/$GROUP_ID --force"
