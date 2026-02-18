# App Store Screenshot Capture Guide

This guide explains how to capture screenshots for the iOS App Store submission.

## Prerequisites

1. Xcode installed with iOS Simulators
2. App built and ready for installation
3. Simulator running

## Required Screenshots

For each device size and language combination:

| # | Screen | Description |
|---|--------|-------------|
| 1 | Home/Channels | Main screen showing all broadcasters |
| 2 | Channel Content | Content from ZDF channel |
| 3 | Themes | Theme browsing view |
| 4 | Search Results | Search for "Tatort" |
| 5 | Media Detail | Detail view of a media item |

## Device Configurations

| Key | Device | Resolution | App Store Size |
|-----|--------|------------|----------------|
| iphone67 | iPhone 16 Pro Max | 1320×2868 @3x | 6.7" (1290×2796) |
| iphone65 | iPhone 16 Plus | 1284×2778 @3x | 6.5" (1242×2688) |
| ipad129 | iPad Pro 13-inch (M4) | 2064×2752 | 12.9" (2048×2732) |

## Quick Start

### Step 1: Build and Install the App

```bash
cd /Users/user289697/Documents/kuckmal/iosApp

# Boot simulator (iPhone 16 Pro Max for 6.7" screenshots)
xcrun simctl boot "iPhone 16 Pro Max"
open -a Simulator

# Build app
xcodebuild -workspace iosApp.xcworkspace \
  -scheme iosApp \
  -destination "platform=iOS Simulator,name=iPhone 16 Pro Max" \
  -derivedDataPath build build

# Install app
xcrun simctl install booted build/Build/Products/Debug-iphonesimulator/iosApp.app
```

### Step 2: Set Clean Status Bar

```bash
# Get simulator UDID
UDID=$(xcrun simctl list devices booted | grep -oE '\([A-F0-9-]{36}\)' | tr -d '()')

# Set clean status bar (9:41 AM, full battery)
xcrun simctl status_bar "$UDID" override \
  --time "9:41" \
  --batteryState charged \
  --batteryLevel 100 \
  --wifiBars 3 \
  --cellularMode active \
  --cellularBars 4
```

### Step 3: Launch App and Download Data

```bash
# Launch app
xcrun simctl launch booted cut.the.crap.kuckmal

# MANUAL: Tap "Download Film List" button in the app
# Wait for download to complete (~2 minutes)
```

### Step 4: Capture Screenshots

Navigate to each screen manually, then capture:

```bash
# Output directory
LANG="de"  # or "en"
DEVICE="iphone67"  # or iphone65, ipad129
OUTPUT_DIR="/Users/user289697/Documents/kuckmal/appstore/screenshots/$DEVICE/$LANG"
mkdir -p "$OUTPUT_DIR"

# Capture screenshots (run each after navigating to the correct screen)
xcrun simctl io booted screenshot "$OUTPUT_DIR/01_home_channels.png"
xcrun simctl io booted screenshot "$OUTPUT_DIR/02_channel_zdf.png"
xcrun simctl io booted screenshot "$OUTPUT_DIR/03_themes.png"
xcrun simctl io booted screenshot "$OUTPUT_DIR/04_search_results.png"
xcrun simctl io booted screenshot "$OUTPUT_DIR/05_media_detail.png"
```

### Step 5: Reset Status Bar

```bash
xcrun simctl status_bar booted clear
```

## Automated Script

Use the manual capture script for guided screenshot capture:

```bash
./appstore/capture_screenshots_manual.sh iphone67 de
```

## Changing Simulator Language

To capture screenshots in different languages:

1. Open Settings app in Simulator
2. Go to General > Language & Region
3. Change language to German (Deutsch) or English
4. Restart the app

Or use Xcode:
1. Edit Scheme > Run > Options
2. Set "App Language" to German or English

## Full Capture Workflow

### German Screenshots (DE)

```bash
# 1. Set simulator language to German
# 2. For each device:
for DEVICE in iphone67 iphone65 ipad129; do
  ./appstore/capture_screenshots_manual.sh $DEVICE de
done
```

### English Screenshots (EN)

```bash
# 1. Set simulator language to English
# 2. For each device:
for DEVICE in iphone67 iphone65 ipad129; do
  ./appstore/capture_screenshots_manual.sh $DEVICE en
done
```

## Directory Structure

```
appstore/screenshots/
├── iphone67/
│   ├── de/
│   │   ├── 01_home_channels.png
│   │   ├── 02_channel_zdf.png
│   │   ├── 03_themes.png
│   │   ├── 04_search_results.png
│   │   └── 05_media_detail.png
│   └── en/
│       └── ...
├── iphone65/
│   ├── de/
│   └── en/
└── ipad129/
    ├── de/
    └── en/
```

## Troubleshooting

### Maestro Not Working

If Maestro fails with "iOS driver not ready", try:
1. Ensure only one simulator is booted
2. Restart the simulator
3. Update Maestro: `curl -Ls "https://get.maestro.mobile.dev" | bash`

### App Shows "No film list found"

The app needs to download the film list before screenshots can be captured:
1. Launch the app
2. Tap "Download Film List" button
3. Wait for download to complete (progress shown in app)

### Wrong Language in Screenshots

1. Check simulator language in Settings > General > Language & Region
2. Or set App Language in Xcode scheme settings
3. Restart the app after changing language
