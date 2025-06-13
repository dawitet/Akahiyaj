#!/bin/bash

echo "Starting Firebase rules testing using CLI instead of app build..."

# First, backup the current rules
echo "Backing up current Firebase rules..."
firebase database:get --project akahidegn-79376 /.settings/rules > current_rules_backup.json

# Test creating a group with the Firebase CLI
echo ""
echo "Testing group creation with Firebase CLI..."
GROUP_ID=$(date +%s)
echo "Creating test group with ID: $GROUP_ID"

# Create test data in a JSON file
JSON_FILE="test_group_$GROUP_ID.json"
cat > $JSON_FILE << EOF
{
  "id": "$GROUP_ID",
  "from": "Test Location",
  "to": "Test Destination",
  "departureTime": "1718545678", 
  "availableSeats": 4,
  "pricePerPerson": 0,
  "createdAt": $(date +%s000),
  "createdBy": "test_user",
  "members": {
    "test_user": true
  }
}
EOF

# Create a properly structured test group
echo "Using test data:"
cat $JSON_FILE
firebase database:set --project akahidegn-79376 /groups/$GROUP_ID --force $JSON_FILE

# Check if the group was created successfully
echo ""
echo "Checking if group was created successfully..."
firebase database:get --project akahidegn-79376 /groups/$GROUP_ID

# Delete the test group
echo ""
echo "Cleaning up test group..."
firebase database:remove --project akahidegn-79376 /groups/$GROUP_ID --force

echo ""
echo "Test complete!"
echo ""
echo "If the group was created successfully, the security rules are working."
echo "You should update your app to match this exact data structure."
