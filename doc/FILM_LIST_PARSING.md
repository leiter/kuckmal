# Film list parsing — findings

*Investigated 2026-08-09, against the MediathekView list of that date
(`Filmliste-akt.json`, 465,370,723 bytes decompressed, 707,323 entries).*

Written up after an iOS report of "no entries for 3sat". The 3sat symptom could
not be reproduced on current `main`, but the investigation turned up two real
defects that were fixed, and one structural gap that is still open.

---

## 1. There is no shared parser

Four implementations, three of them live. They share no parsing logic.

| Parser | Path | Used by | Lines |
|---|---|---|---|
| `IosStreamingMediaListParser` | `shared/src/iosMain/.../data/` | iOS | 334 |
| `MediaListParser` | `androidApp/.../android/data/` | Android | 515 |
| `DesktopMediaListParser` | `desktopApp/.../desktop/data/` | Desktop | 146 |
| `MediaListParser` | `shared/src/commonMain/.../data/` | **nothing — dead code** | 381 |

Consequences:

- A parsing bug can exist on one platform only. The bug in §2 was iOS-only.
- Fixes must be applied three times, or they diverge further.
- The commonMain parser is referenced by no production or test code. It looks
  like an abandoned attempt at unification. It should be deleted or finished —
  leaving it invites someone to assume parsing is shared when it is not.

Only the iOS parser was audited in this pass. **The Android and desktop parsers
have not been checked for the same class of bug**, and the desktop one at 146
lines is doing the same job in a third of the code, which is worth a look.

---

## 2. iOS parser dropped 2.6% of the catalogue (fixed, `07b03b5`)

### What was wrong

`parseFileWithCallback` read the file in 64 KB chunks and decoded each chunk
**in isolation**:

```kotlin
val nsString = NSString.create(nsData, NSUTF8StringEncoding)
if (nsString == null) { ...; continue }   // <- discards the whole 64 KB
```

A read boundary lands at an arbitrary byte offset and routinely splits a
multi-byte UTF-8 character — every umlaut is two bytes, and German broadcaster
metadata is dense with them. `NSString.create` returns `null` for such a chunk,
and the old code then skipped **all ~100 entries in it**.

Separately, an entry that began near the end of one read and finished in the
next was lost too: nothing carried the partial entry across reads.

### Measured impact

Same file, same device, before and after:

| | entries parsed |
|---|---|
| before | 688,577 of 707,323 |
| after | **707,323 of 707,323** |

**18,746 entries — 2.6% of the catalogue — were silently missing.** No error was
logged; the import reported success.

### The part that matters more than the count

The film list omits `channel` and `theme` when they repeat, and
`MediaEntry.fromArray` inherits them from the previous entry:

```kotlin
channel = getOrInherit(0, previous?.channel ?: ""),
theme   = TextSanitizer.repair(getOrInherit(1, previous?.theme ?: "")),
```

(the `TextSanitizer.repair` wrapper is from §3 and unrelated to the inheritance)

So a dropped chunk did not merely lose entries — the entries *after* the gap
inherited the channel and theme from *before* it, **misattributing them to the
wrong broadcaster**. That produces wrong data rather than missing data, and it
is the most plausible mechanism behind a channel or theme appearing empty or
scrambled.

### The fix

The parser now carries two things across reads:

- `pendingBytes` — the trailing incomplete UTF-8 sequence, found by
  `completeUtf8Length()` scanning back at most 4 bytes for a lead byte.
- `pendingText` — the decoded text after the last fully parsed entry, so an
  entry spanning two reads is parsed once, whole.

`parseFileStreaming` was deleted: unused, and it carried the identical decode
bug via `buffer.decodeToString(0, bytesRead)`, so adopting it later would have
reintroduced the data loss.

### Why the database shows fewer rows than the parser reports

Parser reports 707,323; `media_entries` settles at **699,165**. The difference
of 8,158 is **not** loss — `index_media_entries_channel_theme_title` is UNIQUE
on `(channel, theme, title)` and the DAO inserts with `OnConflictStrategy.REPLACE`,
so genuine duplicates collapse. This is existing intended behaviour.

---

## 3. The mojibake is upstream, not ours (mitigated, `07b03b5`)

The published film list contains **10 damaged spots in 465 MB**. The downloaded
file is valid UTF-8 end to end, and the damaged text sits beside correct text
*inside the same entry* — the funk entry reads `Don’t say it` in one field and
`Don�??t say it` in another. A parser bug cannot corrupt one field and not its
neighbour, so this was published damaged.

Two forms:

| Form | Bytes | Count | Example |
|---|---|---|---|
| Re-encoded U+FFFD | `C3 AF C2 BF C2 BD` → `ï¿½` | 1 | `Alfons und Gï¿½ste` |
| Mangled 3-byte char | `EF BF BD 3F 3F` → `�??` | 9 | `Don�??t say it` |

Both stand for exactly one character destroyed before publication. **The original
letter is unrecoverable** — there is no information left to recover it from.

`TextSanitizer` (commonMain, so all platforms) collapses each damaged run to a
single U+FFFD, applied to `theme`, `title` and `description` in
`MediaEntry.fromArray`. `Alfons und Gï¿½ste` now renders `Alfons und G�ste` —
one unknown character instead of three garbage ones.

Deliberately **not** done: guessing the lost character from the neighbouring
correct field or from a similar theme name elsewhere in the list. That would
invent text that was never published, and it would look like data.

URLs are left untouched: a link containing a destroyed character is already dead
and rewriting it cannot make it resolve.

Eight tests in `TextSanitizerTest` use the actual damaged strings from the list,
not synthetic examples.

---

## 4. The 3sat report could not be reproduced

Tested on current `main` against a freshly imported database:

| Interpretation | Result |
|---|---|
| 3sat as **channel** | Works — `Themen (3Sat)` lists …von oben, 37 Grad, 3sat-Kulturdoku, 3satFestival … |
| `3sat` as **theme** (a theme with that literal name exists) | Works — lists Australien-Saga, Die Macht der Elemente, Persona … |

Counts cross-checked against an independent reference parse of the source file
(Python, `json.raw_decode`, replicating the field inheritance):

| | 3Sat entries |
|---|---|
| reference parse of source | 19,516 |
| app database | 19,497 (19 duplicates collapsed by the UNIQUE index) |

Note the channel is stored as **`3Sat`** (capital S) and displayed as `3sat`.
That mapping is correct — `Broadcaster("3Sat", …, "3sat")` and the UI passes
`Channel.name`, not `displayName`, into the query — so it is not the cause.

**Most likely explanation for the original report: a database imported by the
pre-fix parser.** See §5.

---

## 5. OPEN: the fix does not reach existing installs

**The app never re-imports on update.** `AppContent` shows the download screen
only when `dbCount == 0`. An install whose catalogue was built by the old parser
keeps that data — missing and misattributed entries included — indefinitely,
through any number of app updates.

Workarounds available to a user today:

- menu → *Filmliste neu installieren (Daten löschen)*
- delete and reinstall the app

Neither is discoverable for someone who does not already know their data is bad.

**Recommendation:** store an import-format version alongside the data and force a
rebuild when it changes. The Room schema version does not serve this purpose —
the schema did not change here, only the code that fills it. Without this, the
parser fix effectively only applies to new installs.

This matters for the App Store release: anyone who installed a TestFlight or
development build before `07b03b5` is carrying a corrupted catalogue.

---

## How to re-verify

Count entries in the source, independent of app code:

```bash
python3 - "$F" <<'PY'
import sys; p=sys.argv[1]; n=0; carry=b''
with open(p,'rb') as f:
    while True:
        d=f.read(8<<20)
        if not d: break
        buf=carry+d; n+=buf.count(b'"X":['); carry=buf[-4:]
print(n)
PY
```

Compare against the parser's own tally and the database:

```bash
grep "PARSE COMPLETE" "$C/Documents/"*.log | tail -1
sqlite3 "$C/Documents/kuckmal.db" "SELECT COUNT(*) FROM media_entries;"
sqlite3 "$C/Documents/kuckmal.db" \
  "SELECT channel, COUNT(*) FROM media_entries GROUP BY channel ORDER BY 2 DESC;"
```

where `C=$(xcrun simctl get_app_container <udid> cut.the.crap.kuckmal data)`.

Expect: source count == parser count, and database == parser minus duplicate
`(channel, theme, title)` triples.

---

## Related commits

| Commit | Contents |
|---|---|
| `07b03b5` | Parser carry-over fix, `TextSanitizer`, `parseFileStreaming` removed |
| `40e89db` | German localization of the iOS first-run flow, status-bar inset fix, Xcode JDK resolution |
| `3b43c48` | Made the `SharedViewModel` suite deterministic (unrelated to parsing, found while verifying) |
