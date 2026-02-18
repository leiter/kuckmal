#!/bin/bash

# App Store Screenshot Capture - Complete Workflow
# ================================================
#
# This script automates the screenshot capture process for iOS App Store submission.
# Due to Maestro driver limitations, some manual interaction is required.
#
# Usage: ./capture_all_screenshots.sh
#
# Prerequisites:
# - Xcode with iOS Simulators installed
# - App already built (run ./gradlew :shared:compileKotlinIosSimulatorArm64 first)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
IOS_APP_DIR="$PROJECT_DIR/iosApp"
SCREENSHOT_DIR="$SCRIPT_DIR/screenshots"
APP_BUNDLE_ID="cut.the.crap.kuckmal"

# Device configurations for App Store requirements
# iPhone 6.7" (required) - iPhone 16 Pro Max on iOS 18.4
IPHONE67_UDID="3A1BF045-504D-4798-AEDD-8C3D2F866EAE"
IPHONE67_NAME="iPhone 16 Pro Max"

# iPhone 6.5" (required) - iPhone 16 Plus on iOS 18.4
IPHONE65_UDID="A76BB289-D27E-496A-A190-300DBE37159D"
IPHONE65_NAME="iPhone 16 Plus"

# iPad 12.9" (required) - iPad Pro 13-inch (M4) on iOS 18.4
IPAD129_UDID="82F17999-8A2C-4704-8391-F0281B793BFD"
IPAD129_NAME="iPad Pro 13-inch (M4)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }
log_prompt() { echo -e "${YELLOW}>>> $1${NC}"; }

# Capture a screenshot
capture() {
    local udid="$1"
    local output_path="$2"
    xcrun simctl io "$udid" screenshot "$output_path"
    log_info "Saved: $output_path"
}

# Set clean status bar
set_clean_status_bar() {
    local udid="$1"
    xcrun simctl status_bar "$udid" override \
        --time "9:41" \
        --batteryState charged \
        --batteryLevel 100 \
        --wifiBars 3 \
        --cellularMode active \
        --cellularBars 4
}

# Reset status bar
reset_status_bar() {
    local udid="$1"
    xcrun simctl status_bar "$udid" clear
}

# Build app if needed
build_app() {
    log_info "Building iOS app..."
    cd "$IOS_APP_DIR"

    # Check if build exists
    if [ -d "build/Build/Products/Debug-iphonesimulator/iosApp.app" ]; then
        log_info "Using existing build"
    else
        log_info "Building fresh..."
        xcodebuild -workspace iosApp.xcworkspace \
            -scheme iosApp \
            -destination "generic/platform=iOS Simulator" \
            -derivedDataPath build \
            build 2>&1 | tail -5
    fi

    cd "$PROJECT_DIR"
}

# Install app on simulator
install_app() {
    local udid="$1"
    local app_path="$IOS_APP_DIR/build/Build/Products/Debug-iphonesimulator/iosApp.app"

    if [ ! -d "$app_path" ]; then
        log_warn "App not found at $app_path"
        log_info "Building app first..."
        build_app
    fi

    xcrun simctl install "$udid" "$app_path"
    log_info "App installed"
}

# Boot simulator
boot_simulator() {
    local udid="$1"
    local name="$2"

    log_info "Shutting down all simulators..."
    xcrun simctl shutdown all 2>/dev/null || true
    sleep 2

    log_info "Booting $name..."
    xcrun simctl boot "$udid"
    open -a Simulator

    log_info "Waiting for boot..."
    xcrun simctl bootstatus "$udid" -b
}

# Capture screenshots for a device/language combination
capture_screenshots_for_device() {
    local udid="$1"
    local device_key="$2"
    local device_name="$3"
    local language="$4"

    local output_dir="$SCREENSHOT_DIR/$device_key/$language"
    mkdir -p "$output_dir"

    echo ""
    echo "========================================"
    echo "  $device_name - $language"
    echo "========================================"
    echo ""

    # Boot and setup
    boot_simulator "$udid" "$device_name"
    install_app "$udid"
    set_clean_status_bar "$udid"

    # Launch app
    log_info "Launching app..."
    xcrun simctl launch "$udid" "$APP_BUNDLE_ID"
    sleep 3

    echo ""
    echo "========================================"
    echo "MANUAL INTERACTION REQUIRED"
    echo "========================================"
    echo ""
    echo "Please perform these steps in the Simulator:"
    echo ""

    # Screenshot 1
    log_step "1. HOME SCREEN - Channel List"
    echo "   - If 'Download Film List' button is shown, TAP IT"
    echo "   - Wait for download to complete (shows progress)"
    echo "   - Once loaded, ensure home screen shows all channels"
    log_prompt "Press ENTER when the home screen shows channel list..."
    read
    capture "$udid" "$output_dir/01_home_channels.png"

    # Screenshot 2
    log_step "2. CHANNEL CONTENT - ZDF"
    echo "   - Tap on 'ZDF' in the channel list"
    echo "   - Wait for content to load"
    log_prompt "Press ENTER when ZDF content is displayed..."
    read
    capture "$udid" "$output_dir/02_channel_zdf.png"

    # Screenshot 3
    log_step "3. THEME BROWSING"
    echo "   - Tap on 'Alle Themen' button"
    echo "   - Show the theme/category view"
    log_prompt "Press ENTER when themes are displayed..."
    read
    capture "$udid" "$output_dir/03_themes.png"

    # Screenshot 4
    log_step "4. SEARCH RESULTS"
    echo "   - Tap the search icon"
    echo "   - Type 'Tatort' in the search field"
    echo "   - Wait for results to appear"
    log_prompt "Press ENTER when search results are displayed..."
    read
    capture "$udid" "$output_dir/04_search_results.png"

    # Screenshot 5
    log_step "5. MEDIA DETAIL"
    echo "   - Tap on any media item to open detail view"
    echo "   - Show the detail information"
    log_prompt "Press ENTER when detail view is displayed..."
    read
    capture "$udid" "$output_dir/05_media_detail.png"

    # Reset status bar
    reset_status_bar "$udid"

    echo ""
    log_info "Screenshots saved to: $output_dir"
    ls -la "$output_dir"
    echo ""
}

# Main menu
main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║         Kuckmal - App Store Screenshot Capture             ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""

    # Create directories
    mkdir -p "$SCREENSHOT_DIR"/{iphone67,iphone65,ipad129}/{de,en}

    # Build app once
    build_app

    echo "Select capture mode:"
    echo "  1) Capture ALL screenshots (6 device/language combinations)"
    echo "  2) iPhone 6.7\" German only"
    echo "  3) iPhone 6.7\" English only"
    echo "  4) iPhone 6.5\" German only"
    echo "  5) iPhone 6.5\" English only"
    echo "  6) iPad 12.9\" German only"
    echo "  7) iPad 12.9\" English only"
    echo "  q) Quit"
    echo ""
    read -p "Enter choice [1-7, q]: " choice

    case $choice in
        1)
            # Capture all - German first, then English
            capture_screenshots_for_device "$IPHONE67_UDID" "iphone67" "$IPHONE67_NAME" "de"
            capture_screenshots_for_device "$IPHONE65_UDID" "iphone65" "$IPHONE65_NAME" "de"
            capture_screenshots_for_device "$IPAD129_UDID" "ipad129" "$IPAD129_NAME" "de"

            echo ""
            log_warn "Now switching to English. Please change the simulator language:"
            echo "   Settings > General > Language & Region > iPhone Language > English"
            log_prompt "Press ENTER when ready to continue with English screenshots..."
            read

            capture_screenshots_for_device "$IPHONE67_UDID" "iphone67" "$IPHONE67_NAME" "en"
            capture_screenshots_for_device "$IPHONE65_UDID" "iphone65" "$IPHONE65_NAME" "en"
            capture_screenshots_for_device "$IPAD129_UDID" "ipad129" "$IPAD129_NAME" "en"
            ;;
        2) capture_screenshots_for_device "$IPHONE67_UDID" "iphone67" "$IPHONE67_NAME" "de" ;;
        3) capture_screenshots_for_device "$IPHONE67_UDID" "iphone67" "$IPHONE67_NAME" "en" ;;
        4) capture_screenshots_for_device "$IPHONE65_UDID" "iphone65" "$IPHONE65_NAME" "de" ;;
        5) capture_screenshots_for_device "$IPHONE65_UDID" "iphone65" "$IPHONE65_NAME" "en" ;;
        6) capture_screenshots_for_device "$IPAD129_UDID" "ipad129" "$IPAD129_NAME" "de" ;;
        7) capture_screenshots_for_device "$IPAD129_UDID" "ipad129" "$IPAD129_NAME" "en" ;;
        q|Q) exit 0 ;;
        *) echo "Invalid choice"; exit 1 ;;
    esac

    echo ""
    echo "========================================"
    log_info "Screenshot capture complete!"
    echo "========================================"
    echo ""
    echo "Screenshots saved to:"
    find "$SCREENSHOT_DIR" -name "*.png" -type f | head -30
    echo ""
    echo "Total screenshots: $(find "$SCREENSHOT_DIR" -name "*.png" -type f | wc -l)"
}

main "$@"
