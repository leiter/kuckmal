#!/bin/bash

# Manual Screenshot Capture Script for App Store
# This script helps capture screenshots using xcrun simctl io screenshot
#
# Usage: ./capture_screenshots_manual.sh <device> <language>
#   device: iphone67, iphone65, ipad129
#   language: de, en
#
# Example: ./capture_screenshots_manual.sh iphone67 de

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SCREENSHOT_DIR="$SCRIPT_DIR/screenshots"

# Device configurations
declare -A DEVICES
DEVICES["iphone67"]="iPhone 16 Pro Max"
DEVICES["iphone65"]="iPhone 16 Plus"
DEVICES["ipad129"]="iPad Pro 13-inch (M4)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

log_prompt() {
    echo -e "${YELLOW}>>>${NC} $1"
}

# Get UDID for a device
get_device_udid() {
    local device_name="$1"
    xcrun simctl list devices available | grep "$device_name" | head -1 | grep -oE '\([A-F0-9-]{36}\)' | tr -d '()'
}

# Capture screenshot
capture_screenshot() {
    local udid="$1"
    local filename="$2"

    xcrun simctl io "$udid" screenshot "$filename"
    log_info "Screenshot saved: $filename"
}

# Main function
main() {
    local device_key="${1:-iphone67}"
    local language="${2:-de}"

    local device_name="${DEVICES[$device_key]}"
    if [ -z "$device_name" ]; then
        echo "Unknown device: $device_key"
        echo "Valid devices: iphone67, iphone65, ipad129"
        exit 1
    fi

    local output_dir="$SCREENSHOT_DIR/$device_key/$language"
    mkdir -p "$output_dir"

    log_info "========================================"
    log_info "Screenshot Capture: $device_name ($language)"
    log_info "========================================"
    echo ""

    # Get device UDID
    local udid=$(get_device_udid "$device_name")
    if [ -z "$udid" ]; then
        echo "Device '$device_name' not found. Run: xcrun simctl list devices available"
        exit 1
    fi
    log_info "Device UDID: $udid"

    # Check if booted
    local booted=$(xcrun simctl list devices booted | grep "$udid")
    if [ -z "$booted" ]; then
        log_info "Booting simulator..."
        xcrun simctl boot "$udid"
        open -a Simulator
        xcrun simctl bootstatus "$udid" -b
    else
        log_info "Simulator already booted"
    fi

    # Clean status bar
    log_info "Setting clean status bar (9:41 AM, full battery)..."
    xcrun simctl status_bar "$udid" override \
        --time "9:41" \
        --batteryState charged \
        --batteryLevel 100 \
        --wifiBars 3 \
        --cellularMode active \
        --cellularBars 4

    echo ""
    log_info "Output directory: $output_dir"
    echo ""

    # Interactive screenshot capture
    echo "========================================="
    echo "INTERACTIVE SCREENSHOT CAPTURE"
    echo "========================================="
    echo ""
    echo "Navigate to each screen and press ENTER to capture."
    echo ""

    # Screenshot 1
    log_step "Screenshot 1: Home / Channel List"
    echo "    - Show the main screen with all broadcasters"
    log_prompt "Press ENTER when ready to capture..."
    read
    capture_screenshot "$udid" "$output_dir/01_home_channels.png"
    echo ""

    # Screenshot 2
    log_step "Screenshot 2: Channel Content (ZDF)"
    echo "    - Tap on ZDF to show its content"
    log_prompt "Press ENTER when ready to capture..."
    read
    capture_screenshot "$udid" "$output_dir/02_channel_zdf.png"
    echo ""

    # Screenshot 3
    log_step "Screenshot 3: Theme Browsing"
    echo "    - Navigate to themes/categories view"
    log_prompt "Press ENTER when ready to capture..."
    read
    capture_screenshot "$udid" "$output_dir/03_themes.png"
    echo ""

    # Screenshot 4
    log_step "Screenshot 4: Search Results"
    echo "    - Search for 'Tatort' and show results"
    log_prompt "Press ENTER when ready to capture..."
    read
    capture_screenshot "$udid" "$output_dir/04_search_results.png"
    echo ""

    # Screenshot 5
    log_step "Screenshot 5: Media Detail View"
    echo "    - Tap on a media item to show detail view"
    log_prompt "Press ENTER when ready to capture..."
    read
    capture_screenshot "$udid" "$output_dir/05_media_detail.png"
    echo ""

    # Reset status bar
    log_info "Resetting status bar..."
    xcrun simctl status_bar "$udid" clear

    echo "========================================="
    log_info "Screenshot capture complete!"
    echo "========================================="
    echo ""
    echo "Screenshots saved to: $output_dir"
    echo ""
    ls -la "$output_dir"
}

main "$@"
