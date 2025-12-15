package com.mediathekview.shared.model

/**
 * Represents a broadcast channel (TV station) with its display name,
 * brand color, and short abbreviation.
 * Platform-agnostic version without Android resource dependencies.
 */
data class Broadcaster(
    val name: String,
    val brandColor: Long = 0xFF808080,  // Default gray fallback
    val abbreviation: String = ""        // Short display text for fallback
) {
    companion object {
        /**
         * Static list of all available broadcast channels with brand colors.
         */
        val channelList = listOf(
            Broadcaster("3Sat", 0xFFE3051B, "3sat"),      // Red
            Broadcaster("ARD", 0xFF003480, "ARD"),        // Blue
            Broadcaster("ARTE.DE", 0xFFFA481C, "ARTE"),   // Orange-red
            Broadcaster("ARTE.FR", 0xFFFA481C, "ARTE"),   // Orange-red
            Broadcaster("BR", 0xFF0062A1, "BR"),          // Blue
            Broadcaster("HR", 0xFF004A99, "HR"),          // Blue
            Broadcaster("KiKA", 0xFF7AB51D, "KiKA"),      // Green
            Broadcaster("MDR", 0xFF00A8E6, "MDR"),        // Cyan
            Broadcaster("NDR", 0xFF003D7A, "NDR"),        // Dark Blue
            Broadcaster("ORF", 0xFFD40000, "ORF"),        // Red
            Broadcaster("PHOENIX", 0xFF00687C, "PHX"),    // Cyan
            Broadcaster("RBB", 0xFFE4003A, "RBB"),        // Red
            Broadcaster("SR", 0xFF0053A3, "SR"),          // Blue
            Broadcaster("SRF", 0xFFD40000, "SRF"),        // Red
            Broadcaster("SRF.Podcast", 0xFFD40000, "SRF"),// Red
            Broadcaster("SWR", 0xFF003F87, "SWR"),        // Blue
            Broadcaster("WDR", 0xFF00447A, "WDR"),        // Blue
            Broadcaster("ZDF", 0xFFFA7D19, "ZDF"),        // Orange
            Broadcaster("ZDF-tivi", 0xFFFA7D19, "tivi")   // Orange
        )

        fun getChannelName(pos: Int): String = channelList[pos].name

        fun getBrandColorOfName(name: String): Long =
            channelList.find { it.name == name }?.brandColor ?: 0xFF808080

        fun getBrandColor(pos: Int): Long = channelList[pos].brandColor

        fun getAbbreviationOfName(name: String): String =
            channelList.find { it.name == name }?.abbreviation?.ifEmpty { name } ?: name

        fun getAbbreviation(pos: Int): String =
            channelList[pos].abbreviation.ifEmpty { channelList[pos].name }

        fun getByName(name: String): Broadcaster? =
            channelList.find { it.name == name }

        fun getChannelPosition(name: String): Int =
            channelList.indexOfFirst { it.name == name }

        /**
         * Alias for backward compatibility with Android module
         */
        val channelListArray: List<Broadcaster>
            get() = channelList
    }
}
