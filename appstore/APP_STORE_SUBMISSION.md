# App Store Submission Guide

**App Name**: Kuckmal
**Bundle ID**: cut.the.crap.kuckmal

This document provides a complete checklist and reference for submitting Kuckmal to the iOS App Store.

---

## App Store Connect Metadata

### App Information

| Field | Value |
|-------|-------|
| **App Name** | Kuckmal |
| **Subtitle** (30 chars max) | German Public TV Libraries |
| **Primary Language** | German |
| **Category** | Entertainment |
| **Secondary Category** | Utilities |
| **Content Rights** | Does not contain third-party content requiring rights |
| **Age Rating** | 12+/16+ (see Age Rating section below) |
| **Availability** | **Germany only** |
| **Price** | Free |
| **Devices** | iPhone only (`TARGETED_DEVICE_FAMILY = "1"`) |
| **Team** | K4K982LMZ9 |

### Localized Metadata

#### German (de-DE) - Primary

| Field | File |
|-------|------|
| Name | Kuckmal |
| Subtitle | Deutsche Mediatheken vereint |
| Description | `ios/de/description.txt` |
| Keywords | `ios/de/keywords.txt` |
| What's New | `ios/de/release-notes.txt` |

#### English (en-US)

| Field | File |
|-------|------|
| Name | Kuckmal |
| Subtitle | German Public TV Libraries |
| Description | `ios/en/description.txt` |
| Keywords | `ios/en/keywords.txt` |
| What's New | `ios/en/release-notes.txt` |

### URLs (Required)

| Field | Value |
|-------|-------|
Hosted on GitHub Pages from `leiter/kuckmal` (`/docs` folder, `main` branch). Set per localization:

| Field | German (de-DE) | English (en-US) |
|-------|----------------|-----------------|
| **Privacy Policy URL** | `https://leiter.github.io/kuckmal/privacy/` | `https://leiter.github.io/kuckmal/privacy/en/` |
| **Support URL** | `https://leiter.github.io/kuckmal/support/` | `https://leiter.github.io/kuckmal/support/en/` |
| **Marketing URL** (optional) | `https://leiter.github.io/kuckmal/` | `https://leiter.github.io/kuckmal/` |

> **Note**: The pages under `docs/` are generated from the canonical sources in `appstore/` by `scripts/build_docs_site.sh`. Edit the `appstore/` file, re-run the script, commit both. GitHub Pages must be enabled in repository settings (Deploy from branch → `main` → `/docs`); Pages on a private repository requires a paid plan.

---

## Age Rating Questionnaire

Answer against the **catalogue**, not against the app's own interface. Kuckmal surfaces an unfiltered broadcaster catalogue — crime drama (*Tatort*), news footage of war and disasters, documentaries on crime and drug use — and provides no content filtering and no parental controls. These answers match the IARC answers used for Google Play (`android/PLAY_CONSOLE_ANSWERS.md` §5).

| Question | Answer |
|----------|--------|
| Cartoon or Fantasy Violence | None |
| Realistic Violence | Infrequent/Mild |
| Prolonged Graphic or Sadistic Realistic Violence | None |
| Profanity or Crude Humor | None |
| Mature/Suggestive Themes | Infrequent/Mild |
| Horror/Fear Themes | Infrequent/Mild |
| Medical/Treatment Information | Infrequent/Mild |
| Alcohol, Tobacco, or Drug Use or References | Infrequent/Mild |
| Simulated Gambling | None |
| Sexual Content or Nudity | None |
| Graphic Sexual Content and Nudity | None |
| Unrestricted Web Access | **Yes** — playback hands stream URLs to the system browser/player |
| Gambling with Real Currency | No |

**Expected result**: 12+ or 16+

> **Do not** try to optimise this down to 4+. The app has no control over the catalogue, and a rating that Apple disagrees with is a rejection or a forced re-rating. Do not declare children as a target audience despite KiKA/ZDFtivi content.

---

## Build & Version

Both values live in `iosApp/iosApp/Info.plist` (there are no `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` build settings).

| Field | Value | Key |
|-------|-------|-----|
| Version | 1.0.0 | `CFBundleShortVersionString` |
| Build | 26 | `CFBundleVersion` |
| Copyright | 2026 [RIGHTS_HOLDER — fill in] | App Store Connect field |

> Build numbers must increase monotonically per bundle ID. Earlier local builds already reached 25, so 1.0.0 ships as build 26. Never reset to 1.

---

## App Review Information

### Contact Information

| Field | Value |
|-------|-------|
| First Name | [YOUR_FIRST_NAME — fill in] |
| Last Name | [YOUR_LAST_NAME — fill in] |
| Phone | [YOUR_PHONE — fill in] |
| Email | kuckmal@cutthecrap.link |

### Notes for Review

See `review-notes.txt` for the complete review notes to paste into App Store Connect.

Key points:
- No login required
- No demo account needed
- All content from publicly available sources
- No user data collection

---

## Screenshots

See `SCREENSHOT_GUIDE.md` for detailed screenshot requirements.

### Required Screenshot Sizes

The app ships iPhone-only, so no iPad set is needed.

| Device | Resolution | Status |
|--------|------------|--------|
| iPhone 6.9" (iPhone 16 Pro Max) | 1320 x 2868 | **Required** |
| iPhone 6.5" | 1242 x 2688 | Optional |
| iPad | — | Not applicable (iPhone-only build) |

Screenshots are required for the **primary localization (German)** only; English inherits the German set unless you upload a separate one.

The German set in `screenshots/iphone69/de/` was captured against the localized build: home, ARD channel, Tatort search, Tatort episodes, media detail. See `SCREENSHOT_CAPTURE_README.md` to reproduce.

### Screenshot Capture

```bash
# 1. Prepare app with data
maestro test iosApp/.maestro/prepare_app.yaml

# 2. Capture screenshots
maestro test iosApp/.maestro/appstore_screenshots.yaml
```

---

## Pre-Submission Checklist

### App Store Connect Setup

- [ ] Apple Developer Program membership active
- [ ] App record created in App Store Connect
- [ ] Bundle ID registered in Developer Portal
- [ ] App name reserved

### Metadata

- [ ] App name set (Kuckmal)
- [ ] Subtitle set (German/English)
- [ ] Description uploaded (German/English)
- [ ] Keywords uploaded (German/English)
- [ ] What's New / Release Notes uploaded
- [ ] Privacy Policy URL set and accessible
- [ ] Support URL set and accessible
- [ ] Category selected (Entertainment)
- [ ] Age rating questionnaire completed
- [ ] **Availability set to Germany only** (Pricing and Availability)
- [ ] Price set to Free
- [ ] Primary language set to German

### Screenshots

- [x] iPhone 6.9" screenshots (German) — 5 images at 1320×2868 in `screenshots/iphone69/de/`
- [ ] English set (optional — inherits German if omitted)

### Code / project (done in this pass)

- [x] German localization of the Compose UI (`shared/src/commonMain/composeResources/values-de/strings.xml`)
- [x] `CFBundleLocalizations` = de, en
- [x] IP geolocation replaced with device region (`currentRegionCode()`); `GeoDetector` deleted
- [x] `ITSAppUsesNonExemptEncryption` = false
- [x] `UIRequiredDeviceCapabilities` armv7 → arm64
- [x] Version 1.0.0 / build 26
- [x] `TARGETED_DEVICE_FAMILY` = "1" (iPhone only)
- [x] `PrivacyInfo.xcprivacy` added and bundled
- [x] App icon alpha channel stripped (ITMS-90717)
- [x] Dangling `KuckMal.mobileprovision` reference removed from the project file

### App Binary

- [ ] Archive built in Xcode (Product > Archive)
- [ ] Archive validated successfully
- [ ] Archive uploaded to App Store Connect
- [ ] Build processing complete
- [ ] Build selected for submission

### Legal & Compliance

- [ ] GitHub Pages enabled and privacy/support URLs load publicly (test in a private window)
- [ ] App Privacy declared as **Data Not Collected**, tracking = No
- [ ] Export compliance (pre-answered via `ITSAppUsesNonExemptEncryption`)
- [ ] Content rights declared
- [ ] IDFA usage declared (None)
- [ ] Copyright rights holder filled in
- [ ] Reviewer contact name and phone filled in

### Final Review

- [ ] Test app on physical device
- [ ] Verify all links work (support, privacy)
- [ ] Review notes complete in App Store Connect
- [ ] Contact information correct

---

## Export Compliance

Kuckmal uses HTTPS for network communication, which uses standard encryption. In App Store Connect:

`ITSAppUsesNonExemptEncryption` is set to `false` in `Info.plist`, so App Store Connect no longer prompts on each upload. The app uses only standard HTTPS, which is exempt under the EAR.

---

## Common Rejection Reasons (Avoid These)

1. **Broken links**: Ensure privacy policy and support URLs work
2. **Incomplete metadata**: Fill all required fields
3. **Poor screenshots**: Use high-quality, correctly sized images
4. **Crash on launch**: Test thoroughly on device
5. **Placeholder content**: the copyright rights holder and reviewer name/phone are still marked "fill in" above
6. **Guideline 4.2 (Minimum Functionality)**: App must provide value - Kuckmal aggregates multiple sources which adds value
7. **Guideline 5.2.1 (Third-Party Content)**: Document that content is from public sources

---

## Submission Steps

1. **Prepare Build**
   ```bash
   # Open Xcode project
   open iosApp/iosApp.xcodeproj

   # Select "Any iOS Device" as destination
   # Product > Archive
   # Wait for archive to complete
   # Distribute App > App Store Connect > Upload
   ```

2. **Configure in App Store Connect**
   - Go to [appstoreconnect.apple.com](https://appstoreconnect.apple.com)
   - Select your app
   - Fill in all metadata fields
   - Upload screenshots
   - Select build
   - Add review notes

3. **Submit for Review**
   - Click "Add for Review"
   - Answer final questions
   - Submit

4. **Wait for Review**
   - Typical review time: 24-48 hours
   - May receive questions or rejection
   - Respond promptly to any issues

---

## Post-Submission

- Monitor App Store Connect for review status
- Be ready to respond to reviewer questions
- Once approved, choose release option:
  - Manually release
  - Automatically release
  - Release on specific date
