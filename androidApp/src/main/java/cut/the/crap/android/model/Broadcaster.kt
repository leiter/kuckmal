package cut.the.crap.android.model

import cut.the.crap.android.R

/**
 * Represents a broadcast channel (TV station) with its display name, icon resource ID,
 * brand color (for fallback display), and short abbreviation.
 *
 * The brand color is used as a fallback when the logo is not available,
 * displaying the abbreviation on a colored background (similar to MediathekViewWeb).
 *
 * This is a data class which automatically generates:
 * - equals() and hashCode()
 * - toString()
 * - copy() function
 * - componentN() functions for destructuring
 *
 * Brand color sources:
 * - ZDF: https://brandguide.zdf.de/channels/zdf/basiselemente/farben
 * - ARD: Official brand guidelines (Pantone 7686)
 * - 3Sat: https://brandguide.zdf.de/3sat/basiselemente/logo
 * - ARTE: https://brandguid.es/artere/
 * - Phoenix: https://logotyp.us/logo/phoenix/
 * - Others: Derived from official logos and brand materials
 */
data class Broadcaster(
    val name: String,
    val iconRes: Int,
    val brandColor: Long = 0xFF808080,  // Default gray fallback
    val abbreviation: String = ""        // Short display text for fallback
) {
    companion object {
        /**
         * Global flag to control channel display mode.
         * - false (default): Use logo icons (for legacy View-based UI)
         * - true: Use fallback display with brand color + abbreviation (MediathekViewWeb style)
         *
         * Set this to true in Compose UI to use the fallback display.
         */
        @JvmField
        var useFallbackDisplay: Boolean = false

        /**
         * Static list of all available broadcast channels with brand colors.
         * Using listOf() for immutable list instead of ArrayList with double-brace initialization.
         *
         * Brand colors are official where available, otherwise derived from logo colors.
         */
        @JvmField
        val channelListArray = listOf(
            Broadcaster("3Sat", R.drawable._3sat, 0xFFE3051B, "3sat"),      // Red
            Broadcaster("ARD", R.drawable.ard, 0xFF003480, "ARD"),          // Blue (Pantone 7686)
            Broadcaster("ARTE.DE", R.drawable.arte_de, 0xFFFA481C, "ARTE"), // Orange-red
            Broadcaster("ARTE.FR", R.drawable.arte_fr, 0xFFFA481C, "ARTE"), // Orange-red
            Broadcaster("BR", R.drawable.br, 0xFF0062A1, "BR"),             // Blue (Bavarian)
            Broadcaster("HR", R.drawable.hr, 0xFF004A99, "HR"),             // Blue
            Broadcaster("KiKA", R.drawable.kika, 0xFF7AB51D, "KiKA"),       // Green
            Broadcaster("MDR", R.drawable.mdr, 0xFF00A8E6, "MDR"),          // Cyan/Blue
            Broadcaster("NDR", R.drawable.ndr, 0xFF003D7A, "NDR"),          // Dark Blue
            Broadcaster("ORF", R.drawable.orf, 0xFFD40000, "ORF"),          // Red (Austrian)
            Broadcaster("PHOENIX", R.drawable.phoenix, 0xFF00687C, "PHX"),  // Cyan (official since 2018)
            Broadcaster("RBB", R.drawable.rbb, 0xFFE4003A, "RBB"),          // Red
            Broadcaster("SR", R.drawable.sr, 0xFF0053A3, "SR"),             // Blue
            Broadcaster("SRF", R.drawable.srf, 0xFFD40000, "SRF"),          // Red (Swiss)
            Broadcaster("SRF.Podcast", R.drawable.srf_podcast, 0xFFD40000, "SRF"), // Red (Swiss)
            Broadcaster("SWR", R.drawable.swr, 0xFF003F87, "SWR"),          // Blue
            Broadcaster("WDR", R.drawable.wdr, 0xFF00447A, "WDR"),          // Blue
            Broadcaster("ZDF", R.drawable.zdf, 0xFFFA7D19, "ZDF"),          // Orange (official)
            Broadcaster("ZDF-tivi", R.drawable.zdf_tivi, 0xFFFA7D19, "tivi") // Orange (ZDF family)
        )

        /**
         * Get channel icon resource ID by position.
         * @JvmStatic makes this accessible from Java as a static method.
         */
        @JvmStatic
        fun getChannelIcon(pos: Int): Int = channelListArray[pos].iconRes

        /**
         * Get channel name by position.
         */
        @JvmStatic
        fun getChannelName(pos: Int): String = channelListArray[pos].name

        /**
         * Find icon resource ID by channel name.
         * Using Kotlin's find() extension function instead of manual loop.
         * Returns 0 if not found (0 is not a valid resource ID).
         */
        @JvmStatic
        fun getIconOfName(name: String): Int =
            channelListArray.find { it.name == name }?.iconRes ?: 0

        /**
         * Find channel position by name.
         * Returns -1 if not found.
         */
        @JvmStatic
        fun getChannelPosition(name: String): Int =
            channelListArray.indexOfFirst { it.name == name }

        /**
         * Get brand color by channel name.
         * Returns default gray (0xFF808080) if not found.
         */
        @JvmStatic
        fun getBrandColorOfName(name: String): Long =
            channelListArray.find { it.name == name }?.brandColor ?: 0xFF808080

        /**
         * Get brand color by position.
         */
        @JvmStatic
        fun getBrandColor(pos: Int): Long = channelListArray[pos].brandColor

        /**
         * Get abbreviation by channel name.
         * Returns the name itself if not found or abbreviation is empty.
         */
        @JvmStatic
        fun getAbbreviationOfName(name: String): String =
            channelListArray.find { it.name == name }?.abbreviation?.ifEmpty { name } ?: name

        /**
         * Get abbreviation by position.
         */
        @JvmStatic
        fun getAbbreviation(pos: Int): String =
            channelListArray[pos].abbreviation.ifEmpty { channelListArray[pos].name }

        /**
         * Get the full Broadcaster object by name.
         * Returns null if not found.
         */
        @JvmStatic
        fun getByName(name: String): Broadcaster? =
            channelListArray.find { it.name == name }
    }
}
