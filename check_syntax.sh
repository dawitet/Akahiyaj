#!/bin/bash

echo "Checking syntax in MainActivity.kt"

TARGET_FILE="/Users/dawitsahle/AndroidStudioProjects/Akahidegn/app/src/main/java/com/dawitf/akahidegn/MainActivity.kt"

echo "Checking for unbalanced braces..."
OPEN_BRACES=$(grep -o "{" "$TARGET_FILE" | wc -l)
CLOSE_BRACES=$(grep -o "}" "$TARGET_FILE" | wc -l)

echo "Open braces: $OPEN_BRACES"
echo "Close braces: $CLOSE_BRACES"

if [ $OPEN_BRACES -ne $CLOSE_BRACES ]; then
    echo "⚠️ ERROR: Unbalanced braces. Missing $(($OPEN_BRACES - $CLOSE_BRACES)) closing braces."
else
    echo "✅ Braces are balanced."
fi

# Also check the specific lines with errors
echo ""
echo "Checking line 834..."
sed -n '830,838p' "$TARGET_FILE"
echo ""
echo "Checking line 1441..."
sed -n '1438,1444p' "$TARGET_FILE"
