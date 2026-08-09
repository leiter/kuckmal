package cut.the.crap.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * The damaged strings below are the actual ones found in the published
 * MediathekView film list (August 2026), not synthetic examples.
 */
class TextSanitizerTest {

    @Test
    fun collapsesDoubleEncodedReplacementCharacter() {
        // Theme of an ARD entry whose sibling field still reads "Alfons und Gäste".
        assertEquals("Alfons und G�ste", TextSanitizer.repair("Alfons und Gï¿½ste"))
    }

    @Test
    fun collapsesMangledThreeByteSequence() {
        // funk entry: the title field still reads "Don’t say it - bring it!".
        assertEquals(
            "Don�t say it � bring it!",
            TextSanitizer.repair("Don�??t say it �?? bring it!")
        )
    }

    @Test
    fun collapsesRunsOfReplacementCharacters() {
        assertEquals("a�b", TextSanitizer.repair("a���b"))
    }

    @Test
    fun leavesTrailingSingleReplacementCharacterAlone() {
        // A truncated funk description; one lost character stays one lost character.
        assertEquals(
            "und sich extra die affigsten �",
            TextSanitizer.repair("und sich extra die affigsten �")
        )
    }

    @Test
    fun returnsSameInstanceForCleanText() {
        // Import runs this over every field of ~700k entries; the clean path must not allocate.
        val clean = "Alfons und Gäste – Sven Ratzke"
        assertSame(clean, TextSanitizer.repair(clean))
    }

    @Test
    fun leavesLegitimateUmlautsAndDashesAlone() {
        val text = "Tatort: Die Kälte der Erde – Größe, Gebärdensprache, Österreich"
        assertSame(text, TextSanitizer.repair(text))
    }

    @Test
    fun leavesLegitimateIWithDiaeresisAlone() {
        // "ï" alone triggers the fast-path check but must not be altered.
        val text = "Caïn – Naïve Anaïs"
        assertSame(text, TextSanitizer.repair(text))
    }

    @Test
    fun handlesEmptyString() {
        assertSame("", TextSanitizer.repair(""))
    }
}
