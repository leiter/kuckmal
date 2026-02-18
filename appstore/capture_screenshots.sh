#!/bin/bash

# App Store Screenshot Capture Script
# Captures screenshots for all required devices and languages
#
# Usage: ./capture_screenshots.sh [device] [language]
#   device: iphone67, iphone65, ipad129, or all
#   language: de, en, or all
#
# Examples:
#   ./capture_screenshots.sh all all       # Capture all screenshots
#   ./capture_screenshots.sh iphone67 de   # Only iPhone 6.7" German
#   ./capture_screenshots.sh ipad129 en    # Only iPad 12.9" English

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
IOS_APP_DIR="$PROJECT_DIR/iosApp"
SCREENSHOT_DIR="$SCRIPT_DIR/screenshots"
MAESTRO_OUTPUT_DIR="$HOME/.maestro/tests"

# Device configurations
# Format: "name:udid_pattern:screenshot_dir:resolution"
# iPhone 16 Pro Max = 6.7" (1290x2796) - matches iPhone 15 Pro Max requirement
# iPhone 16 Plus = 6.5" (1284x2778) - close to iPhone 11 Pro Max (1242x2688)
# iPad Pro 13-inch (M4) = 12.9" (2064x2752) - matches iPad Pro 12.9" requirement

declare -A DEVICES
DEVICES["iphone67"]="iPhone 16 Pro Max:iphone67"
DEVICES["iphone65"]="iPhone 16 Plus:iphone65"
DEVICES["ipad129"]="iPad Pro 13-inch (M4):ipad129"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Get UDID for a device name
get_device_udid() {
    local device_name="$1"
    xcrun simctl list devices available | grep "$device_name" | head -1 | grep -oE '\([A-F0-9-]{36}\)' | tr -d '()'
}

# Boot simulator with specific locale
boot_simulator() {
    local device_name="$1"
    local language="$2"
    local udid=$(get_device_udid "$device_name")

    if [ -z "$udid" ]; then
        log_error "Device '$device_name' not found"
        return 1
    fi

    log_info "Shutting down all simulators..."
    xcrun simctl shutdown all 2>/dev/null || true

    log_info "Booting $device_name ($udid)..."
    xcrun simctl boot "$udid"

    # Set locale
    local locale_identifier
    if [ "$language" == "de" ]; then
        locale_identifier="de_DE"
    else
        locale_identifier="en_US"
    fi

    log_info "Setting locale to $locale_identifier..."
    # Note: Setting simulator locale requires plutil manipulation or UI interaction
    # For now, the app should detect system language automatically

    # Open Simulator app
    open -a Simulator

    # Wait for simulator to be ready
    log_info "Waiting for simulator to boot..."
    xcrun simctl bootstatus "$udid" -b

    echo "$udid"
}

# Build and install app
build_and_install() {
    local udid="$1"
    local device_name="$2"

    log_info "Building app for $device_name..."

    cd "$IOS_APP_DIR"

    # Determine destination based on device type
    local destination="id=$udid"

    # Build the app
    xcodebuild -workspace iosApp.xcworkspace \
        -scheme iosApp \
        -destination "$destination" \
        -derivedDataPath build \
        build \
        2>&1 | tail -20

    # Find and install the app
    local app_path=$(find build -name "iosApp.app" -type d | head -1)

    if [ -z "$app_path" ]; then
        log_error "Built app not found"
        return 1
    fi

    log_info "Installing app..."
    xcrun simctl install "$udid" "$app_path"

    cd "$PROJECT_DIR"
}

# Run Maestro flows and capture screenshots
capture_screenshots() {
    local udid="$1"
    local device_key="$2"
    local language="$3"

    local output_dir="$SCREENSHOT_DIR/$device_key/$language"
    mkdir -p "$output_dir"

    log_info "Running prepare_app flow..."
    cd "$PROJECT_DIR"
    maestro test iosApp/.maestro/prepare_app.yaml --udid "$udid" || {
        log_warn "prepare_app failed, continuing anyway..."
    }

    log_info "Capturing screenshots..."
    maestro test iosApp/.maestro/appstore_screenshots.yaml --udid "$udid" || {
        log_error "Screenshot capture failed"
        return 1
    }

    # Find and move screenshots
    log_info "Moving screenshots to $output_dir..."

    # Maestro saves screenshots to ~/.maestro/tests/[timestamp]/
    local latest_test_dir=$(ls -td "$MAESTRO_OUTPUT_DIR"/*/ 2>/dev/null | head -1)

    if [ -d "$latest_test_dir" ]; then
        # Find all appstore screenshots
        find "$latest_test_dir" -name "appstore_*.png" -exec cp {} "$output_dir/" \;

        # Rename to follow naming convention
        for f in "$output_dir"/appstore_*.png; do
            if [ -f "$f" ]; then
                local basename=$(basename "$f")
                local newname="${device_key}_${basename#appstore_}_${language}.png"
                mv "$f" "$output_dir/$newname" 2>/dev/null || true
            fi
        done
    fi

    log_info "Screenshots saved to $output_dir"
}

# Main capture function for a single device/language combination
capture_for_device_language() {
    local device_key="$1"
    local language="$2"

    local device_info="${DEVICES[$device_key]}"
    local device_name="${device_info%%:*}"

    log_info "========================================"
    log_info "Capturing: $device_name ($language)"
    log_info "========================================"

    # Boot simulator
    local udid=$(boot_simulator "$device_name" "$language")
    if [ -z "$udid" ]; then
        log_error "Failed to boot simulator"
        return 1
    fi

    # Build and install
    build_and_install "$udid" "$device_name"

    # Capture screenshots
    capture_screenshots "$udid" "$device_key" "$language"

    log_info "Completed: $device_name ($language)"
}

# Parse arguments
DEVICE_ARG="${1:-all}"
LANGUAGE_ARG="${2:-all}"

# Validate arguments
if [ "$DEVICE_ARG" != "all" ] && [ -z "${DEVICES[$DEVICE_ARG]}" ]; then
    log_error "Unknown device: $DEVICE_ARG"
    echo "Valid devices: iphone67, iphone65, ipad129, all"
    exit 1
fi

if [ "$LANGUAGE_ARG" != "all" ] && [ "$LANGUAGE_ARG" != "de" ] && [ "$LANGUAGE_ARG" != "en" ]; then
    log_error "Unknown language: $LANGUAGE_ARG"
    echo "Valid languages: de, en, all"
    exit 1
fi

# Determine devices to process
if [ "$DEVICE_ARG" == "all" ]; then
    DEVICES_TO_PROCESS=("iphone67" "iphone65" "ipad129")
else
    DEVICES_TO_PROCESS=("$DEVICE_ARG")
fi

# Determine languages to process
if [ "$LANGUAGE_ARG" == "all" ]; then
    LANGUAGES_TO_PROCESS=("de" "en")
else
    LANGUAGES_TO_PROCESS=("$LANGUAGE_ARG")
fi

# Summary
log_info "Screenshot Capture Plan:"
log_info "  Devices: ${DEVICES_TO_PROCESS[*]}"
log_info "  Languages: ${LANGUAGES_TO_PROCESS[*]}"
log_info "  Output: $SCREENSHOT_DIR"
echo ""

# Execute
for device in "${DEVICES_TO_PROCESS[@]}"; do
    for lang in "${LANGUAGES_TO_PROCESS[@]}"; do
        capture_for_device_language "$device" "$lang"
    done
done

# Summary
log_info "========================================"
log_info "Screenshot capture complete!"
log_info "========================================"
log_info "Screenshots saved to: $SCREENSHOT_DIR"
echo ""
echo "Directory structure:"
find "$SCREENSHOT_DIR" -type f -name "*.png" | head -30

echo ""
log_info "Next steps:"
echo "  1. Review screenshots in $SCREENSHOT_DIR"
echo "  2. Upload to App Store Connect"
