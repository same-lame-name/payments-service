#!/bin/bash

# The name of the output blueprint file
BLUEPRINT_FILE="blueprint.txt"
if [ "$SEARCH_PATH" != "." ]; then
    BLUEPRINT_FILE="blueprint.txt"
fi

# Ensure we start with a clean slate
echo "Initializing blueprint..."
>"$BLUEPRINT_FILE"

echo "🔍 Finding relevant project files and building blueprint..."

# Find all relevant files. Add or remove patterns as needed.
# This structure is robust and handles all filenames correctly.
find . -type f \( -name "*.java" -o -name "pom.xml" -o -name "*.yml" -o -name "*.properties" \) -print0 | while IFS= read -r -d '' file; do

    # Normalize path for consistency (convert \ to /)
    normalized_file_path="${file//\\//}"
    echo "  Adding: $normalized_file_path"

    # Append the file block to the blueprint file
    {
        echo "<<--FILE_START-->>"
        echo "Path: $normalized_file_path"
        echo "<<--CONTENT_START-->>"
        # Use 'cat' and explicitly handle text mode for Windows/Git Bash compatibility
        cat "$file"
        # Ensure there is a trailing newline for the final delimiter
        echo
        echo "<<--FILE_END-->>"
    } >> "$BLUEPRINT_FILE"
done

echo "✅ Blueprint created successfully: $BLUEPRINT_FILE"