package cut.the.crap.web

/**
 * Broadcaster/Channel model for webApp
 * Contains channel branding information
 */
data class Broadcaster(
    val name: String,
    val brandColor: Long = 0xFF808080,
    val abbreviation: String = ""
) {
    companion object {
        val channelList = listOf(
            Broadcaster("3Sat", 0xFFE3051B, "3sat"),
            Broadcaster("ARD", 0xFF003480, "ARD"),
            Broadcaster("ARTE.DE", 0xFFFA481C, "ARTE"),
            Broadcaster("ARTE.FR", 0xFFFA481C, "ARTE"),
            Broadcaster("BR", 0xFF0062A1, "BR"),
            Broadcaster("HR", 0xFF004A99, "HR"),
            Broadcaster("KiKA", 0xFF7AB51D, "KiKA"),
            Broadcaster("MDR", 0xFF00A8E6, "MDR"),
            Broadcaster("NDR", 0xFF003D7A, "NDR"),
            Broadcaster("ORF", 0xFFD40000, "ORF"),
            Broadcaster("PHOENIX", 0xFF00687C, "PHX"),
            Broadcaster("RBB", 0xFFE4003A, "RBB"),
            Broadcaster("SR", 0xFF0053A3, "SR"),
            Broadcaster("SRF", 0xFFD40000, "SRF"),
            Broadcaster("SRF.Podcast", 0xFFD40000, "SRF"),
            Broadcaster("SWR", 0xFF003F87, "SWR"),
            Broadcaster("WDR", 0xFF00447A, "WDR"),
            Broadcaster("ZDF", 0xFFFA7D19, "ZDF"),
            Broadcaster("ZDF-tivi", 0xFFFA7D19, "tivi")
        )

        fun getBrandColorOfName(name: String): Long =
            channelList.find { it.name == name }?.brandColor ?: 0xFF808080

        fun getAbbreviationOfName(name: String): String =
            channelList.find { it.name == name }?.abbreviation?.ifEmpty { name } ?: name
    }
}

/**
 * Media item for UI display
 */
data class MediaItem(
    val channel: String,
    val theme: String,
    val title: String,
    val date: String,
    val time: String,
    val duration: String,
    val size: String,
    val description: String,
    val url: String = "",
    val hdUrl: String = ""
)

/**
 * Sample data for development/testing
 * Will be replaced with API calls later
 */
object SampleData {

    private val sampleThemes = mapOf(
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
        "PHOENIX" to listOf("phoenix runde", "phoenix plus", "Dokumentation", "History", "Zeitgeschichte"),
        "KiKA" to listOf("logo!", "Wissen macht Ah!", "Checker Tobi", "Die Sendung mit der Maus"),
        "ORF" to listOf("Zeit im Bild", "Report", "Dok 1", "Kulturmontag", "Sport"),
        "SRF" to listOf("Tagesschau", "10vor10", "Arena", "DOK", "Kulturplatz"),
        "ZDF-tivi" to listOf("logo!", "PUR+", "1, 2 oder 3", "Die Biene Maja")
    )

    fun getThemesForChannel(channel: String): List<String> {
        return sampleThemes[channel] ?: listOf("Allgemein", "Nachrichten", "Dokumentation", "Unterhaltung")
    }

    fun getTitlesForTheme(theme: String): List<String> {
        return listOf(
            "$theme - Folge 1",
            "$theme - Folge 2",
            "$theme - Spezial",
            "$theme - Best of",
            "$theme vom 25.07.2024"
        )
    }

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
                "Eine spannende Sendung mit interessanten Inhalten und informativen Beitraegen.",
            url = "https://example.com/video.mp4"
        )
    }
}
