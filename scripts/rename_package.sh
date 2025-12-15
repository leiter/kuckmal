#!/bin/bash

# Rename directories from com/mediathekview/android to cut/the/crap
# For androidApp module

BASE_DIRS=(
  "androidApp/src/main/java"
  "androidApp/src/test/java"
  "androidApp/src/androidTest/java"
)

for BASE in "${BASE_DIRS[@]}"; do
  OLD_PATH="$BASE/com/mediathekview/android"
  NEW_PATH="$BASE/cut/the/crap"
  
  if [ -d "$OLD_PATH" ]; then
    echo "Moving $OLD_PATH -> $NEW_PATH"
    mkdir -p "$NEW_PATH"
    cp -r "$OLD_PATH"/* "$NEW_PATH/" 2>/dev/null || true
    rm -rf "$BASE/com/mediathekview"
  fi
done

# Remove empty com directory if exists
for BASE in "${BASE_DIRS[@]}"; do
  if [ -d "$BASE/com" ] && [ -z "$(ls -A $BASE/com 2>/dev/null)" ]; then
    rmdir "$BASE/com"
  fi
done

echo "Directory structure renamed"
