package com.mediathekview.android.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mediathekview.android.data.MediaListParser
import com.mediathekview.android.database.AppDatabase
import com.mediathekview.android.database.MediaDao
import com.mediathekview.android.database.MediaEntry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tests for differential media list updates
 * Verifies: new items added, existing items updated (not duplicated), no duplicate entries
 */
@RunWith(AndroidJUnit4::class)
class MediaRepositoryDiffUpdateTest {

    private lateinit var database: AppDatabase
    private lateinit var mediaDao: MediaDao
    private lateinit var repository: MediaRepositoryImpl
    private lateinit var parser: MediaListParser
    private lateinit var testDir: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        mediaDao = database.mediaDao()
        parser = MediaListParser()
        repository = MediaRepositoryImpl(mediaDao, parser)

        testDir = File.createTempFile("test", "dir").apply {
            delete()
            mkdir()
            deleteOnExit()
        }
    }

    @After
    fun tearDown() {
        database.close()
        testDir.deleteRecursively()
    }

    private fun createTestFile(name: String, content: String): File {
        return File(testDir, name).apply {
            writeText(content)
            deleteOnExit()
        }
    }

    @Test
    fun testInitialLoadFollowedByDiffUpdateNoDuplicates() = runBlocking {
        // Load initial 3 entries
        val initialFile = createTestFile("initial.json", """
            {
              "Filmliste": ["13.11.2025", "10:00", "abc123", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Title1", "13.11.2025", "10:00:00", "00:15:00", "150", "Original description", "https://example.com/v1.mp4", "https://ard.de", "", "", "", "", "", "", "1731488400", "", "DE", "false"],
              "X": ["ARD", "News", "Title2", "13.11.2025", "10:30:00", "00:20:00", "200", "Description 2", "https://example.com/v2.mp4", "", "", "", "", "", "", "", "1731490200", "", "DE", "false"],
              "X": ["ZDF", "Sport", "Title3", "13.11.2025", "11:00:00", "00:25:00", "250", "Description 3", "https://example.com/v3.mp4", "", "", "", "", "", "", "", "1731492000", "", "DE", "false"]
            }
        """.trimIndent())

        repository.loadMediaListFromFile(initialFile.absolutePath).collect { result ->
            if (result is MediaRepository.LoadingResult.Complete) {
                assertEquals("Should load 3 initial entries", 3, result.totalEntries)
            }
        }

        assertEquals("Database should have 3 entries", 3, repository.getCount())

        // Apply diff with 1 updated entry and 2 new entries
        val diffFile = createTestFile("diff.json", """
            {
              "Filmliste": ["13.11.2025", "12:00", "def456", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Title1", "13.11.2025", "10:00:00", "00:15:00", "150", "UPDATED description", "https://example.com/v1.mp4", "https://ard.de", "", "", "", "", "", "", "1731488400", "", "DE", "true"],
              "X": ["ARD", "News", "Title4", "13.11.2025", "12:00:00", "00:30:00", "300", "New entry 4", "https://example.com/v4.mp4", "", "", "", "", "", "", "", "1731495600", "", "DE", "false"],
              "X": ["ZDF", "Movies", "Title5", "13.11.2025", "12:30:00", "00:35:00", "350", "New entry 5", "https://example.com/v5.mp4", "", "", "", "", "", "", "", "1731497400", "", "DE", "false"]
            }
        """.trimIndent())

        repository.applyDiffToDatabase(diffFile.absolutePath).collect { result ->
            if (result is MediaRepository.LoadingResult.Complete) {
                assertEquals("Diff should contain 3 entries", 3, result.totalEntries)
            }
        }

        // Verify final count
        val finalCount = repository.getCount()
        assertEquals("Database should have 5 entries total", 5, finalCount)

        // Verify Title1 was UPDATED not duplicated
        val title1Entries = mediaDao.searchEntries("Title1", 10)
        assertEquals("Title1 should appear exactly once", 1, title1Entries.size)
        assertEquals("Title1 description should be updated", "UPDATED description", title1Entries[0].description)
        assertTrue("Title1 isNew flag should be updated to true", title1Entries[0].isNew)

        // Verify Title2 still exists
        val title2Entries = mediaDao.searchEntries("Title2", 10)
        assertEquals("Title2 should still exist", 1, title2Entries.size)

        // Verify new entries were added
        val title4Entries = mediaDao.searchEntries("Title4", 10)
        assertEquals("Title4 should be added", 1, title4Entries.size)

        val title5Entries = mediaDao.searchEntries("Title5", 10)
        assertEquals("Title5 should be added", 1, title5Entries.size)
    }

    @Test
    fun testMultipleDiffUpdatesNoDuplicatesAccumulate() = runBlocking {
        // Initial load
        val initialFile = createTestFile("initial2.json", """
            {
              "Filmliste": ["13.11.2025", "10:00", "abc", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Title1", "13.11.2025", "10:00:00", "00:15:00", "150", "Version 1", "https://example.com/v1.mp4", "", "", "", "", "", "", "", "1731488400", "", "", "false"]
            }
        """.trimIndent())

        repository.loadMediaListFromFile(initialFile.absolutePath).collect { result ->
            if (result is MediaRepository.LoadingResult.Complete) {
                assertEquals(1, result.totalEntries)
            }
        }

        assertEquals("Initial count should be 1", 1, repository.getCount())

        // Apply first diff
        val diff1File = createTestFile("diff1.json", """
            {
              "Filmliste": ["13.11.2025", "11:00", "def", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Title1", "13.11.2025", "10:00:00", "00:15:00", "150", "Version 2", "https://example.com/v1.mp4", "", "", "", "", "", "", "", "1731488400", "", "", "false"]
            }
        """.trimIndent())

        repository.applyDiffToDatabase(diff1File.absolutePath).collect { result ->
            if (result is MediaRepository.LoadingResult.Complete) {
                assertEquals(1, result.totalEntries)
            }
        }

        assertEquals("Count should still be 1 after first diff", 1, repository.getCount())
        val afterDiff1 = mediaDao.searchEntries("Title1", 10)
        assertEquals("Title1 should appear once", 1, afterDiff1.size)
        assertEquals("Title1 updated to Version 2", "Version 2", afterDiff1[0].description)

        // Apply second diff
        val diff2File = createTestFile("diff2.json", """
            {
              "Filmliste": ["13.11.2025", "12:00", "ghi", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Title1", "13.11.2025", "10:00:00", "00:15:00", "150", "Version 3", "https://example.com/v1.mp4", "", "", "", "", "", "", "", "1731488400", "", "", "false"]
            }
        """.trimIndent())

        repository.applyDiffToDatabase(diff2File.absolutePath).collect { result ->
            if (result is MediaRepository.LoadingResult.Complete) {
                assertEquals(1, result.totalEntries)
            }
        }

        assertEquals("Count should still be 1 after second diff", 1, repository.getCount())
        val afterDiff2 = mediaDao.searchEntries("Title1", 10)
        assertEquals("Title1 should STILL appear only once", 1, afterDiff2.size)
        assertEquals("Title1 updated to Version 3", "Version 3", afterDiff2[0].description)
    }

    @Test
    fun testDiffWithSamePrimaryKeyUpdatesNotDuplicates() = runBlocking {
        // Primary key in MediaEntry is channel, theme, title
        val initialFile = createTestFile("initial4.json", """
            {
              "Filmliste": ["13.11.2025", "10:00", "abc", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Breaking", "13.11.2025", "10:00:00", "00:10:00", "100", "Original story", "https://example.com/breaking.mp4", "", "", "", "", "", "", "", "1731488400", "", "", "false"]
            }
        """.trimIndent())

        repository.loadMediaListFromFile(initialFile.absolutePath).collect { result ->
            if (result is MediaRepository.LoadingResult.Complete) {
                assertEquals(1, result.totalEntries)
            }
        }

        // Apply diff with same channel+theme+title but different content
        val diffFile = createTestFile("diff4.json", """
            {
              "Filmliste": ["13.11.2025", "12:00", "def", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Breaking", "13.11.2025", "12:00:00", "00:15:00", "150", "Updated story with more details", "https://example.com/breaking_updated.mp4", "", "", "", "", "", "", "", "1731495600", "", "", "true"]
            }
        """.trimIndent())

        repository.applyDiffToDatabase(diffFile.absolutePath).collect { result ->
            if (result is MediaRepository.LoadingResult.Complete) {
                assertEquals(1, result.totalEntries)
            }
        }

        // Verify NO duplicate
        val finalCount = repository.getCount()
        assertEquals("Should still have exactly 1 entry", 1, finalCount)

        // Verify the entry was UPDATED
        val entry = mediaDao.getMediaEntry("ARD", "News", "Breaking")
        assertNotNull("Entry should exist", entry)
        assertEquals("Description should be updated", "Updated story with more details", entry!!.description)
        assertEquals("URL should be updated", "https://example.com/breaking_updated.mp4", entry.url)
        assertEquals("Time should be updated", "12:00:00", entry.time)
        assertEquals("Duration should be updated", "00:15:00", entry.duration)
        assertEquals("Size should be updated", "150", entry.sizeMB)
        assertEquals("Timestamp should be updated", 1731495600L, entry.timestamp)
        assertTrue("isNew flag should be updated", entry.isNew)
    }
}
