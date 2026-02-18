# Screenshot Guide for App Store

This guide documents the required screenshots for the iOS App Store listing.

## Required Device Screenshots

### iPhone Screenshots

| Device Type | Display Size | Resolution | Required |
|-------------|--------------|------------|----------|
| iPhone 15 Pro Max | 6.7" | 1290 x 2796 | **Yes** |
| iPhone 11 Pro Max / XS Max | 6.5" | 1242 x 2688 | **Yes** |
| iPhone 8 Plus / 7 Plus | 5.5" | 1242 x 2208 | Optional |

### iPad Screenshots

| Device Type | Display Size | Resolution | Required |
|-------------|--------------|------------|----------|
| iPad Pro 12.9" (6th gen) | 12.9" | 2048 x 2732 | **Yes** |
| iPad Pro 12.9" (2nd gen) | 12.9" | 2048 x 2732 | Optional |

## Screenshot Limits

- **Minimum**: 1 screenshot per device size
- **Maximum**: 10 screenshots per device size
- **Recommended**: 5-6 screenshots to showcase key features

## Recommended Screens to Capture

Capture these screens in order of priority:

### 1. Channel List / Home Screen
- Show the main screen with broadcaster selection
- Demonstrates the variety of available channels
- **Key message**: "All German public broadcasters in one app"

### 2. Topic/Category Browsing
- Display the topic or category view
- Show different content categories (documentaries, news, etc.)
- **Key message**: "Easy navigation by topic"

### 3. Search Results
- Show search functionality with results
- Use a popular search term (e.g., "Tatort", "Tagesschau")
- **Key message**: "Find exactly what you're looking for"

### 4. Media Detail View
- Show a detail view of a specific show/film
- Include description, duration, availability
- **Key message**: "Complete information at a glance"

### 5. Offline/Download Feature (if applicable)
- Show downloaded content or download in progress
- **Key message**: "Watch offline, anywhere"

## Capture Instructions

### Using Xcode Simulator

1. Open Xcode and select your target device simulator
2. Run the app in the simulator
3. Navigate to the screen you want to capture
4. Press **Cmd + S** to save a screenshot
5. Screenshots are saved to Desktop by default

### Using Physical Device

1. Connect device to Mac
2. Open the app and navigate to desired screen
3. Press **Side Button + Volume Up** simultaneously
4. Transfer screenshots via AirDrop or Photos

## Screenshot Tips

- **Remove status bar clutter**: Use a clean time (9:41 AM - Apple's classic)
- **Full battery**: Show full battery indicator
- **No notifications**: Clear all notifications before capture
- **Consistent data**: Use the same sample content across screenshots
- **Light mode first**: Capture light mode, consider dark mode as bonus

## Localization

Capture separate screenshot sets for:
- **German (de-DE)**: Primary market
- **English (en-US)**: International reach

Ensure the app language matches the screenshot set language.

## File Naming Convention

```
[device]_[number]_[screen]_[language].png

Examples:
iphone67_01_home_de.png
iphone67_02_search_de.png
iphone65_01_home_en.png
ipad129_01_home_de.png
```

## App Store Connect Upload

1. Go to App Store Connect > Your App > App Store tab
2. Scroll to "Screenshots" section
3. Select device size tab
4. Drag and drop screenshots in desired order
5. Repeat for each language localization

## Preview Video (Optional)

- **Duration**: 15-30 seconds
- **Resolution**: Same as screenshots for each device
- **Format**: H.264, .mov or .mp4
- **Audio**: Optional, consider background music

A short video showing navigation through the app can be very effective but is not required for initial submission.

---

## Automated Screenshots with Maestro

Use Maestro to automatically capture screenshots for the App Store.

### Prerequisites

1. Install Maestro: `curl -Ls "https://get.maestro.mobile.dev" | bash`
2. Have iOS Simulator running with desired device
3. Build and install the app on the simulator

### Maestro Flows

Two flows are provided in `iosApp/.maestro/`:

| Flow | Purpose |
|------|---------|
| `prepare_app.yaml` | Downloads film list to populate app with data |
| `appstore_screenshots.yaml` | Captures 5 key screens for App Store |

### Usage

```bash
# Step 1: Prepare the app with data (run once)
maestro test iosApp/.maestro/prepare_app.yaml

# Step 2: Capture screenshots
maestro test iosApp/.maestro/appstore_screenshots.yaml
```

### Screenshots Captured

The `appstore_screenshots.yaml` flow captures:

1. **appstore_01_home_channels** - Main channel list showing all broadcasters
2. **appstore_02_channel_zdf** - ZDF channel content
3. **appstore_03_themes** - Theme browsing view
4. **appstore_04_search_results** - Search results for "Tatort"
5. **appstore_05_media_detail** - Media detail view

Screenshots are saved to Maestro's default output directory. Copy them to `appstore/screenshots/` and rename according to the file naming convention.

### Capturing for Different Devices

Run the flows on different simulator devices to capture all required sizes:

```bash
# iPhone 15 Pro Max (6.7")
xcrun simctl boot "iPhone 15 Pro Max"
maestro test iosApp/.maestro/appstore_screenshots.yaml

# iPhone 11 Pro Max (6.5")
xcrun simctl boot "iPhone 11 Pro Max"
maestro test iosApp/.maestro/appstore_screenshots.yaml

# iPad Pro 12.9"
xcrun simctl boot "iPad Pro (12.9-inch) (6th generation)"
maestro test iosApp/.maestro/appstore_screenshots.yaml
```

---

## Quick Capture Script

The easiest way to capture all screenshots is using the interactive script:

```bash
./appstore/capture_all_screenshots.sh
```

This script will:
1. Build the app if needed
2. Boot each simulator in sequence
3. Set up a clean status bar (9:41 AM, full battery)
4. Guide you through capturing each screenshot
5. Save screenshots to the correct directories

### Alternative: Manual Capture

For individual screenshots:

```bash
# Set up clean status bar
xcrun simctl status_bar booted override --time "9:41" --batteryState charged --batteryLevel 100

# Take screenshot
xcrun simctl io booted screenshot appstore/screenshots/iphone67/de/01_home.png

# Reset status bar when done
xcrun simctl status_bar booted clear
```

---

## Checklist

- [ ] iPhone 6.7" screenshots (1290 x 2796) - German
- [ ] iPhone 6.7" screenshots (1290 x 2796) - English
- [ ] iPhone 6.5" screenshots (1242 x 2688) - German
- [ ] iPhone 6.5" screenshots (1242 x 2688) - English
- [ ] iPad Pro 12.9" screenshots (2048 x 2732) - German
- [ ] iPad Pro 12.9" screenshots (2048 x 2732) - English
- [ ] All screenshots show clean status bar
- [ ] App language matches screenshot language
- [ ] Screenshots uploaded to App Store Connect
