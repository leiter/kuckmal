# Google Play release plan — Kuckmal (Android)

Package `cut.the.crap.kuckmal` · first release · plan written 2026-07-24

Everything that can be prepared without a Play Console account is in this
directory. What remains is the work that needs your keys, your hosting and your
decisions.

- Store text (DE + EN): `store-listing/`
- Graphics: `graphics/`
- Screenshots: `screenshots/phone/de-DE/`
- Every Play Console question, answered: `PLAY_CONSOLE_ANSWERS.md`

---

## What was verified on device

Captured on a Pixel 2 (Android 11, 1080×1920) against the live production API.

| Check | Result |
|---|---|
| Debug build installs and runs | ✅ |
| Live catalogue loads (browse, themes, entries) | ✅ |
| Full-text search | ✅ (`Tatort`, `Tagesschau`) |
| Detail view | ✅ |
| Video playback, current item | ✅ (Tagesschau 24.07.2026) |
| Video playback, 5-month-old item | ❌ HTTP 404 — expired at the broadcaster |
| **Release build (R8 + resource shrinking) compiles** | ✅ |
| **Release build launches, no R8 crash** | ✅ — Room, Koin and Gson survive shrinking |
| Release build first-run download + import, clean install | ✅ 705,100 entries, but ~13 min — see B1 |
| Storage permission scoped away on API 29+ | ✅ not requested at all on API 30 |
| Video download enqueues and writes a file | ✅ no crash, file created |
| Progressive download produces a real file | ✅ 355 MB `ftyp mp42` MP4 (ARTE) |
| HLS entry refuses instead of writing junk | ✅ message shown, no file — see B1b |

The R8 result matters most: minification with Room + Koin + Gson is the classic
way a release build dies where the debug build is fine, and this one is clean.

---

## Blockers — must be fixed before upload

### A1. No signing configuration
`androidApp/build.gradle` has no `signingConfig` at all, so
`bundleRelease` produces an unsigned artifact that Play rejects.

Create an upload key (**you** should run this — the keystore is a credential and
must not end up in the repo or in a chat log):

```bash
keytool -genkeypair -v \
  -keystore ~/keys/kuckmal-upload.jks \
  -alias kuckmal-upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Put the credentials in `~/.gradle/gradle.properties` (outside the repo):

```properties
KUCKMAL_STORE_FILE=/home/<you>/keys/kuckmal-upload.jks
KUCKMAL_STORE_PASSWORD=…
KUCKMAL_KEY_ALIAS=kuckmal-upload
KUCKMAL_KEY_PASSWORD=…
```

Then add to `androidApp/build.gradle`:

```groovy
android {
    signingConfigs {
        release {
            if (project.hasProperty('KUCKMAL_STORE_FILE')) {
                storeFile file(KUCKMAL_STORE_FILE)
                storePassword KUCKMAL_STORE_PASSWORD
                keyAlias KUCKMAL_KEY_ALIAS
                keyPassword KUCKMAL_KEY_PASSWORD
            }
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            // … existing settings
        }
    }
}
```

Back the `.jks` up somewhere durable. Losing it means you can never update the
app under this package name again (Play App Signing mitigates this — enrol when
Play offers it during the first upload).

### A2. App name was still `ÖrFinder` — ✅ FIXED
`app_name` is now `Kuckmal` in both `values/strings.xml` and
`values-de/strings.xml`. Verified in the built APK: `application-label:'Kuckmal'`
across every locale, and no `ÖrFinder` string remains in the package.

### A3. Privacy policy is mandatory, unhosted, and currently inaccurate
Blocking for publication. The policy claims data "never leaves your device",
but `GeoDetector.kt` sends the user's IP to `ip-api.com` over plain HTTP.
Full explanation and three fix options in `PLAY_CONSOLE_ANSWERS.md` §8.2 and §9.
Also replace the `[YOUR_EMAIL]` placeholders throughout `appstore/`.

### A4. Storage permissions were over-broad — ✅ FIXED
`READ_EXTERNAL_STORAGE` is removed (it had no non-test usage anywhere in main
source) and `WRITE_EXTERNAL_STORAGE` is now scoped with `maxSdkVersion="28"`,
which is the only range where `DownloadManager.setDestinationInExternalPublicDir`
still needs it.

Verified against the **release** APK:

```
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
uses-permission: name='android.permission.WRITE_EXTERNAL_STORAGE' maxSdkVersion='28'
```

(`REORDER_TASKS` appears in the *debug* build only — it comes from
`androidx.fragment:fragment-testing`, which is a `debugImplementation`
dependency, so it never reaches production.)

**Runtime permission request — ✅ FIXED.** There was no `requestPermissions` call
anywhere in main source, so on API 23–28 `WRITE_EXTERNAL_STORAGE` was never
granted at runtime and downloads to the public Downloads directory would fail.
`ComposeMainScreen` now gates the download action through `withStoragePermission`,
which requests the permission via `ActivityResultContracts.RequestPermission()`
and replays the pending download once it is granted (or shows
`error_storage_permission_required` if it is denied). Below API 23 the permission
is install-time granted, so `checkSelfPermission` already passes and no dialog
appears; from API 29 the branch is skipped entirely.

Verified on API 30 (Pixel 2): `WRITE_EXTERNAL_STORAGE` does not even appear in
`dumpsys package … requested permissions`, no permission dialog is shown, and a
download still enqueues and writes to `/sdcard/Download/Kuckmal/…`. **The API
23–28 branch itself is not yet verified on a device** — no system image in that
range is installed locally. Test it on an API 28 emulator before release.

### A5. Ship an AAB, not an APK
Play requires the App Bundle format for new apps:

```bash
./gradlew :androidApp:bundleRelease
# androidApp/build/outputs/bundle/release/androidApp-release.aab
```

---

## Should fix before a public launch

### B1. First run takes ~13 minutes and needs ~500 MB of transient storage
Measured end to end on the release build, from a clean install (Pixel 2, Wi-Fi).
The run **completed successfully** — 705,100 entries imported, and the app is
fully usable afterwards. But the cost is high:

| Stage | Measured |
|---|---|
| Download `Filmliste-akt.xz` | **75 MB**, ~48 s |
| Decompress to JSON | **464 MB** written to app storage, ~90 s |
| Parse into Room | 705,100 entries, **~10 min 45 s** |
| Cleanup | the 464 MB JSON is deleted afterwards ✅ |
| **Total** | **~13 minutes** |

Problems, one now fixed:

- ~~The welcome dialog says **"Download size: ~30 MB"**~~ — ✅ **FIXED**. It now
  reads `Download: ca. 75 MB` (de) / `Download size: ~75 MB` (en). The German
  translation was missing entirely, so the dialog had been falling back to the
  English line inside an otherwise German dialog; that is corrected too. The
  same string in `shared/src/commonMain/composeResources/values/strings.xml`
  (iOS/desktop) was updated to match. Verified on device.
- The 464 MB intermediate file is transient, but it still has to fit. On a device
  that is low on storage the import will fail, and there is no pre-flight
  free-space check and no clear error path. The test device dropped to 3 % free
  during the import.
- 13 minutes is a long way past what a user will wait without abandoning,
  especially with no time estimate on screen.

None of this blocks review, but all of it will drive one-star "app is broken /
eats my storage" reviews on day one. The listing text now discloses the real
figures, but disclosure is not a substitute for a free-space pre-check, a
progress estimate, and a streaming import that skips the 464 MB temp file.

### B1b. Download produced a broken 6 KB file for HLS sources — ✅ FIXED
Found while regression-testing the permission fix on a real device.

Downloading *Tagesschau vom 24.07.2026* (SRF) produced:

```
/sdcard/Download/Kuckmal/SRF/Tagesschau_vom_24.07.2026.mp4   6026 bytes
```

whose contents are:

```
#EXTM3U
#EXT-X-TARGETDURATION:10
#EXTINF:5.160,
segment-1-f4-v1-a1.ts
…
```

That is the HLS **playlist**, not the video. Two compounding bugs:

1. `DownloadManager` cannot download an HLS stream — it fetches the `.m3u8`
   manifest and stops. The actual media lives in hundreds of `.ts` segments that
   have to be fetched and muxed.
2. The extension is chosen by testing `.mp4` **before** `.m3u8`
   (`AppModule.kt` and `MediaViewModel.kt`, duplicated in both). SRF URLs look
   like `…_h264_,q40,…,.mp4.csmil/index-f4-v1-a1.m3u8` — they contain both, so
   the file is misnamed `.mp4` and looks like a real video to the user.

The user gets a file that appears to be a downloaded episode and fails to play.
"Herunterladen" is advertised in the store listing, so this needs handling before
launch.

**How widespread:** measured over a 1,094-entry sample of `Filmliste-diff.xz`,
**99.4 % of entries are progressive `.mp4` and 0.6 % are HLS.** HLS is not spread
evenly — it is concentrated in specific broadcasters: 100 % of ORF entries in the
sample were HLS, and the SRF entry that exposed this bug was too. So downloads
work for the large majority of the catalogue and fail for ORF/SRF-style entries.
(The sample is a few hours of updates and is ZDF-heavy, so treat the exact
percentage as directional, not authoritative; the per-broadcaster pattern is the
reliable part.)

**Falling back to another quality does not help.** For every HLS entry in the
sample, all quality variants (`Url`, `Url_HD`, `Url_Klein`) were HLS too — 7 of 7,
with zero progressive alternatives. The SRF case matched: base and small were
both `index-f5/f4-v1-a1.m3u8`.

**Fix applied:** `MediaUrlUtils.isHlsStream()` now gates the download in both
`AppModule.kt` (Compose path, live) and `MediaViewModel.kt` (legacy XML path).
HLS entries show `error_download_not_supported_stream` instead of writing junk,
and `MediaUrlUtils.downloadFileExtension()` tests `.m3u8` before `.mp4` so the
misnaming is gone too.

Verified on device: the SRF entry now shows the German message and writes no
file; an ARTE progressive entry still downloads a real 355 MB `ftyp mp42` MP4.

Making HLS genuinely downloadable (media3 `HlsDownloader`, segment
concatenation, TS→MP4 remux) is tracked in `TODO.md` under
**Download Feature — Fix and Refinement**. Not required for this release.

### B2. Detail view has a visible layout defect
The metadata card is overlapped by the quality/actions panel — the "Size" row is
clipped in half. Visible in `screenshots/phone/de-DE/05_details.png`, which is
one of the five store screenshots. Worth fixing before the screenshots ship.

### B3. Mixed-language UI
The German UI shows English labels: `Theme`, `Title`, `Date`, `Time`, `Duration`,
`Size`, `Quality`, `High`, `Low`. The default locale is German and the store
listing is German-first.

These are all cases where `values-de/strings.xml` is missing a key and Android
falls back to the English default — the same root cause as the download-size line
fixed in B1. Diffing the keys of the two files will find the rest in one pass.

### B4. Broadcaster logos do not render in the detail view
The channel header shows a grey box with the channel name as text, although PNG
logos exist in `androidApp/src/main/res/drawable/` (matches the open item in
`TODO.md`). Cosmetic, but it is the largest element on the detail screen.

### B5. Stale breadcrumb after a new search
After searching `Tagesschau`, the header still reads `Titel: Tatort`
(visible in `screenshots/_raw/menu.png`).

### B6. Dead code shipped in the release
`MediaActivity` (legacy XML UI) is still in the manifest as a non-exported
activity. Per `CLAUDE.md` the XML code is legacy. Removing it shrinks the
binary and the review surface.

---

## Trademark and content risk — read before you publish

Kuckmal is an unaffiliated app that indexes ARD/ZDF/Arte/ORF/SRF catalogues and
displays their names and brand colours. Three things reduce the risk of a
takedown notice:

1. **The disclaimer is in the listing text** (both locales) and states plainly
   that the app is independent, hosts nothing, and links to official servers.
   Keep it in — do not trim it for length.
2. **Do not use broadcaster logos in store graphics.** The feature graphic and
   icon are deliberately brand-neutral. The channel names in the description are
   nominative use, which is defensible; a logo in a marketing asset is not.
3. **Do not use a video frame as a store screenshot.** The captured playback
   screenshot is parked in `screenshots/_raw/` and marked
   `player_reference_do_not_publish.png`. It shows an ARD broadcast frame —
   fine as internal evidence that playback works, not fine as marketing.

The five selected screenshots show only app UI, catalogue titles and channel
name chips.

---

## Assets produced

### Screenshots — `screenshots/phone/de-DE/`
Five, 1080×1920 (9:16), from the live catalogue, status bar cleaned via SysUI
demo mode. Play requires 2–8 phone screenshots; these satisfy the size and
aspect rules.

| File | Shows |
|---|---|
| `01_alle_sender.png` | Channel list and cross-broadcaster topics |
| `02_sender_themen.png` | ARD selected, its topics |
| `03_suche.png` | Search for "Tatort" across the catalogue |
| `04_sendungen.png` | Episode list for a topic |
| `05_details.png` | Detail view with metadata, play and download |

Not published, kept for reference: `screenshots/_raw/`.

### Graphics — `graphics/`
Generated from `script/AppIcon_1024x1024.png` by
`scripts/generate_play_assets.py`.

| File | Purpose | Spec |
|---|---|---|
| `play_icon_512.png` | Store icon | 512×512, RGB, full-bleed, no alpha |
| `feature_graphic_1024x500.png` | Feature graphic | 1024×500, RGB |
| `tv_banner_1280x720.png` | TV banner | 1280×720 — only if TV is opted into |

### Text — `store-listing/`
`de-DE` and `en-US`, each with app name, short description, full description and
release notes. All verified against Play's limits (30 / 80 / 4000 / 500 chars).

---

## Order of work

1. Fix A1, A3, A5 (signing, privacy policy, AAB). A2 and A4 are done.
2. Decide the two open questions in `PLAY_CONSOLE_ANSWERS.md`: unfiltered-content
   rating (§5) and news-app declaration (§7). Recommendations are Yes and No.
3. The B1 string claim is fixed; the free-space pre-check is still open. Fix B2
   as well, since that layout defect appears in a store screenshot.
4. Build and verify the signed bundle:
   ```bash
   ./gradlew :androidApp:bundleRelease
   ```
5. Create the app in Play Console, fill in the listing from `store-listing/`,
   upload the graphics and screenshots.
6. Complete "App content" using `PLAY_CONSOLE_ANSWERS.md`.
7. Upload to **Internal testing** first and reinstall from Play on a real device
   — this is the only way to catch problems that only appear in the
   Play-signed, bundle-split artifact.
8. Closed testing, then production at a 20 % staged rollout, DE/AT/CH.

---

## Repeating the screenshots

The device flow is scripted in `scripts/capture_play_screenshots.sh`. It expects
one connected device with the app installed and a populated catalogue.
