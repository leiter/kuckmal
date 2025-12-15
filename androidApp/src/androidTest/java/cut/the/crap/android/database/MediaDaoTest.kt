package cut.the.crap.android.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for MediaDao and Room database
 * These tests run on an Android device or emulator
 */
@RunWith(AndroidJUnit4::class)
class MediaDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var mediaDao: MediaDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        mediaDao = database.mediaDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieve_singleEntry() = runBlocking {
        val entry = MediaEntry(
            channel = "ARD",
            theme = "Nachrichten",
            title = "Tagesschau",
            date = "30.10.2025",
            time = "20:00:00",
            duration = "00:15:00",
            sizeMB = "150",
            description = "Die Nachrichten",
            url = "https://example.com/video.mp4",
            website = "https://ard.de",
            subtitleUrl = "https://example.com/sub.vtt",
            smallUrl = "https://example.com/small.mp4",
            hdUrl = "https://example.com/hd.mp4",
            timestamp = 1730318400L,
            geo = "DE",
            isNew = true
        )

        // Insert
        val id = mediaDao.insert(entry)
        assertTrue(id > 0)

        // Retrieve
        val retrieved = mediaDao.getMediaEntry("ARD", "Nachrichten", "Tagesschau")
        assertNotNull(retrieved)
        assertEquals(entry.channel, retrieved!!.channel)
        assertEquals(entry.theme, retrieved.theme)
        assertEquals(entry.title, retrieved.title)
        assertEquals(entry.timestamp, retrieved.timestamp)
        assertTrue(retrieved.isNew)
    }

    @Test
    fun insertAll_bulkInsert() = runBlocking {
        val entries = listOf(
            MediaEntry(channel = "ARD", theme = "News", title = "Title1", timestamp = 1000000L),
            MediaEntry(channel = "ARD", theme = "News", title = "Title2", timestamp = 2000000L),
            MediaEntry(channel = "ZDF", theme = "Sport", title = "Title3", timestamp = 3000000L)
        )

        // Insert all
        mediaDao.insertAll(entries)

        // Verify count
        val count = mediaDao.getCount()
        assertEquals(3, count)
    }

    @Test
    fun deleteAll_removesAllEntries() = runBlocking {
        // Insert some entries
        val entries = listOf(
            MediaEntry(channel = "ARD", theme = "News", title = "Title1"),
            MediaEntry(channel = "ZDF", theme = "Sport", title = "Title2")
        )
        mediaDao.insertAll(entries)
        assertEquals(2, mediaDao.getCount())

        // Delete all
        mediaDao.deleteAll()

        // Verify empty
        assertEquals(0, mediaDao.getCount())
    }

    @Test
    fun getAllChannels_returnsUniqueChannelsSorted() = runBlocking {
        val entries = listOf(
            MediaEntry(channel = "ZDF", theme = "News", title = "T1"),
            MediaEntry(channel = "ARD", theme = "News", title = "T2"),
            MediaEntry(channel = "ZDF", theme = "Sport", title = "T3"),
            MediaEntry(channel = "3sat", theme = "Movies", title = "T4")
        )
        mediaDao.insertAll(entries)

        val channels = mediaDao.getAllChannels()

        assertEquals(3, channels.size)
        assertEquals("3sat", channels[0])  // Sorted alphabetically
        assertEquals("ARD", channels[1])
        assertEquals("ZDF", channels[2])
    }

    @Test
    fun getAllThemes_returnsUniqueThemesWithDateFilter() = runBlocking {
        val entries = listOf(
            MediaEntry(channel = "ARD", theme = "News", title = "T1", timestamp = 1000000L),
            MediaEntry(channel = "ARD", theme = "Sport", title = "T2", timestamp = 2000000L),
            MediaEntry(channel = "ZDF", theme = "News", title = "T3", timestamp = 3000000L),
            MediaEntry(channel = "ZDF", theme = "Movies", title = "T4", timestamp = 500000L)  // Old
        )
        mediaDao.insertAll(entries)

        // Get themes newer than timestamp 900000
        val themes = mediaDao.getAllThemes(900000L, 100, 0)

        // Should have 2 unique themes: News (from both ARD and ZDF) and Sport
        assertEquals(2, themes.size)
        assertTrue(themes.contains("News"))
        assertTrue(themes.contains("Sport"))
        assertFalse(themes.contains("Movies"))  // Filtered out by date
    }

    @Test
    fun getThemesForChannel_filtersbyChannel() = runBlocking {
        val entries = listOf(
            MediaEntry(channel = "ARD", theme = "News", title = "T1", timestamp = 1000000L),
            MediaEntry(channel = "ARD", theme = "Sport", title = "T2", timestamp = 2000000L),
            MediaEntry(channel = "ZDF", theme = "Movies", title = "T3", timestamp = 3000000L)
        )
        mediaDao.insertAll(entries)

        val themes = mediaDao.getThemesForChannel("ARD", 0L, 100, 0)

        assertEquals(2, themes.size)
        assertTrue(themes.contains("News"))
        assertTrue(themes.contains("Sport"))
        assertFalse(themes.contains("Movies"))  // ZDF, not ARD
    }

    @Test
    fun getTitlesForTheme_returnsCorrectTitles() = runBlocking {
        val entries = listOf(
            MediaEntry(channel = "ARD", theme = "News", title = "Title1", timestamp = 1000000L),
            MediaEntry(channel = "ARD", theme = "News", title = "Title2", timestamp = 2000000L),
            MediaEntry(channel = "ARD", theme = "Sport", title = "Title3", timestamp = 3000000L)
        )
        mediaDao.insertAll(entries)

        val titles = mediaDao.getTitlesForTheme("News", 0L)

        assertEquals(2, titles.size)
        assertTrue(titles.contains("Title1"))
        assertTrue(titles.contains("Title2"))
        assertFalse(titles.contains("Title3"))  // Sport theme
    }

    @Test
    fun getTitlesForChannelAndTheme_filtersCorrectly() = runBlocking {
        val entries = listOf(
            MediaEntry(channel = "ARD", theme = "News", title = "ARD News 1", timestamp = 1000000L),
            MediaEntry(channel = "ARD", theme = "News", title = "ARD News 2", timestamp = 2000000L),
            MediaEntry(channel = "ZDF", theme = "News", title = "ZDF News 1", timestamp = 3000000L)
        )
        mediaDao.insertAll(entries)

        val titles = mediaDao.getTitlesForChannelAndTheme("ARD", "News", 0L)

        assertEquals(2, titles.size)
        assertTrue(titles.contains("ARD News 1"))
        assertTrue(titles.contains("ARD News 2"))
        assertFalse(titles.contains("ZDF News 1"))  // Different channel
    }

    @Test
    fun getMediaEntry_returnsCorrectEntry() = runBlocking {
        val entry = MediaEntry(
            channel = "ARD",
            theme = "News",
            title = "TestTitle",
            description = "Test Description",
            url = "https://example.com/test.mp4",
            timestamp = 1000000L
        )
        mediaDao.insert(entry)

        val retrieved = mediaDao.getMediaEntry("ARD", "News", "TestTitle")

        assertNotNull(retrieved)
        assertEquals("ARD", retrieved!!.channel)
        assertEquals("News", retrieved.theme)
        assertEquals("TestTitle", retrieved.title)
        assertEquals("Test Description", retrieved.description)
        assertEquals("https://example.com/test.mp4", retrieved.url)
    }

    @Test
    fun searchEntries_findsMatchingEntries() = runBlocking {
        val entries = listOf(
            MediaEntry(channel = "ARD", theme = "News", title = "Football Match", description = "Sports news"),
            MediaEntry(channel = "ZDF", theme = "Sport", title = "Basketball Game", description = "Live game"),
            MediaEntry(channel = "3sat", theme = "Movies", title = "Action Movie", description = "An action film")
        )
        mediaDao.insertAll(entries)

        // Search for "game"
        val results = mediaDao.searchEntries("game", 10)

        assertEquals(1, results.size)
        assertEquals("Basketball Game", results[0].title)
    }

    @Test
    fun getEntriesByChannel_withPagination() = runBlocking {
        val entries = mutableListOf<MediaEntry>()
        for (i in 1..25) {
            entries.add(MediaEntry(channel = "ARD", theme = "News", title = "Title$i", timestamp = i.toLong() * 1000))
        }
        mediaDao.insertAll(entries)

        // Get first page (10 items)
        val page1 = mediaDao.getEntriesByChannel("ARD", 0L, 10, 0)
        assertEquals(10, page1.size)

        // Get second page
        val page2 = mediaDao.getEntriesByChannel("ARD", 0L, 10, 10)
        assertEquals(10, page2.size)

        // Get third page
        val page3 = mediaDao.getEntriesByChannel("ARD", 0L, 10, 20)
        assertEquals(5, page3.size)  // Only 5 remaining
    }

    @Test
    fun getRecentEntries_returnsNewAndRecentItems() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        val oneDayAgo = now - (24 * 60 * 60)
        val oneWeekAgo = now - (7 * 24 * 60 * 60)

        val entries = listOf(
            MediaEntry(channel = "ARD", theme = "News", title = "Recent", timestamp = oneDayAgo, isNew = false),
            MediaEntry(channel = "ARD", theme = "News", title = "Old", timestamp = oneWeekAgo, isNew = false),
            MediaEntry(channel = "ARD", theme = "News", title = "New Tagged", timestamp = oneWeekAgo, isNew = true)
        )
        mediaDao.insertAll(entries)

        // Get entries from last 3 days
        val threeDaysAgo = now - (3 * 24 * 60 * 60)
        val recent = mediaDao.getRecentEntries(threeDaysAgo, 10)

        assertEquals(2, recent.size)  // Recent + New Tagged
        assertTrue(recent.any { it.title == "Recent" })
        assertTrue(recent.any { it.title == "New Tagged" })
        assertFalse(recent.any { it.title == "Old" })
    }

    @Test
    fun insertWithConflict_replacesEntry() = runBlocking {
        // Insert initial entry
        val entry1 = MediaEntry(
            id = 1,
            channel = "ARD",
            theme = "News",
            title = "Title",
            description = "Original"
        )
        mediaDao.insert(entry1)

        // Insert conflicting entry (same ID)
        val entry2 = MediaEntry(
            id = 1,
            channel = "ARD",
            theme = "News",
            title = "Title",
            description = "Updated"
        )
        mediaDao.insert(entry2)

        // Should only have one entry with updated description
        val count = mediaDao.getCount()
        assertEquals(1, count)

        val retrieved = mediaDao.getMediaEntry("ARD", "News", "Title")
        assertEquals("Updated", retrieved!!.description)
    }

    @Test
    fun stressTest_insertLargeNumberOfEntries() = runBlocking {
        val entries = mutableListOf<MediaEntry>()
        for (i in 1..10000) {
            entries.add(MediaEntry(
                channel = "Channel${i % 10}",
                theme = "Theme${i % 100}",
                title = "Title$i",
                timestamp = i.toLong()
            ))
        }

        // Insert in chunks to simulate real usage
        entries.chunked(1000).forEach { chunk ->
            mediaDao.insertAll(chunk)
        }

        val count = mediaDao.getCount()
        assertEquals(10000, count)

        // Verify queries still work efficiently
        val channels = mediaDao.getAllChannels()
        assertEquals(10, channels.size)

        val themes = mediaDao.getAllThemes(0L, 1000, 0)
        assertEquals(100, themes.size)
    }
}
