#!/bin/bash

echo "Restoring regular Firebase rules from updated_firebase_rules.json..."
firebase database:set --project akahidegn-79376 /.settings/rules updated_firebase_rules.json

echo ""
echo "Regular rules restored."
