#!/bin/bash

echo "Creating a fixed version of MainActivity.kt with properly balanced braces"

ORIGINAL_FILE="/Users/dawitsahle/AndroidStudioProjects/Akahidegn/app/src/main/java/com/dawitf/akahidegn/MainActivity.kt"
BACKUP_FILE="/Users/dawitsahle/AndroidStudioProjects/Akahidegn/app/src/main/java/com/dawitf/akahidegn/MainActivity.kt.before_fix"
FIXED_FILE="/Users/dawitsahle/AndroidStudioProjects/Akahidegn/app/src/main/java/com/dawitf/akahidegn/MainActivity.kt.fixed"

# First make a backup
cp "$ORIGINAL_FILE" "$BACKUP_FILE"
echo "Created backup at $BACKUP_FILE"

# Create a fixed version with an additional closing brace at the end
cp "$ORIGINAL_FILE" "$FIXED_FILE"
echo "}" >> "$FIXED_FILE"

# Check if the fix balanced the braces
OPEN_BRACES=$(grep -o "{" "$FIXED_FILE" | wc -l)
CLOSE_BRACES=$(grep -o "}" "$FIXED_FILE" | wc -l)

if [ $OPEN_BRACES -eq $CLOSE_BRACES ]; then
    echo "✅ Fix appears successful. Replacing original file..."
    mv "$FIXED_FILE" "$ORIGINAL_FILE"
    echo "Original file updated. You can now build the project."
else
    echo "⚠️ Fix did not balance braces correctly. Check file manually."
    echo "Fixed file available at: $FIXED_FILE"
fi
