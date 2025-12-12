package com.mediathekview.web.repository

import com.mediathekview.web.Broadcaster
import com.mediathekview.web.MediaItem
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mock implementation of MediaRepository for development/testing.
 * Generates 500 mock entries and simulates API latency.
 */
class MockMediaRepository : MediaRepository {

    private val allEntries: List<MediaItem> by lazy { generateMockData() }

    private val simulatedLatencyMs: LongRange = 200L..500L

    private suspend fun simulateNetworkDelay() {
        delay(Random.nextLong(simulatedLatencyMs.first, simulatedLatencyMs.last))
    }

    override suspend fun getChannels(): List<Broadcaster> {
        simulateNetworkDelay()
        return Broadcaster.channelList
    }

    override suspend fun getThemes(
        channel: String?,
        page: Int,
        pageSize: Int
    ): PagedResult<String> {
        simulateNetworkDelay()

        val themes = if (channel != null) {
            allEntries.filter { it.channel == channel }
                .map { it.theme }
                .distinct()
                .sorted()
        } else {
            allEntries.map { it.theme }
                .distinct()
                .sorted()
        }

        return paginate(themes, page, pageSize)
    }

    override suspend fun getTitles(
        channel: String,
        theme: String,
        page: Int,
        pageSize: Int
    ): PagedResult<MediaItem> {
        simulateNetworkDelay()

        val items = allEntries.filter {
            it.channel == channel && it.theme == theme
        }

        return paginate(items, page, pageSize)
    }

    override suspend fun getMediaItem(
        channel: String,
        theme: String,
        title: String
    ): MediaItem? {
        simulateNetworkDelay()

        return allEntries.find {
            it.channel == channel && it.theme == theme && it.title == title
        }
    }

    override suspend fun search(
        query: String,
        channel: String?,
        page: Int,
        pageSize: Int
    ): PagedResult<MediaItem> {
        simulateNetworkDelay()

        val queryLower = query.lowercase()
        val items = allEntries.filter { item ->
            val matchesQuery = item.title.lowercase().contains(queryLower) ||
                    item.theme.lowercase().contains(queryLower) ||
                    item.description.lowercase().contains(queryLower)

            val matchesChannel = channel == null || item.channel == channel

            matchesQuery && matchesChannel
        }

        return paginate(items, page, pageSize)
    }

    override suspend fun getFilteredItems(
        dateFilter: DateFilter,
        durationFilter: DurationFilter,
        channel: String?,
        page: Int,
        pageSize: Int
    ): PagedResult<MediaItem> {
        simulateNetworkDelay()

        val items = allEntries.filter { item ->
            val matchesChannel = channel == null || item.channel == channel
            val matchesDuration = matchesDurationFilter(item, durationFilter)
            val matchesDate = matchesDateFilter(item, dateFilter)

            matchesChannel && matchesDuration && matchesDate
        }

        return paginate(items, page, pageSize)
    }

    private fun matchesDurationFilter(item: MediaItem, filter: DurationFilter): Boolean {
        if (filter == DurationFilter.ALL) return true

        val minutes = parseDurationMinutes(item.duration)
        return when (filter) {
            DurationFilter.SHORT -> minutes < 15
            DurationFilter.MEDIUM -> minutes in 15..60
            DurationFilter.LONG -> minutes > 60
            DurationFilter.ALL -> true
        }
    }

    private fun matchesDateFilter(item: MediaItem, filter: DateFilter): Boolean {
        // For mock data, we simulate date matching based on the date string
        // In real implementation, this would compare actual dates
        if (filter == DateFilter.ALL) return true

        // Mock implementation: use hash of date string to simulate distribution
        val hash = item.date.hashCode() % 30
        return when (filter) {
            DateFilter.TODAY -> hash == 0
            DateFilter.LAST_WEEK -> hash in 0..6
            DateFilter.LAST_MONTH -> hash in 0..29
            DateFilter.ALL -> true
        }
    }

    private fun parseDurationMinutes(duration: String): Int {
        // Parse duration strings like "45 Min", "1h 30min", "30 Min"
        val hourMatch = Regex("(\\d+)\\s*[hH]").find(duration)
        val minMatch = Regex("(\\d+)\\s*[mM]").find(duration)

        val hours = hourMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val minutes = minMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

        return hours * 60 + minutes
    }

    private fun <T> paginate(items: List<T>, page: Int, pageSize: Int): PagedResult<T> {
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, items.size)

        val pageItems = if (startIndex < items.size) {
            items.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PagedResult(
            items = pageItems,
            totalCount = items.size,
            page = page,
            pageSize = pageSize,
            hasMore = endIndex < items.size
        )
    }

    companion object {
        /**
         * Generate 500 mock media entries distributed across channels and themes
         */
        private fun generateMockData(): List<MediaItem> {
            val entries = mutableListOf<MediaItem>()
            val random = Random(42) // Fixed seed for reproducible data

            // Theme definitions per channel
            val channelThemes = mapOf(
                "ARD" to listOf("Tagesschau", "Tatort", "Sportschau", "Weltspiegel", "Hart aber fair", "Anne Will", "Report Mainz"),
                "ZDF" to listOf("heute", "Terra X", "ZDF Magazin Royale", "Markus Lanz", "aspekte", "ZDF spezial", "Frontal"),
                "3Sat" to listOf("Kulturzeit", "nano", "scobel", "37 Grad", "Dokumentarfilm", "makro", "HITEC"),
                "ARTE.DE" to listOf("Tracks", "Karambolage", "ARTE Journal", "Xenius", "Kreatur", "Square Idee", "Reise durch Amerika"),
                "BR" to listOf("Rundschau", "quer", "Puls", "Capriccio", "Kontrovers", "Blickpunkt Sport", "Stationen"),
                "NDR" to listOf("Panorama", "extra 3", "Markt", "Kulturjournal", "Nordmagazin", "45 Min", "DIE REPORTAGE"),
                "WDR" to listOf("Aktuelle Stunde", "Westpol", "Monitor", "Quarks", "Die Story", "Lokalzeit", "sport inside"),
                "SWR" to listOf("Landesschau", "Marktcheck", "Nachtcafe", "Kunscht!", "Sport im Dritten", "odysso", "Planet Wissen"),
                "HR" to listOf("hessenschau", "defacto", "Maintower", "alles wissen", "Herkules", "mex", "hessenreporter"),
                "MDR" to listOf("MDR aktuell", "Fakt ist!", "Riverboat", "MDR Garten", "Sachsenspiegel", "Kripo live", "Lebensretter"),
                "RBB" to listOf("Abendschau", "Kontraste", "rbb24", "Theodor", "Klartext", "Brandenburg aktuell", "zibb"),
                "SR" to listOf("aktueller bericht", "Wir im Saarland", "sportarena", "Saarthema", "SAAR3", "Fahr mal hin"),
                "PHOENIX" to listOf("phoenix runde", "phoenix plus", "Dokumentation", "History", "Zeitgeschichte", "Unter den Linden", "phoenix persoenlich"),
                "KiKA" to listOf("logo!", "Wissen macht Ah!", "Checker Tobi", "Die Sendung mit der Maus", "PUR+", "Tigerenten Club", "KiKA LIVE"),
                "ORF" to listOf("Zeit im Bild", "Report", "Dok 1", "Kulturmontag", "Sport", "Weltjournal", "Am Schauplatz"),
                "SRF" to listOf("Tagesschau", "10vor10", "Arena", "DOK", "Kulturplatz", "Einstein", "Rundschau"),
                "ZDF-tivi" to listOf("logo!", "PUR+", "1, 2 oder 3", "Die Biene Maja", "Wickie", "JoNaLu", "Siebenstein"),
                "ARTE.FR" to listOf("ARTE Journal FR", "Karambolage FR", "Tracks FR", "Invitation au voyage")
            )

            val descriptions = listOf(
                "Eine spannende Dokumentation ueber aktuelle Themen und Entwicklungen.",
                "Hintergruende und Analysen zu wichtigen gesellschaftlichen Fragen.",
                "Unterhaltung und Information fuer die ganze Familie.",
                "Exklusive Einblicke hinter die Kulissen.",
                "Bewegende Geschichten aus aller Welt.",
                "Wissenschaft verstaendlich erklaert.",
                "Kultur und Kunst im Fokus.",
                "Sport, Spannung und Emotionen.",
                "Nachrichten und Hintergruende des Tages.",
                "Investigative Recherchen und Enthuellungen."
            )

            val durations = listOf(
                "5 Min", "10 Min", "15 Min", "20 Min", "30 Min",
                "45 Min", "60 Min", "1h 30min", "90 Min"
            )

            val sizes = listOf(
                "50 MB", "100 MB", "200 MB", "350 MB", "500 MB",
                "750 MB", "1.2 GB", "1.5 GB", "2.0 GB"
            )

            var entryId = 0

            // Distribute ~500 entries across channels
            for (broadcaster in Broadcaster.channelList) {
                val themes = channelThemes[broadcaster.name] ?: listOf("Allgemein", "Nachrichten", "Dokumentation")
                val entriesPerChannel = 500 / Broadcaster.channelList.size + random.nextInt(5)

                for (i in 0 until entriesPerChannel) {
                    if (entries.size >= 500) break

                    val theme = themes[random.nextInt(themes.size)]
                    val episodeNum = random.nextInt(1, 100)
                    val day = random.nextInt(1, 29)
                    val month = random.nextInt(1, 13)
                    val year = if (random.nextBoolean()) 2024 else 2025
                    val hour = random.nextInt(6, 24)
                    val minute = listOf(0, 15, 30, 45)[random.nextInt(4)]

                    val titleVariants = listOf(
                        "$theme - Folge $episodeNum",
                        "$theme vom ${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.$year",
                        "$theme: Spezial",
                        "$theme - Best of",
                        "$theme extra",
                        "$theme kompakt",
                        "$theme - Die Reportage"
                    )

                    entries.add(
                        MediaItem(
                            channel = broadcaster.name,
                            theme = theme,
                            title = titleVariants[random.nextInt(titleVariants.size)],
                            date = "${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.$year",
                            time = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} Uhr",
                            duration = durations[random.nextInt(durations.size)],
                            size = sizes[random.nextInt(sizes.size)],
                            description = descriptions[random.nextInt(descriptions.size)],
                            url = "https://example.com/media/${broadcaster.name.lowercase()}/$entryId.mp4",
                            hdUrl = "https://example.com/media/${broadcaster.name.lowercase()}/${entryId}_hd.mp4"
                        )
                    )
                    entryId++
                }

                if (entries.size >= 500) break
            }

            return entries.take(500)
        }
    }
}
