package cut.the.crap.shared.ui

import cut.the.crap.shared.database.MediaEntry

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

    /**
     * Sample themes per channel for webApp mock data
     */
    val sampleThemes = mapOf(
        "ARD" to listOf("Tagesschau", "Tatort", "Sportschau", "Weltspiegel", "Hart aber fair"),
        "ZDF" to listOf("heute", "Terra X", "ZDF Magazin Royale", "Markus Lanz", "aspekte"),
        "3Sat" to listOf("Kulturzeit", "nano", "scobel", "37 Grad", "Dokumentarfilm"),
        "ARTE.DE" to listOf("Tracks", "Karambolage", "ARTE Journal", "Xenius", "Kreatur"),
        "BR" to listOf("Rundschau", "quer", "Puls", "Capriccio", "Kontrovers"),
        "NDR" to listOf("Panorama", "extra 3", "Markt", "Kulturjournal", "Nordmagazin"),
        "WDR" to listOf("Aktuelle Stunde", "Westpol", "Monitor", "Quarks", "Die Story"),
        "SWR" to listOf("Landesschau", "Marktcheck", "Nachtcafe", "Kunscht!", "Sport im Dritten"),
        "HR" to listOf("hessenschau", "defacto", "Maintower", "alles wissen", "Herkules"),
        "MDR" to listOf("MDR aktuell", "Fakt ist!", "Riverboat", "MDR Garten", "Sachsenspiegel"),
        "RBB" to listOf("Abendschau", "Kontraste", "rbb24", "Theodor", "Klartext"),
        "SR" to listOf("aktueller bericht", "Wir im Saarland", "sportarena", "Saarthema"),
        "PHOENIX" to listOf("phoenix runde", "phoenix plus", "Dokumentation", "History", "1000 Inseln im Sankt-Lorenz-Strom"),
        "KiKA" to listOf("logo!", "Wissen macht Ah!", "Checker Tobi", "Die Sendung mit der Maus"),
        "ORF" to listOf("Zeit im Bild", "Report", "Dok 1", "Kulturmontag", "Sport"),
        "SRF" to listOf("Tagesschau", "10vor10", "Arena", "DOK", "Kulturplatz"),
        "ZDF-tivi" to listOf("logo!", "PUR+", "1, 2 oder 3", "Die Biene Maja")
    )

    /**
     * Get themes for a channel, with fallback
     */
    fun getThemesForChannel(channel: String): List<String> {
        return sampleThemes[channel] ?: listOf("Allgemein", "Nachrichten", "Dokumentation", "Unterhaltung")
    }

    /**
     * Generate sample titles for a theme
     */
    fun getTitlesForTheme(theme: String): List<String> {
        return listOf(
            "$theme - Folge 1",
            "$theme - Folge 2",
            "$theme - Spezial",
            "$theme - Best of",
            "$theme vom 25.07.2024"
        )
    }

    /**
     * Generate a sample MediaItem for a given channel, theme, and title
     */
    fun createSampleMediaItem(channel: String, theme: String, title: String): MediaItem {
        return MediaItem(
            channel = channel,
            theme = theme,
            title = title,
            date = "25.07.2024",
            time = "20:15 Uhr",
            duration = "45 Min",
            size = "750 MB",
            description = "Beschreibung fuer $title im Thema $theme von $channel. " +
                "Eine spannende Sendung mit interessanten Inhalten und informativen Beitraegen."
        )
    }
}

/**
 * Extension function to convert database MediaEntry to UI MediaItem
 */
fun MediaEntry.toMediaItem(): MediaItem {
    return MediaItem(
        channel = this.channel,
        theme = this.theme,
        title = this.title,
        date = this.date,
        time = this.time,
        duration = this.duration,
        size = "${this.sizeMB} MB",
        description = this.description
    )
}
