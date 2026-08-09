package cut.the.crap.shared.util

/**
 * Repairs encoding damage that is already present in the published MediathekView
 * film list. This is upstream corruption, not something the app introduces: the
 * downloaded file is valid UTF-8 end to end, and the damaged text sits next to
 * correct text inside the same entry.
 *
 * As of the August 2026 list there are ten damaged spots in ~465 MB, in two forms:
 *
 *  1. `ï¿½`  — U+FFFD that was itself re-encoded (its bytes EF BF BD read as
 *             Latin-1 and written back out as UTF-8). Renders as three garbage
 *             characters, e.g. "Alfons und Gï¿½ste".
 *  2. `�??` — a three-byte character whose lead byte became U+FFFD and whose
 *             two continuation bytes became "?", e.g. "Don�??t say it" where
 *             the same entry's title field still reads "Don’t say it".
 *
 * Both forms stand for exactly one character whose identity was destroyed before
 * the list was published, so the original letter cannot be recovered — there is
 * no information left to recover it from. What this does is collapse each damaged
 * run to a single U+FFFD, which is the standard way to render one unknown
 * character: "Alfons und G�ste" rather than "Alfons und Gï¿½ste".
 *
 * Deliberately not attempted: guessing the lost character from context (the
 * neighbouring correct field, or a similar theme name elsewhere in the list).
 * That would silently invent text that was never published.
 */
object TextSanitizer {

    private const val REPLACEMENT = '�'

    /** U+FFFD's UTF-8 bytes decoded as Latin-1: EF BF BD -> "ï¿½". */
    private const val DOUBLE_ENCODED = "ï¿½"

    /** A destroyed three-byte sequence: U+FFFD followed by its two mangled continuation bytes. */
    private const val MANGLED_THREE_BYTE = "�??"

    /**
     * Returns [value] with damaged encoding runs collapsed to a single U+FFFD.
     *
     * Returns the original instance when there is nothing to repair, which is the
     * overwhelmingly common case — this runs on every field of every one of
     * ~700,000 entries during import, so the clean path allocates nothing.
     */
    fun repair(value: String): String {
        if (value.isEmpty()) return value
        if (!value.contains(REPLACEMENT) && !value.contains('ï')) return value

        var result = value
        if (result.contains(DOUBLE_ENCODED)) {
            result = result.replace(DOUBLE_ENCODED, REPLACEMENT.toString())
        }
        if (result.contains(MANGLED_THREE_BYTE)) {
            result = result.replace(MANGLED_THREE_BYTE, REPLACEMENT.toString())
        }
        // Collapse runs left over from adjacent damage, e.g. "��" -> "�".
        while (result.contains("$REPLACEMENT$REPLACEMENT")) {
            result = result.replace("$REPLACEMENT$REPLACEMENT", REPLACEMENT.toString())
        }
        return result
    }
}
