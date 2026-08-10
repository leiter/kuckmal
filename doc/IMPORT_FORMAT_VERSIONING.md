# Import format versioning — design proposal

**Status: proposed, not implemented.** Written 2026-08-10.

Companion to [`FILM_LIST_PARSING.md`](FILM_LIST_PARSING.md), which records the bugs
that motivated this. Not urgent right now — only test releases exist, so no
install in the wild is carrying a bad catalogue. Worth having before the first
public release.

---

## The gap

Nothing records **how** the catalogue was built. No import metadata is persisted
anywhere — not in the database, not in `NSUserDefaults`/`SharedPreferences`. The
app therefore cannot distinguish a catalogue parsed by a correct pipeline from
one parsed by a broken pipeline, and it only imports when the database is empty
(`dbCount == 0` in `AppContent`).

Room migrations do not cover this. `AppDatabase` is at `version = 2`, but the
schema does not change when *parsing* changes. Every defect fixed in the week of
2026-08-09 altered stored content while leaving the schema byte-identical:

- the 64 KB chunk-drop in `IosStreamingMediaListParser` (2.6% of entries lost,
  plus channel/theme misattribution after each gap)
- `TextSanitizer` normalising upstream encoding damage
- the field mapping in `MediaEntry.fromArray`

A user on the old build who updates to the fixed build keeps the old data
forever. Nothing rebuilds it.

## Two facts about the current code that shape the design

1. **The import deletes before it parses.** `IosMediaRepository.loadMediaListFromFile`
   calls `mediaDao.deleteAll()` as Step 1, then parses and inserts. A failure
   part-way leaves the user with an empty or partial catalogue.
2. **Android has an incremental path** — `MediaRepositoryImpl.applyDiffToDatabase`.
   A format change must force a *full* rebuild, otherwise badly-parsed rows
   survive the diff.

---

## Proposal

A single-row metadata table plus a constant in shared code.

```kotlin
@Entity(tableName = "import_metadata")
data class ImportMetadata(
    @PrimaryKey val id: Int = 0,
    val dataFormatVersion: Int,
    val importedAtEpochSeconds: Long,
    val parsedEntryCount: Int,   // what the parser reported
    val storedEntryCount: Int    // what actually landed, after dedup
)
```

```kotlin
/**
 * Bump whenever a change alters what ends up STORED for a given source file.
 * A bump forces every existing install to rebuild its catalogue.
 *
 * Bump for changes to:
 *   - any parser (iOS / Android / desktop)
 *   - TextSanitizer
 *   - MediaEntry.fromArray field mapping or inheritance
 *   - the UNIQUE (channel, theme, title) index or the insert conflict strategy
 *
 * Do NOT bump for: UI changes, query changes, performance work.
 */
const val DATA_FORMAT_VERSION = 1
```

Startup check, alongside the existing `repository.getCount()` call in
`SharedViewModel.init`:

```
storedCount == 0                       -> NeedsInitialImport
meta == null || meta.version < CURRENT -> NeedsRebuild
else                                   -> UpToDate
```

`meta == null` covers installs that predate the table, which is exactly the
current test-release population.

### Store it in the database, not in preferences

The version describes the data, so it has to live and die with the data. A
restored device backup, or a cleared preferences store, would otherwise desync
the two — either skipping a rebuild that is needed, or forcing one that is not.
Keeping both in the same database makes them consistent by construction.

---

## Decisions worth making deliberately

### Prompt, do not rebuild silently

It is a ~75 MB download and minutes of processing. Doing that unannounced on
launch, possibly on cellular, is hostile. Show a dialog explaining the one-time
rebuild and reuse the existing progress UI. The strings will need German and
English (see `composeResources/values*/strings.xml`).

Allowing "later" means carrying a known-bad catalogue until the user agrees;
acceptable, but then re-prompt rather than forget.

### Fix delete-before-import first

Wiping the table and then parsing is tolerable when the *user* explicitly asks
for a reinstall. It is not tolerable for a rebuild the *app* initiated: a
failure turns "slightly wrong catalogue" into "no catalogue", for someone who
did not ask for anything.

Download and decompress to a temporary location, and swap only once the parse
has succeeded. **This should land as its own change before the hook is wired
up**, otherwise the hook makes a bad failure mode more likely by triggering it
automatically.

### Force a full rebuild, never a diff

When the version differs, bypass `applyDiffToDatabase`.

---

## The part that pays for itself

Recording `parsedEntryCount` next to `storedEntryCount` gives a cheap
self-check. The 2.6% loss was invisible precisely because the import *reported
success* — nothing compared what went in against what came out. Persisting both,
and logging when the ratio shifts sharply between imports, would have surfaced
that bug without anyone noticing a missing channel.

Expected relationship, for reference: parser count == source `"X":[` count, and
stored count == parsed count minus duplicate `(channel, theme, title)` triples
(8,158 of 707,323 on the 2026-08-09 list, ~1.2%).

---

## Non-goals

Per-entry checksums, a general migration framework, or versioning individual
fields. One integer plus a forced rebuild covers every scenario actually hit so
far. The catalogue is disposable — it is re-derivable from the source at any
time, and that property is what makes this cheap. Do not build something that
assumes otherwise.

---

## Rough order of work

1. Download-then-swap in the import path (independent value, removes the hazard).
2. `import_metadata` entity + DAO, Room schema version 3 with a migration that
   creates the table.
3. `DATA_FORMAT_VERSION` constant, written on every successful import.
4. Startup check + rebuild prompt, localized.
5. Log parsed-vs-stored divergence.

Steps 1 and 2 are independently useful and could land separately.
