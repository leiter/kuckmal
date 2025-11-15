package com.mediathekview.android.compose.models

/**
 * Sample data models for Compose screens
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

// Sample data for demo
object SampleData {
    val sampleChannels = listOf(
        Channel("3sat", "3sat"),
        Channel("ARD", "ARD"),
        Channel("arte", "arte"),
        Channel("BR", "BR"),
        Channel("hr", "hr"),
        Channel("KIKA", "KIKA"),
        Channel("mdr", "mdr"),
        Channel("NDR", "NDR"),
        Channel("ORF", "ORF"),
        Channel("phoenix", "phoenix")
    )

    val sampleTitles = listOf(
        "Von Fischern und Indianern",
        "Von Liebe und Leidenschaft",
        "Von Cowboys und Soldaten",
        "Von Millionären und Saucen",
        "Von Schlössern und Türmen"
    )

    val sampleMediaItem = MediaItem(
        channel = "phoenix",
        theme = "1000 Inseln im Sankt-Lorenz-Strom",
        title = "Von Liebe und Leidenschaft",
        date = "25.07.2019",
        time = "7:30 Uhr",
        duration = "43Min 17Sek",
        size = "747 MB",
        description = "Film von Almut Faass und Frank Gensthaler"
    )
}
