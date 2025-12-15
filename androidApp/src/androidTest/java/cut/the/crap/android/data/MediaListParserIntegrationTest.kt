package cut.the.crap.android.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cut.the.crap.shared.database.AppDatabase
import cut.the.crap.shared.database.getDatabaseBuilder
import cut.the.crap.shared.database.getRoomDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Integration tests for MediaListParser using test assets
 * These tests run on an Android device or emulator and test the full parsing pipeline
 */
@RunWith(AndroidJUnit4::class)
class MediaListParserIntegrationTest {

    private lateinit var context: Context
    private lateinit var parser: MediaListParser
    private lateinit var database: AppDatabase
    private lateinit var testFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        parser = MediaListParser()

        // Set up in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()

        // Copy test asset to cache directory for parsing
        testFile = copyAssetToCache("test_media_list.json")
    }

    @After
    fun tearDown() {
        database.close()
        testFile.delete()
    }

    /**
     * Helper function to copy asset file to cache directory
     * Returns the File object pointing to the copied file
     */
    private fun copyAssetToCache(assetName: String): File {
        val cacheFile = File(context.cacheDir, assetName)
        context.assets.open(assetName).use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }
        return cacheFile
    }

    @Test
    fun parseAssetFile_successfullyParsesTestData() {
        // Parse the test asset file
        val result = parser.parseFile(testFile.absolutePath)

        assertEquals(0, result)  // Success code

        val mediaList = parser.getMediaList()
        assertEquals(3, mediaList.size)

        // Verify first entry (ARD - Tagesschau)
        val firstEntry = mediaList[0]
        assertEquals("ARD", firstEntry.channel)
        assertEquals("Nachrichten", firstEntry.theme)
        assertEquals("Tagesschau", firstEntry.title)
        assertEquals("Die Nachrichten um 20 Uhr", firstEntry.description)
        assertEquals("https://example.com/video.mp4", firstEntry.url)

        // Verify second entry (inherits channel from first)
        val secondEntry = mediaList[1]
        assertEquals("ARD", secondEntry.channel)
        assertEquals("Nachrichten", secondEntry.theme)
        assertEquals("Tagesthemen", secondEntry.title)

        // Verify third entry (ZDF - Heute Sport)
        val thirdEntry = mediaList[2]
        assertEquals("ZDF", thirdEntry.channel)
        assertEquals("Sport", thirdEntry.theme)
        assertEquals("Heute Sport", thirdEntry.title)
    }

    @Test
    fun parseAssetFileChunked_insertsToDatabase() = runBlocking {
        var totalParsed = 0
        var chunkCount = 0
        var completeCalled = false

        parser.parseFileChunked(testFile.absolutePath, object : MediaListParser.ChunkCallback {
            override fun onChunk(entries: List<cut.the.crap.android.model.MediaEntry>, totalParsedSoFar: Int) {
                chunkCount++
                totalParsed = totalParsedSoFar

                // Insert to database
                val dbEntries = entries.map { it.toDatabaseEntry() }
                runBlocking {
                    database.mediaDao().insertAll(dbEntries)
                }
            }

            override fun onComplete(totalEntries: Int) {
                completeCalled = true
                assertEquals(3, totalEntries)
            }

            override fun onError(error: Exception, entriesParsed: Int) {
                fail("Parsing should not fail: ${error.message}")
            }
        })

        // Verify parsing completed
        assertTrue(completeCalled)
        assertEquals(3, totalParsed)

        // Verify database has entries
        val count = database.mediaDao().getCount()
        assertEquals(3, count)

        // Verify we can query the data
        val channels = database.mediaDao().getAllChannels()
        assertTrue(channels.contains("ARD"))
        assertTrue(channels.contains("ZDF"))
    }

    @Test
    fun getThemesFromAsset_returnsCorrectThemes() {
        // Parse the test file
        parser.parseFile(testFile.absolutePath)
        val mediaList = parser.getMediaList()

        // Get all themes
        val themes = parser.getAllThemesFromCache(mediaList, 0, 100)

        // Should have 2 unique themes (Nachrichten and Sport)
        assertEquals(2, themes.size)
        assertTrue(themes.contains("Nachrichten"))
        assertTrue(themes.contains("Sport"))
    }

    @Test
    fun getThemesOfChannel_returnsChannelSpecificThemes() {
        // Parse the test file
        parser.parseFile(testFile.absolutePath)
        val mediaList = parser.getMediaList()

        // Get themes for ARD
        val ardThemes = parser.getThemesOfChannelFromCache(mediaList, "ARD", 0, 100)
        assertEquals(1, ardThemes.size)
        assertEquals("Nachrichten", ardThemes[0])

        // Get themes for ZDF
        val zdfThemes = parser.getThemesOfChannelFromCache(mediaList, "ZDF", 0, 100)
        assertEquals(1, zdfThemes.size)
        assertEquals("Sport", zdfThemes[0])
    }

    @Test
    fun getTitlesOfTheme_returnsCorrectTitles() {
        // Parse the test file
        parser.parseFile(testFile.absolutePath)
        val mediaList = parser.getMediaList()

        // Get titles for "Nachrichten" theme on ARD
        val titles = parser.getTitlesOfThemeFromCache(mediaList, "ARD", "Nachrichten")

        assertEquals(2, titles.size)
        assertTrue(titles.contains("Tagesschau"))
        assertTrue(titles.contains("Tagesthemen"))
    }

    @Test
    fun getMediaInfo_returnsCompleteInfo() {
        // Parse the test file
        parser.parseFile(testFile.absolutePath)
        val mediaList = parser.getMediaList()

        // Get info for specific entry
        val info = parser.getMediaInfoFromCache(mediaList, "ARD", "Nachrichten", "Tagesschau")

        assertNotNull(info)
        assertEquals("ARD", info!![0])
        assertEquals("Nachrichten", info[1])
        assertEquals("Tagesschau", info[2])
        assertEquals("Die Nachrichten um 20 Uhr", info[6])  // description
        assertEquals("https://example.com/video.mp4", info[7])  // url
    }
}
