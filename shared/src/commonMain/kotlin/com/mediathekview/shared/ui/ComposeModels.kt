package com.mediathekview.shared.ui

/**
 * UI data models for Compose screens
 * Platform-independent models used across Android and Web
 */
data class MediaItem(
    val channel: String,
    val theme: String,
    val title: String,
    val date: String,
    val time: String,
    val duration: String,
    val size: String,
    val description: String
)

data class Channel(
    val name: String,
    val displayName: String
)

/**
 * Sample data for demo/preview purposes.
 * Channel names must match exactly with Broadcaster.channelListArray
 * to display correct brand colors.
 */
object SampleData {
    val sampleChannels = listOf(
        Channel("3Sat", "3sat"),
        Channel("ARD", "ARD"),
        Channel("ARTE.DE", "ARTE"),
        Channel("BR", "BR"),
        Channel("HR", "HR"),
        Channel("KiKA", "KiKA"),
        Channel("MDR", "MDR"),
        Channel("NDR", "NDR"),
        Channel("ORF", "ORF"),
        Channel("PHOENIX", "phoenix"),
        Channel("RBB", "RBB"),
        Channel("SR", "SR"),
        Channel("SRF", "SRF"),
        Channel("SWR", "SWR"),
        Channel("WDR", "WDR"),
        Channel("ZDF", "ZDF"),
        Channel("ZDF-tivi", "ZDF-tivi")
    )

    val sampleTitles = listOf(
        "Von Fischern und Indianern",
        "Von Liebe und Leidenschaft",
        "Von Cowboys und Soldaten",
        "Von Millionären und Saucen",
        "Von Schlössern und Türmen"
    )

    val sampleMediaItem = MediaItem(
        channel = "PHOENIX",
        theme = "1000 Inseln im Sankt-Lorenz-Strom",
        title = "Von Liebe und Leidenschaft",
        date = "25.07.2019",
        time = "7:30 Uhr",
        duration = "43Min 17Sek",
        size = "747 MB",
        description = "Film von Almut Faass und Frank Gensthaler"
    )
}
