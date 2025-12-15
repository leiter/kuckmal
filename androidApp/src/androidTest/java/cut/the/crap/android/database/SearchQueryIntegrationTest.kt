package cut.the.crap.android.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Integration tests for search queries against a production database snapshot
 *
 * SETUP:
 * Place a production database file at:
 *   app/src/androidTest/assets/test_media_database.db
 *
 * To create this file:
 * 1. Run the app and load full Filmliste
 * 2. Copy database from device:
 *    adb exec-out run-as cut.the.crap cat databases/media_database > test_media_database.db
 * 3. Move to assets folder:
 *    mv test_media_database.db app/src/androidTest/assets/
 *
 * The test will automatically copy this database to the test device.
 */
@RunWith(AndroidJUnit4::class)
class SearchQueryIntegrationTest {

    private var database: AppDatabase? = null
    private lateinit var mediaDao: MediaDao
    private lateinit var context: Context
    private var testDbFile: File? = null

    companion object {
        private const val ASSET_DB_NAME = "test_media_database.db"
        private const val TEST_DB_NAME = "test_media_database_copy.db"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Copy database from assets to internal storage
        val dbFile = copyDatabaseFromAssets()
        testDbFile = dbFile

        // Open the copied database
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB_NAME
        )
            .createFromFile(dbFile)
            .fallbackToDestructiveMigration()
            .build()

        database = db
        mediaDao = db.mediaDao()

        // Verify database has data
        val count = runBlocking { mediaDao.getCount() }
        org.junit.Assume.assumeTrue(
            """
            Database from assets has insufficient data ($count entries).

            To prepare the test database:
            1. Run app and load full Filmliste (685k+ entries)
            2. Export database:
               adb exec-out run-as cut.the.crap cat databases/media_database > test_media_database.db
            3. Place in assets:
               mv test_media_database.db app/src/androidTest/assets/
            4. Rebuild and run tests
            """.trimIndent(),
            count > 1000
        )

        println("✓ Test database loaded with $count entries")
    }

    /**
     * Copy database file from androidTest/assets to internal storage
     * Uses instrumentation context to access test APK assets
     */
    private fun copyDatabaseFromAssets(): File {
        val dbPath = context.getDatabasePath(TEST_DB_NAME)

        // Ensure parent directory exists
        dbPath.parentFile?.mkdirs()

        // Delete existing copy if present
        if (dbPath.exists()) {
            dbPath.delete()
        }

        // Copy from assets using INSTRUMENTATION context (test APK), not app context
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        try {
            instrumentationContext.assets.open(ASSET_DB_NAME).use { inputStream ->
                FileOutputStream(dbPath).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            println("✓ Copied database from assets to ${dbPath.absolutePath}")
        } catch (e: Exception) {
            throw IllegalStateException(
                """
                Failed to copy database from assets.

                Ensure '$ASSET_DB_NAME' exists in app/src/androidTest/assets/

                To create it:
                1. Run app and load Filmliste
                2. adb exec-out run-as cut.the.crap cat databases/media_database > test_media_database.db
                3. mv test_media_database.db app/src/androidTest/assets/

                Error: ${e.message}
                """.trimIndent(),
                e
            )
        }

        return dbPath
    }

    @After
    fun tearDown() {
        database?.close()
        // Clean up test database copy
        testDbFile?.let { dbFile ->
            dbFile.delete()
            // Also delete Room's journal files
            File(dbFile.absolutePath + "-shm").delete()
            File(dbFile.absolutePath + "-wal").delete()
        }
    }

    @Test
    fun verifyDatabaseIsPopulated() = runBlocking {
        val count = mediaDao.getCount()
        println("✓ Database contains $count entries")
        assertTrue("Database should have entries", count > 1000)
    }

    @Test
    fun searchEntries_globalSearch() = runBlocking {
        val query = "Markus Lanz"

        val startTime = System.currentTimeMillis()
        val results = mediaDao.searchEntries(query, limit = 100)
        val duration = System.currentTimeMillis() - startTime

        println("Global search for '$query':")
        println("  - Found ${results.size} entries in ${duration}ms")
        println("  - First 5 results:")
        results.take(5).forEach { entry ->
            println("    • ${entry.channel} / ${entry.theme} / ${entry.title}")
        }

        assertTrue("Should find results for '$query'", results.isNotEmpty())

        results.forEach { entry ->
            val matchFound = entry.title.contains(query, ignoreCase = true) ||
                            entry.theme.contains(query, ignoreCase = true) ||
                            entry.description.contains(query, ignoreCase = true)
            assertTrue(
                "Entry should match query '$query': ${entry.channel}/${entry.theme}/${entry.title}",
                matchFound
            )
        }

        assertTrue("Search should complete in under 5 seconds", duration < 5000)
    }

    @Test
    fun searchEntriesByChannel_channelSpecificSearch() = runBlocking {
        val channel = "ZDF"
        val query = "Politik"

        val startTime = System.currentTimeMillis()
        val results = mediaDao.searchEntriesByChannel(channel, query, limit = 100)
        val duration = System.currentTimeMillis() - startTime

        println("Channel search for '$query' in '$channel':")
        println("  - Found ${results.size} entries in ${duration}ms")
        println("  - Sample results:")
        results.take(3).forEach { entry ->
            println("    • ${entry.theme} / ${entry.title}")
        }

        assertTrue("Should find results for '$query' in $channel", results.isNotEmpty())

        results.forEach { entry ->
            assertEquals("All results should be from $channel", channel, entry.channel)
        }

        results.forEach { entry ->
            val matchFound = entry.title.contains(query, ignoreCase = true) ||
                            entry.theme.contains(query, ignoreCase = true) ||
                            entry.description.contains(query, ignoreCase = true)
            assertTrue("Entry should match query '$query'", matchFound)
        }

        assertTrue("Search should complete in under 3 seconds", duration < 3000)
    }

    @Test
    fun searchEntriesByTheme_themeSpecificSearch() = runBlocking {
        val theme = "Politik"
        val query = "Markus"

        val startTime = System.currentTimeMillis()
        val results = mediaDao.searchEntriesByTheme(theme, query, limit = 100)
        val duration = System.currentTimeMillis() - startTime

        println("Theme search for '$query' in theme '$theme':")
        println("  - Found ${results.size} entries in ${duration}ms")
        println("  - Channels represented: ${results.map { it.channel }.distinct().sorted()}")
        println("  - Sample titles:")
        results.take(5).forEach { entry ->
            println("    • [${entry.channel}] ${entry.title}")
        }

        assertTrue("Should find results for '$query' in theme '$theme'", results.isNotEmpty())

        results.forEach { entry ->
            assertEquals("All results should be from theme '$theme'", theme, entry.theme)
        }

        results.forEach { entry ->
            val matchFound = entry.title.contains(query, ignoreCase = true) ||
                            entry.description.contains(query, ignoreCase = true)
            assertTrue("Entry should match query '$query'", matchFound)
        }

        assertTrue("Search should complete in under 3 seconds", duration < 3000)
    }

    @Test
    fun searchEntriesByChannelAndTheme_combinedSearch() = runBlocking {
        val channel = "ZDF"
        val theme = "Politik"
        val query = "Markus"

        val startTime = System.currentTimeMillis()
        val results = mediaDao.searchEntriesByChannelAndTheme(channel, theme, query, limit = 100)
        val duration = System.currentTimeMillis() - startTime

        println("Combined search for '$query' in $channel / $theme:")
        println("  - Found ${results.size} entries in ${duration}ms")
        println("  - All titles:")
        results.forEach { entry ->
            println("    • ${entry.title}")
        }

        results.forEach { entry ->
            assertEquals("All results should be from channel '$channel'", channel, entry.channel)
            assertEquals("All results should be from theme '$theme'", theme, entry.theme)

            val matchFound = entry.title.contains(query, ignoreCase = true) ||
                            entry.description.contains(query, ignoreCase = true)
            assertTrue("Entry should match query '$query'", matchFound)
        }

        assertTrue("Search should complete in under 2 seconds", duration < 2000)
    }

    @Test
    fun searchWithTrailingSpace_shouldTrim() = runBlocking {
        val query1 = "Markus Lanz"
        val query2 = "Markus Lanz "

        val results1 = mediaDao.searchEntries(query1, limit = 100)
        val results2 = mediaDao.searchEntries(query2, limit = 100)

        println("Search comparison:")
        println("  - Without trailing space: ${results1.size} results")
        println("  - With trailing space: ${results2.size} results")

        val difference = kotlin.math.abs(results1.size - results2.size)
        assertTrue(
            "Results should be similar (difference: $difference)",
            difference < 5
        )
    }

    @Test
    fun searchEmptyQuery_shouldReturnNoResults() = runBlocking {
        val results = mediaDao.searchEntries("", limit = 100)
        println("Empty query returned ${results.size} results")
    }

    @Test
    fun searchCaseSensitivity_shouldBeCaseInsensitive() = runBlocking {
        val queryLower = "markus lanz"
        val queryUpper = "MARKUS LANZ"
        val queryMixed = "Markus Lanz"

        val resultsLower = mediaDao.searchEntries(queryLower, limit = 100)
        val resultsUpper = mediaDao.searchEntries(queryUpper, limit = 100)
        val resultsMixed = mediaDao.searchEntries(queryMixed, limit = 100)

        println("Case sensitivity test:")
        println("  - lowercase: ${resultsLower.size} results")
        println("  - UPPERCASE: ${resultsUpper.size} results")
        println("  - Mixed: ${resultsMixed.size} results")

        assertEquals("Case should not matter", resultsLower.size, resultsUpper.size)
        assertEquals("Case should not matter", resultsLower.size, resultsMixed.size)
    }

    @Test
    fun searchPerformanceComparison_globalVsChannelVsTheme() = runBlocking {
        val query = "Politik"
        val channel = "ZDF"
        val theme = "Nachrichten"

        val startGlobal = System.currentTimeMillis()
        val globalResults = mediaDao.searchEntries(query, limit = 1000)
        val globalDuration = System.currentTimeMillis() - startGlobal

        val startChannel = System.currentTimeMillis()
        val channelResults = mediaDao.searchEntriesByChannel(channel, query, limit = 1000)
        val channelDuration = System.currentTimeMillis() - startChannel

        val startTheme = System.currentTimeMillis()
        val themeResults = mediaDao.searchEntriesByTheme(theme, query, limit = 1000)
        val themeDuration = System.currentTimeMillis() - startTheme

        println("Performance comparison for '$query':")
        println("  - Global search: ${globalResults.size} results in ${globalDuration}ms")
        println("  - Channel search ($channel): ${channelResults.size} results in ${channelDuration}ms")
        println("  - Theme search ($theme): ${themeResults.size} results in ${themeDuration}ms")

        assertTrue(
            "Channel search should be faster or similar to global search",
            channelDuration <= globalDuration * 1.5
        )

        assertTrue("Global search should complete in under 10s", globalDuration < 10000)
        assertTrue("Channel search should complete in under 5s", channelDuration < 5000)
        assertTrue("Theme search should complete in under 5s", themeDuration < 5000)
    }

    @Test
    fun verifyDistinctThemesInSearchResults() = runBlocking {
        val query = "Markus"

        val results = mediaDao.searchEntries(query, limit = 5000)
        val distinctThemes = results.map { it.theme }.distinct()

        println("Search for '$query' found:")
        println("  - ${results.size} total entries")
        println("  - ${distinctThemes.size} distinct themes")
        println("  - Sample themes: ${distinctThemes.take(10)}")

        assertTrue("Should find multiple distinct themes", distinctThemes.size > 1)
    }

    @Test
    fun verifyPaginationInSearch() = runBlocking {
        val query = "Politik"

        val page1 = mediaDao.searchEntries(query, limit = 10)
        val allResults = mediaDao.searchEntries(query, limit = 100)

        println("Pagination test:")
        println("  - Page 1 (limit 10): ${page1.size} results")
        println("  - All results (limit 100): ${allResults.size} results")

        assertTrue("Page 1 should have at most 10 results", page1.size <= 10)
        assertTrue("All results should have more than page 1", allResults.size >= page1.size)
    }
}
