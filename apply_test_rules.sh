#!/bin/bash

echo "WARNING: This script will apply wide-open rules for TESTING ONLY."
echo "These rules are NOT suitable for production."
echo "They will allow any authenticated user to read and write ANY data."
echo ""
read -p "Are you sure you want to continue? (y/N) " confirm
if [[ $confirm != [yY] ]]; then
    echo "Operation cancelled."
    exit 1
fi

echo "Applying testing-only Firebase rules..."
firebase database:set --project akahidegn-79376 /.settings/rules testing_rules.json

echo ""
echo "Testing rules applied. DO NOT USE THESE IN PRODUCTION."
echo "To restore proper rules, run:"
echo "firebase database:set --project akahidegn-79376 /.settings/rules updated_firebase_rules.json"
