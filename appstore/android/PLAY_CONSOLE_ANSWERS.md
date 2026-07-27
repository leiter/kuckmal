# Play Console – prepared answers

Every declaration Google asks for when creating the app and completing the
"App content" section, with the answer to give and the reason behind it.

Two answers marked **DECIDE** are judgement calls that need your input before
you submit — they are explained inline.

---

## 1. Create app

| Field | Answer |
|---|---|
| App name | `Kuckmal – Mediatheken` (max. 30 chars, 21 used) |
| Default language | German – Germany (de-DE) |
| App or game | **App** |
| Free or paid | **Free** (cannot be changed to paid later) |
| Declarations | Tick "Developer Program Policies" and "US export laws" |

Package name is already fixed by the build: `cut.the.crap.kuckmal`. It can never
be changed after the first upload.

---

## 2. Store listing

| Field | Source file |
|---|---|
| App name | `store-listing/<locale>/app-name.txt` |
| Short description | `store-listing/<locale>/short-description.txt` |
| Full description | `store-listing/<locale>/full-description.txt` |
| App icon (512×512) | `graphics/play_icon_512.png` |
| Feature graphic (1024×500) | `graphics/feature_graphic_1024x500.png` |
| Phone screenshots | `screenshots/phone/de-DE/*.png` (5 files, 1080×1920) |
| TV banner (1280×720) | `graphics/tv_banner_1280x720.png` — only if TV is opted into |

**App category:** Entertainment
**Tags:** Video players & editors / Streaming / TV
**Contact details:** email is mandatory; website and phone are optional.
**Privacy policy URL:** mandatory — see §9.

---

## 3. App access

> Is all app functionality available without special access?

**Answer: "All functionality is available without special access."**

There is no login, no account, no region lock enforced by the app and no paid
tier. Nothing has to be provided to the review team.

---

## 4. Ads

> Does your app contain ads?

**Answer: No.**

No ad SDK is linked, no ad network is contacted, and no promotional content of
any kind is served. Verified against the dependency list in
`androidApp/build.gradle`.

---

## 5. Content rating (IARC questionnaire)

Email address: your developer contact address.
Category: **"All other app types"** (this is not a game).

| Question | Answer | Reason |
|---|---|---|
| Violence – does the app contain violence? | **No** | The app itself contains no violent content. |
| Sexuality / nudity | **No** | |
| Language – profanity? | **No** | |
| Controlled substances | **No** | |
| Gambling / simulated gambling | **No** | |
| In-app purchases | **No** | |
| Shares user location | **No** | |
| Allows users to interact / communicate | **No** | No comments, chat, sharing or profiles. |
| Shares user-provided personal information with third parties | **No** | |
| Does the app allow users to purchase digital goods? | **No** | |
| **Does the app provide access to unfiltered internet content?** | **DECIDE — recommended: Yes** | See below. |

### DECIDE #1 — "unfiltered internet content"

Kuckmal streams third-party video from broadcaster servers that Kuckmal does not
moderate. That catalogue does contain material rated for adolescents and adults:
crime drama (*Tatort*), news reports with real footage of war and disaster,
documentaries about drugs and crime.

- Answering **Yes** is the honest reading and typically produces a rating around
  **USK 16 / PEGI 16 / ESRB Teen**. That is a mild cost — the app still appears
  in normal search — and it is safe.
- Answering **No** and later being found to give access to unrated adult-adjacent
  content is a policy violation that can get the app removed. IARC ratings are
  audited.

**Recommendation: answer Yes.** Do not try to optimise the rating downwards on
a catalogue you do not control.

---

## 6. Target audience and content

| Question | Answer |
|---|---|
| Target age groups | **18 and over** (optionally also 16–17) |
| Do not select any age group under 13 | — |
| Appeals to children? | **No** |
| Are children a target audience? | **No** |

Even though the catalogue includes KiKA and ZDFtivi, do **not** declare children
as a target audience. Doing so pulls the app into the **Families policy**, which
requires a certified ad SDK stack, a stricter content rating, verified privacy
disclosures and a separate review. The app has no parental controls or content
filtering, so it cannot meet that bar today.

If you want to serve children later, that is a separate project: add content
filtering and parental controls first, then change this declaration.

---

## 7. Other declarations

| Declaration | Answer | Reason |
|---|---|---|
| News app | **DECIDE — recommended: No** | See below. |
| COVID-19 contact tracing / status app | **No** | |
| Data safety | See §8 | |
| Government app | **No** | Kuckmal is independent; the *broadcasters* are public bodies, the app is not. |
| Financial features | **No** | |
| Health apps | **No** | |
| Advertising ID | **No** — the app does not use it | Do not add the `AD_ID` permission. |

### DECIDE #2 — news app declaration

The catalogue includes *Tagesschau*, *heute*, *Panorama* and other news
programmes, but Kuckmal does not produce, edit or curate journalism — it is an
index over broadcaster catalogues, and news is one category among many.

**Recommendation: answer No.** Declaring "yes" invites the news-publisher
verification flow (proof of editorial ownership, imprint, journalist details),
which you cannot satisfy for content produced by ARD and ZDF.

If you are challenged on this, the defence is in the listing text: the app is
described as an aggregator, and the disclaimer states it is unaffiliated.

---

## 8. Data safety form

The honest answer is **not** "we collect nothing" — one feature transmits data to
a third party. Read §8.2 before filling this in.

### 8.1 Summary answers

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data encrypted in transit? | **No** — see below |
| Do you provide a way for users to request that their data is deleted? | **No** (nothing is stored off-device) |
| Has your data collection been independently validated? | No (optional) |

**"Collect"** in Play's definition means transmitting data off the device *and
retaining it*. Kuckmal stores nothing server-side: no account, no profile, no
analytics, no crash reporting SDK, no advertising ID. Favourites, history and
the media index live only in app-private storage.

### 8.2 The one thing that is not "nothing" — read this

`shared/src/commonMain/kotlin/cut/the/crap/shared/util/GeoDetector.kt` calls:

```
http://ip-api.com/json/?fields=status,countryCode
```

This runs from the detail view to decide whether to warn about a geo-restricted
item. Two consequences:

1. **The user's IP address reaches a third party (ip-api.com).** Under Play's
   definition this is not "collection" *by you* — you neither receive nor retain
   it — so "No collection" remains defensible. But it flatly contradicts the
   sentence in the current privacy policy that says *"This data never leaves your
   device and is not transmitted to any servers."* That sentence must be fixed
   regardless of which option you pick below.
2. **The request is plain HTTP.** So you must answer **No** to "all user data is
   encrypted in transit", and this is why `android:usesCleartextTraffic="true"`
   is in the manifest.

**Recommended fix (do this before submitting), in order of preference:**

- **Best:** delete the ip-api.com call. Get the country from the device locale /
  SIM instead — no network request, no third party, no cleartext, and you can
  then also remove `usesCleartextTraffic` if no broadcaster URL needs it. The
  geo warnings keep working.
- **Acceptable:** switch to `https://` (ip-api.com's HTTPS endpoint requires a
  paid key; `ipapi.co` or Cloudflare's `1.1.1.1/cdn-cgi/trace` are free HTTPS
  alternatives), then answer **Yes** to encryption in transit, and disclose the
  third-party call in the privacy policy.
- **Minimum:** leave it, answer **No** to encryption in transit, and disclose it
  in the privacy policy.

Do not submit with the privacy policy as currently written — a policy that
contradicts observable app behaviour is a rejection reason on its own.

### 8.3 Also worth checking

Video streaming and downloads go directly to broadcaster servers, which see the
user's IP by necessity. That is inherent to playback and does not need to be
declared as collection, but the privacy policy should say it (the current one
already does, under "Third-Party Content").

---

## 9. Privacy policy and support URLs — blocking

A reachable privacy policy URL is **mandatory** to publish. The text exists at
`appstore/privacy-policy-de.md` and `appstore/privacy-policy-en.md`, but it is
not hosted and still contains `[YOUR_EMAIL]` placeholders.

Before submitting:

1. Fix the "never leaves your device" claim per §8.2, and add the third-party
   call if you keep it.
2. Replace every `[YOUR_EMAIL]` in the privacy policies, `review-notes.txt` and
   `support-page.md`.
3. Host both pages at stable public URLs (GitHub Pages is sufficient) and put the
   German URL into the de-DE listing, the English one into en-US.

---

## 10. Countries and rollout

Recommended for the first release:

- **Countries:** Germany, Austria, Switzerland. Most of the catalogue is
  geo-restricted to the DACH region, so a worldwide launch mainly produces
  one-star reviews from users who cannot play anything.
- **Track:** start on **Internal testing**, then **Closed testing**, and only
  then Production. First-time review of a new developer account takes days and
  Google may require a closed test with real testers before granting production
  access (this applies to personal accounts created after Nov 2023).
- **Staged rollout:** start production at 20 %.

---

## 11. Android TV — DECIDE

`androidApp/src/main/AndroidManifest.xml` declares
`android.intent.category.LEANBACK_LAUNCHER` and a TV banner, so Play will offer
the Android TV form factor.

**Recommendation for v1: do not opt into the TV form factor.** TV is a separate
review with its own requirements (D-pad-only navigation for every screen, no
touch-only affordances, TV screenshots, banner). The Compose UI has not been
verified against those rules in this pass. Ship phone first, then submit TV as a
follow-up — the manifest can stay as it is; not opting in simply means Play does
not list the app on TV.

A `tv_banner_1280x720.png` is generated and ready for when you do opt in.
