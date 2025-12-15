#!/bin/bash
set -e

echo "=== 1. Renaming androidApp: cut.the.crap -> cut.the.crap.android ==="
for BASE in androidApp/src/main/java androidApp/src/test/java androidApp/src/androidTest/java; do
  if [ -d "$BASE/cut/the/crap" ]; then
    echo "  Moving $BASE/cut/the/crap -> $BASE/cut/the/crap/android"
    mkdir -p "$BASE/cut/the/crap/android"
    # Move all contents except 'android' dir itself
    for item in "$BASE/cut/the/crap"/*; do
      if [ "$(basename "$item")" != "android" ]; then
        mv "$item" "$BASE/cut/the/crap/android/"
      fi
    done
  fi
done
# Update package declarations
find androidApp -type f -name "*.kt" -exec sed -i 's/package cut\.the\.crap$/package cut.the.crap.android/g' {} +
find androidApp -type f -name "*.kt" -exec sed -i 's/package cut\.the\.crap\./package cut.the.crap.android./g' {} +
find androidApp -type f -name "*.kt" -exec sed -i 's/import cut\.the\.crap\./import cut.the.crap.android./g' {} +
sed -i "s/namespace 'cut.the.crap'/namespace 'cut.the.crap.android'/g" androidApp/build.gradle
sed -i 's/applicationId "cut.the.crap"/applicationId "cut.the.crap.android"/g' androidApp/build.gradle

echo "=== 2. Renaming shared: com.mediathekview.shared -> cut.the.crap.shared ==="
for BASE in shared/src/commonMain/kotlin shared/src/androidMain/kotlin shared/src/iosMain/kotlin shared/src/desktopMain/kotlin shared/src/jsMain/kotlin; do
  OLD_PATH="$BASE/com/mediathekview/shared"
  NEW_PATH="$BASE/cut/the/crap/shared"
  if [ -d "$OLD_PATH" ]; then
    echo "  Moving $OLD_PATH -> $NEW_PATH"
    mkdir -p "$NEW_PATH"
    cp -r "$OLD_PATH"/* "$NEW_PATH/" 2>/dev/null || true
    rm -rf "$BASE/com"
  fi
done
find shared -type f -name "*.kt" -exec sed -i 's/com\.mediathekview\.shared/cut.the.crap.shared/g' {} +

echo "=== 3. Renaming webApp: com.mediathekview.web -> cut.the.crap.web ==="
OLD_PATH="webApp/src/jsMain/kotlin/com/mediathekview/web"
NEW_PATH="webApp/src/jsMain/kotlin/cut/the/crap/web"
if [ -d "$OLD_PATH" ]; then
  echo "  Moving $OLD_PATH -> $NEW_PATH"
  mkdir -p "$NEW_PATH"
  cp -r "$OLD_PATH"/* "$NEW_PATH/" 2>/dev/null || true
  rm -rf "webApp/src/jsMain/kotlin/com"
fi
find webApp -type f -name "*.kt" -exec sed -i 's/com\.mediathekview\.web/cut.the.crap.web/g' {} +

echo "=== 4. Renaming desktopApp: com.mediathekview.desktop -> cut.the.crap.desktop ==="
OLD_PATH="desktopApp/src/main/kotlin/com/mediathekview/desktop"
NEW_PATH="desktopApp/src/main/kotlin/cut/the/crap/desktop"
if [ -d "$OLD_PATH" ]; then
  echo "  Moving $OLD_PATH -> $NEW_PATH"
  mkdir -p "$NEW_PATH"
  cp -r "$OLD_PATH"/* "$NEW_PATH/" 2>/dev/null || true
  rm -rf "desktopApp/src/main/kotlin/com"
fi
find desktopApp -type f -name "*.kt" -exec sed -i 's/com\.mediathekview\.desktop/cut.the.crap.desktop/g' {} +

echo "=== 5. Updating cross-module imports ==="
# Update imports of shared in androidApp
find androidApp -type f -name "*.kt" -exec sed -i 's/com\.mediathekview\.shared/cut.the.crap.shared/g' {} +
# Update imports of shared in desktopApp
find desktopApp -type f -name "*.kt" -exec sed -i 's/com\.mediathekview\.shared/cut.the.crap.shared/g' {} +
# Update imports of shared in webApp
find webApp -type f -name "*.kt" -exec sed -i 's/com\.mediathekview\.shared/cut.the.crap.shared/g' {} +

echo "=== 6. Updating build.gradle files ==="
# Update shared namespace if exists
find . -name "build.gradle.kts" -exec sed -i 's/com\.mediathekview\.shared/cut.the.crap.shared/g' {} + 2>/dev/null || true
find . -name "build.gradle" -exec sed -i 's/com\.mediathekview\.shared/cut.the.crap.shared/g' {} + 2>/dev/null || true
find . -name "build.gradle.kts" -exec sed -i 's/com\.mediathekview\.desktop/cut.the.crap.desktop/g' {} + 2>/dev/null || true
find . -name "build.gradle" -exec sed -i 's/com\.mediathekview\.desktop/cut.the.crap.desktop/g' {} + 2>/dev/null || true

echo "=== Done! ==="
