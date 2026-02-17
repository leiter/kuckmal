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
| **Age Rating** | 4+ (see Age Rating section below) |

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
| **Privacy Policy URL** | `https://kuckmal.cutthecrap.link/privacy` |
| **Support URL** | `https://kuckmal.cutthecrap.link/support` |
| **Marketing URL** (optional) | `https://kuckmal.cutthecrap.link` |

> **Note**: Host the privacy policy files (`privacy-policy-en.md`, `privacy-policy-de.md`) at the privacy URL. Create a simple support page with FAQ and contact information.

---

## Age Rating Questionnaire

Answer these questions in App Store Connect:

| Question | Answer |
|----------|--------|
| Cartoon or Fantasy Violence | None |
| Realistic Violence | None |
| Prolonged Graphic or Sadistic Realistic Violence | None |
| Profanity or Crude Humor | None |
| Mature/Suggestive Themes | None |
| Horror/Fear Themes | None |
| Medical/Treatment Information | None |
| Alcohol, Tobacco, or Drug Use or References | None |
| Simulated Gambling | None |
| Sexual Content or Nudity | None |
| Graphic Sexual Content and Nudity | None |
| Unrestricted Web Access | No |
| Gambling with Real Currency | No |

**Result**: 4+ (suitable for all ages)

> **Note**: The app only provides access to content from German public broadcasters. The broadcasters themselves apply their own age ratings to individual content. Kuckmal does not filter or modify this content.

---

## Build & Version

| Field | Value |
|-------|-------|
| Version | 1.0.0 |
| Build | 1 |
| Copyright | 2026 [Your Name/Company] |

---

## App Review Information

### Contact Information

| Field | Value |
|-------|-------|
| First Name | [YOUR_FIRST_NAME] |
| Last Name | [YOUR_LAST_NAME] |
| Phone | [YOUR_PHONE] |
| Email | [YOUR_EMAIL] |

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

| Device | Resolution | Status |
|--------|------------|--------|
| iPhone 6.7" (iPhone 15 Pro Max) | 1290 x 2796 | Required |
| iPhone 6.5" (iPhone 11 Pro Max) | 1242 x 2688 | Required |
| iPad Pro 12.9" | 2048 x 2732 | Required |

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

### Screenshots

- [ ] iPhone 6.7" screenshots (German) - 5 images
- [ ] iPhone 6.7" screenshots (English) - 5 images
- [ ] iPhone 6.5" screenshots (German) - 5 images
- [ ] iPhone 6.5" screenshots (English) - 5 images
- [ ] iPad Pro 12.9" screenshots (German) - 5 images
- [ ] iPad Pro 12.9" screenshots (English) - 5 images

### App Binary

- [ ] Archive built in Xcode (Product > Archive)
- [ ] Archive validated successfully
- [ ] Archive uploaded to App Store Connect
- [ ] Build processing complete
- [ ] Build selected for submission

### Legal & Compliance

- [ ] Privacy policy hosted and accessible
- [ ] Export compliance answered (No encryption)
- [ ] Content rights declared
- [ ] IDFA usage declared (None)

### Final Review

- [ ] Test app on physical device
- [ ] Verify all links work (support, privacy)
- [ ] Review notes complete in App Store Connect
- [ ] Contact information correct

---

## Export Compliance

Kuckmal uses HTTPS for network communication, which uses standard encryption. In App Store Connect:

| Question | Answer |
|----------|--------|
| Does your app use encryption? | Yes |
| Does your app qualify for any exemptions? | Yes |
| Exemption | Standard encryption exempt under EAR (uses only standard HTTPS) |

---

## Common Rejection Reasons (Avoid These)

1. **Broken links**: Ensure privacy policy and support URLs work
2. **Incomplete metadata**: Fill all required fields
3. **Poor screenshots**: Use high-quality, correctly sized images
4. **Crash on launch**: Test thoroughly on device
5. **Placeholder content**: Replace [YOUR_EMAIL] etc. before submission
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
