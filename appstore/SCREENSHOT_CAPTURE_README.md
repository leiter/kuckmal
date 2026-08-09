# App Store Screenshot Capture Guide

Verified procedure for capturing the German App Store screenshots. Every step below
was run end to end; the caveats are real problems that were hit, not theory.

## What the store actually needs

- **One iPhone size: 6.9" / 1320×2868.** That is the only mandatory iPhone set.
  App Store Connect scales it down for smaller devices.
- **German only.** Screenshots are required for the primary localization (German).
  English inherits the German set unless a separate one is uploaded.
- **No iPad set.** The app ships iPhone-only (`TARGETED_DEVICE_FAMILY = "1"`).

| Key | Simulator | Screenshot size | Notes |
|-----|-----------|-----------------|-------|
| iphone69 | iPhone 16 Pro Max *or* iPhone 17 Pro Max | 1320×2868 | Both produce the required size |

Output goes to `appstore/screenshots/iphone69/de/`.

| # | File | Screen |
|---|------|--------|
| 1 | `01_home_channels.png` | Channel rail + all themes |
| 2 | `02_channel_ard.png` | ARD selected, theme list |
| 3 | `03_search_tatort.png` | Search results for "Tatort" |
| 4 | `04_tatort_titles.png` | Episodes of the Tatort theme |
| 5 | `05_media_detail.png` | Detail view: date, duration, size, description, quality |

## Two constraints that will waste your time if you don't know them

**1. Maestro cannot see the Compose UI.** Compose Multiplatform renders everything
into a single UIView, so `maestro hierarchy` shows only the app container and the
status bar. Any flow using `tapOn: text:` or `assertVisible:` silently fails to
match. All flows here tap by percentage of the screen instead.

**2. Maestro percentages must be integers.** `"16.5%"` throws
`NumberFormatException` internally, and the step is still reported as
`COMPLETED`. A fractional coordinate looks like a working tap that does nothing.
Use `"17%"`.

Because taps are coordinate-based and row positions depend on the current film
list, **verify every screenshot visually** after a run.

## Procedure

### 1. Build and install

```bash
cd iosApp
xcrun simctl boot "iPhone 17 Pro Max"      # or iPhone 16 Pro Max
open -a Simulator

xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' \
  -derivedDataPath /tmp/kuckmal-dd \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" build

UDID=$(xcrun simctl list devices booted | grep -oE '[0-9A-F-]{36}' | head -1)
xcrun simctl install $UDID /tmp/kuckmal-dd/Build/Products/Debug-iphonesimulator/iosApp.app
```

> If the build fails on the "Build Shared Framework" phase, see the JDK note in
> the Troubleshooting section.

### 2. Force German and a clean status bar

```bash
xcrun simctl spawn $UDID defaults write cut.the.crap.kuckmal AppleLanguages -array de
xcrun simctl spawn $UDID defaults write cut.the.crap.kuckmal AppleLocale -string de_DE

xcrun simctl status_bar $UDID override --time "09:41" \
  --batteryState charged --batteryLevel 100 \
  --cellularMode active --cellularBars 4 --wifiMode active --wifiBars 3
```

### 3. Import the film list (~10–15 min)

```bash
maestro test iosApp/.maestro/prepare_app_de.yaml
```

This taps "Filmliste herunterladen" and returns immediately. The import runs
through `Wird entpackt…` then `Einträge werden importiert…`. Poll until the
browse view appears:

```bash
until xcrun simctl io $UDID screenshot /tmp/p.png >/dev/null 2>&1 \
      && [ "$(stat -f%z /tmp/p.png)" -gt 250000 ]; do sleep 60; done
```

The browse view is far denser than the progress screen (~350 KB vs ~120 KB PNG),
which is a crude but reliable "finished" signal. Confirm by opening `/tmp/p.png`.

### 4. Capture

```bash
maestro test iosApp/.maestro/appstore_screenshots_de.yaml
```

Then copy the five PNGs into `appstore/screenshots/iphone69/de/` and **look at
each one**.

## Content guidelines

- Scroll past the leading `#` and `$` themes — alphabetically first, but they look
  like junk data in a store listing.
- Watch for mojibake in the source data (e.g. `Alfons und Gï¿½ste` instead of
  `Gäste`). Some film-list entries carry broken encoding; keep those rows out of
  frame.
- Never use a video playback frame as a screenshot — it shows broadcaster content.
  Same rule as the Play listing (`android/PLAY_RELEASE_PLAN.md`).
- Don't put broadcaster logos in store graphics.

## Troubleshooting

**Build fails: `JAVA_HOME is set to an invalid directory`**
Xcode build phases don't inherit the login shell environment. The "Build Shared
Framework" phase now resolves a JDK itself (Android Studio's JBR, then Homebrew,
then `/usr/libexec/java_home`). If it still fails, no JDK was found — install one.

**Build fails: `ClassNotFoundException: org.jetbrains.kotlin.cli.utilities.MainKt`**
The Kotlin/Native distribution for the pinned Kotlin version isn't in `~/.konan`.
Run `./gradlew commonizeNativeDistribution` (a *root* task), then `./gradlew --stop`
before rebuilding — the daemon caches the classpath computed while it was missing.

**A tap does nothing but Maestro says COMPLETED**
A fractional percentage. Use integers.

**Keyboard covers the results**
`hideKeyboard` fails on this simulator. Tap the return key at `"87%,89%"`. On the
first text entry, iOS also shows a "Type Deutsch und Englisch" onboarding sheet —
dismiss it by tapping Continue at `"50%,95%"`.
