#!/bin/bash

# --- Final, Corrected unmerge.sh ---
# Now includes a fix for Windows (CRLF) line endings.

BLUEPRINT_FILE=${1:-"blueprint.txt"}

if [ ! -f "$BLUEPRINT_FILE" ]; then
    echo "❌ Error: Blueprint file not found: $BLUEPRINT_FILE"
    exit 1
fi

echo "🚀 Reconstructing project from '$BLUEPRINT_FILE'..."

STATE=0
CURRENT_FILE=""
TEMP_CONTENT_FILE=$(mktemp)
trap 'rm -f "$TEMP_CONTENT_FILE"' EXIT

# Process the blueprint line by line
while IFS= read -r line; do

    # THE FIX: Remove the trailing carriage return if it exists.
    line=${line%$'\r'}

    if [[ "$line" == "<<--FILE_START-->>" ]]; then
        STATE=0
        continue
    fi

    if [[ "$line" == "<<--CONTENT_START-->>" ]]; then
        STATE=2
        >"$TEMP_CONTENT_FILE" # Erase temp file for new content
        continue
    fi

    if [[ "$line" == "<<--FILE_END-->>" ]]; then
        STATE=0

        if [ -n "$CURRENT_FILE" ]; then
            DIR_NAME=$(dirname "$CURRENT_FILE")
            if [ ! -d "$DIR_NAME" ]; then
                mkdir -p "$DIR_NAME"
            fi

            mv "$TEMP_CONTENT_FILE" "$CURRENT_FILE"
            echo "  ✓ Created/Updated: $CURRENT_FILE"
            CURRENT_FILE=""
        fi
        continue
    fi

    if [[ $STATE -eq 0 && "$line" == Path:* ]]; then
        CURRENT_FILE="${line#Path: }"
        STATE=1
        continue
    fi

    if [[ $STATE -eq 2 ]]; then
        echo "$line" >> "$TEMP_CONTENT_FILE"
    fi

done < "$BLUEPRINT_FILE"

echo "✅ Project reconstruction complete!"