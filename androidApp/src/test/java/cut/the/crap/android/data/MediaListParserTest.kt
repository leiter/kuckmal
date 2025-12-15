package cut.the.crap.android.data

import cut.the.crap.android.model.MediaEntry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for MediaListParser
 */
class MediaListParserTest {

    private lateinit var parser: MediaListParser
    private lateinit var testDir: File

    @Before
    fun setup() {
        parser = MediaListParser()
        // Create a temp directory for test files
        testDir = File.createTempFile("test", "dir").apply {
            delete()
            mkdir()
            deleteOnExit()
        }
    }

    private fun createTestFile(name: String): File {
        return File(testDir, name).apply {
            deleteOnExit()
        }
    }

    @Test
    fun `parseFile successfully parses valid JSON`() {
        // Create a test JSON file
        val testFile = createTestFile("test.json")
        testFile.writeText("""
            {
              "Filmliste": ["30.10.2025", "21:00", "abc123", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Title1", "30.10.2025", "20:00:00", "00:15:00", "150", "Description", "https://example.com/v1.mp4", "https://ard.de", "", "", "", "", "", "", "1730318400", "", "DE", "true"],
              "X": ["", "", "Title2", "30.10.2025", "21:00:00", "00:20:00", "200", "Desc2", "https://example.com/v2.mp4", "", "", "", "", "", "", "", "1730322000", "", "", "false"]
            }
        """.trimIndent())

        val result = parser.parseFile(testFile.absolutePath)

        assertEquals(0, result)  // Success
        assertEquals(2, parser.getNumberOfLines())

        val list = parser.getMediaList()
        assertEquals(2, list.size)

        // Check first entry
        assertEquals("ARD", list[0].channel)
        assertEquals("News", list[0].theme)
        assertEquals("Title1", list[0].title)
        assertEquals(1730318400L, list[0].dateL)
        assertTrue(list[0].isNew)

        // Check second entry (inherits channel/theme)
        assertEquals("ARD", list[1].channel)  // Inherited
        assertEquals("News", list[1].theme)   // Inherited
        assertEquals("Title2", list[1].title)
        assertEquals(1730322000L, list[1].dateL)
        assertFalse(list[1].isNew)
    }

    @Test
    fun `parseFileChunked processes in chunks with callbacks`() {
        // Create a larger test file with multiple entries
        val testFile = createTestFile("test_chunked.json")
        val sb = StringBuilder()
        sb.append("{\"Filmliste\":[\"30.10.2025\",\"21:00\",\"abc123\",\"MediathekView\",\"1.0\"]")

        // Add 2500 entries to test chunking (default chunk size is 1000)
        for (i in 1..2500) {
            sb.append(",\"X\":[\"ARD\",\"News\",\"Title$i\",\"\",\"\",\"\",\"\",\"\",\"https://example.com/v$i.mp4\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"$i\",\"\",\"\",\"false\"]")
        }
        sb.append("}")
        testFile.writeText(sb.toString())

        var chunkCount = 0
        var totalParsed = 0
        var completed = false

        parser.parseFileChunked(testFile.absolutePath, object : MediaListParser.ChunkCallback {
            override fun onChunk(entries: List<MediaEntry>, totalParsedSoFar: Int) {
                chunkCount++
                totalParsed = totalParsedSoFar
                assertTrue(entries.isNotEmpty())
                assertTrue(entries.size <= 1000)  // Chunk size limit
            }

            override fun onComplete(totalEntries: Int) {
                completed = true
                assertEquals(2500, totalEntries)
            }

            override fun onError(error: Exception, entriesParsed: Int) {
                fail("Should not have error: ${error.message}")
            }
        }, chunkSize = 1000)

        assertTrue(completed)
        assertEquals(2500, totalParsed)
        assertTrue(chunkCount >= 3)  // Should have at least 3 chunks for 2500 entries
    }

    @Test
    fun `parseFile handles non-existent file`() {
        val result = parser.parseFile("/nonexistent/file.json")
        assertEquals(3, result)  // Read error
    }

    @Test
    fun `parseFile handles empty file`() {
        val testFile = createTestFile("empty.json")
        testFile.writeText("")

        val result = parser.parseFile(testFile.absolutePath)
        assertEquals(3, result)  // Read error (EOF)
    }

    @Test
    fun `parseFile handles invalid JSON`() {
        val testFile = createTestFile("invalid.json")
        testFile.writeText("{invalid json}")

        val result = parser.parseFile(testFile.absolutePath)
        assertEquals(3, result)  // Read error (MalformedJsonException extends IOException)
    }

    @Test
    fun `setLimitDate filters entries correctly`() {
        val testFile = createTestFile("test_limit.json")
        testFile.writeText("""
            {
              "Filmliste": ["30.10.2025", "21:00", "abc123", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Old", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"],
              "X": ["ARD", "News", "New", "", "", "", "", "", "", "", "", "", "", "", "", "", "2000000", "", "", "false"]
            }
        """.trimIndent())

        parser.setLimitDate(1500000)  // Set filter between the two entries
        parser.parseFile(testFile.absolutePath)

        val list = parser.getMediaList()
        assertEquals(2, list.size)

        // First entry should be filtered out
        assertFalse(list[0].inTimePeriod)

        // Second entry should be included
        assertTrue(list[1].inTimePeriod)
    }

    @Test
    fun `getAllThemes returns unique sorted themes`() {
        val testFile = createTestFile("test_themes.json")
        testFile.writeText("""
            {
              "Filmliste": ["30.10.2025", "21:00", "abc123", "MediathekView", "1.0"],
              "X": ["ARD", "News", "T1", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"],
              "X": ["ARD", "Sport", "T2", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"],
              "X": ["ZDF", "News", "T3", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"],
              "X": ["ZDF", "Movies", "T4", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"]
            }
        """.trimIndent())

        parser.parseFile(testFile.absolutePath)
        val themes = parser.getAllThemes(0, 100)

        assertEquals(3, themes.size)  // Movies, News, Sport (sorted)
        assertEquals("Movies", themes[0])
        assertEquals("News", themes[1])
        assertEquals("Sport", themes[2])
    }

    @Test
    fun `getThemesOfChannel returns themes for specific channel`() {
        val testFile = createTestFile("test_channel_themes.json")
        testFile.writeText("""
            {
              "Filmliste": ["30.10.2025", "21:00", "abc123", "MediathekView", "1.0"],
              "X": ["ARD", "News", "T1", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"],
              "X": ["ARD", "Sport", "T2", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"],
              "X": ["ZDF", "Movies", "T3", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"]
            }
        """.trimIndent())

        parser.parseFile(testFile.absolutePath)
        val themes = parser.getThemesOfChannel("ARD", 0, 100)

        assertEquals(2, themes.size)
        assertTrue(themes.contains("News"))
        assertTrue(themes.contains("Sport"))
        assertFalse(themes.contains("Movies"))  // ZDF theme, not ARD
    }

    @Test
    fun `getTitlesOfTheme returns titles for specific theme`() {
        val testFile = createTestFile("test_titles.json")
        testFile.writeText("""
            {
              "Filmliste": ["30.10.2025", "21:00", "abc123", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Title1", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"],
              "X": ["ARD", "News", "Title2", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"],
              "X": ["ARD", "Sport", "Title3", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"]
            }
        """.trimIndent())

        parser.parseFile(testFile.absolutePath)
        val titles = parser.getTitlesOfTheme("ARD", "News")

        assertEquals(2, titles.size)
        assertEquals("Title1", titles[0])
        assertEquals("Title2", titles[1])
    }

    @Test
    fun `getMediaInfo returns correct entry info`() {
        val testFile = createTestFile("test_info.json")
        testFile.writeText("""
            {
              "Filmliste": ["30.10.2025", "21:00", "abc123", "MediathekView", "1.0"],
              "X": ["ARD", "News", "TestTitle", "30.10.2025", "20:00:00", "00:15:00", "150", "Test Description", "https://example.com/video.mp4", "https://ard.de", "", "", "https://example.com/small.mp4", "", "", "", "1730318400", "", "DE", "true"]
            }
        """.trimIndent())

        parser.parseFile(testFile.absolutePath)
        val info = parser.getMediaInfo("ARD", "News", "TestTitle")

        assertNotNull(info)
        assertEquals(11, info!!.size)  // Now returns 11 elements
        assertEquals("ARD", info[0])
        assertEquals("News", info[1])
        assertEquals("TestTitle", info[2])
        assertEquals("30.10.2025", info[3])  // date
        assertEquals("20:00:00", info[4])     // time
        assertEquals("00:15:00", info[5])     // duration
        assertEquals("150", info[6])          // size
        assertEquals("Test Description", info[7])  // description
        assertEquals("https://example.com/video.mp4", info[8])  // url
        assertEquals("https://example.com/small.mp4", info[9])  // urlSmall
        assertEquals("1730318400", info[10])  // dateL
    }

    @Test
    fun `clean clears all parsed data`() {
        val testFile = createTestFile("test_clean.json")
        testFile.writeText("""
            {
              "Filmliste": ["30.10.2025", "21:00", "abc123", "MediathekView", "1.0"],
              "X": ["ARD", "News", "Title", "", "", "", "", "", "", "", "", "", "", "", "", "", "1000000", "", "", "false"]
            }
        """.trimIndent())

        parser.parseFile(testFile.absolutePath)
        assertEquals(1, parser.getNumberOfLines())

        parser.clean()
        assertEquals(0, parser.getNumberOfLines())
        assertTrue(parser.getMediaList().isEmpty())
    }

    @Test
    fun `parseFile with real Filmliste-akt respects 100K entry limit`() {
        // Get the test resource file
        val resourceUrl = javaClass.classLoader?.getResource("Filmliste-akt")
        assertNotNull("Filmliste-akt test resource not found", resourceUrl)

        val testFile = File(resourceUrl!!.toURI())
        assertTrue("Filmliste-akt file should exist", testFile.exists())
        assertTrue("Filmliste-akt file should be readable", testFile.canRead())

        val result = parser.parseFile(testFile.absolutePath)

        // Should parse successfully
        assertEquals(0, result)

        // Should stop at 100,000 entries
        val entryCount = parser.getNumberOfLines()
        assertTrue("Should have parsed entries", entryCount > 0)
        assertTrue("Should stop at 100K entries, got $entryCount", entryCount <= 100_000)

        println("Successfully parsed $entryCount entries from real Filmliste-akt")
    }

    @Test
    fun `parseFile with real Filmliste-akt extracts complete metadata`() {
        // Get the test resource file
        val resourceUrl = javaClass.classLoader?.getResource("Filmliste-akt")
        assertNotNull("Filmliste-akt test resource not found", resourceUrl)

        val testFile = File(resourceUrl!!.toURI())
        parser.parseFile(testFile.absolutePath)

        val mediaList = parser.getMediaList()
        assertTrue("Should have parsed media entries", mediaList.isNotEmpty())

        // Get the first entry (should be 3Sat)
        val firstEntry = mediaList[0]

        // Verify all fields are populated
        assertEquals("3Sat", firstEntry.channel)
        assertEquals("...von oben", firstEntry.theme)
        assertEquals("Island von oben", firstEntry.title)

        // Verify date/time/duration fields are populated
        assertNotNull("Date should be populated", firstEntry.date)
        assertTrue("Date should not be empty", firstEntry.date.isNotEmpty())
        assertEquals("30.11.2023", firstEntry.date)

        assertNotNull("Time should be populated", firstEntry.time)
        assertTrue("Time should not be empty", firstEntry.time.isNotEmpty())
        assertEquals("14:05:00", firstEntry.time)

        assertNotNull("Duration should be populated", firstEntry.duration)
        assertTrue("Duration should not be empty", firstEntry.duration.isNotEmpty())
        assertEquals("00:43:27", firstEntry.duration)

        // Verify size field is populated
        assertNotNull("Size should be populated", firstEntry.sizeMB)
        assertTrue("Size should not be empty", firstEntry.sizeMB.isNotEmpty())
        assertEquals("864", firstEntry.sizeMB)

        // Verify description
        assertTrue("Description should be populated", firstEntry.description.isNotEmpty())
        assertTrue("Description should contain expected text",
            firstEntry.description.contains("Island: eine Insel"))

        // Verify URLs
        assertTrue("URL should be populated", firstEntry.url.isNotEmpty())
        assertTrue("URL should be valid", firstEntry.url.startsWith("https://"))

        // Verify geo restriction
        assertEquals("DE-AT-CH", firstEntry.geo)

        // Verify isNew flag
        assertFalse("isNew should be false for first entry", firstEntry.isNew)

        println("First entry verified: ${firstEntry.channel} - ${firstEntry.title}")
    }

    @Test
    fun `getMediaInfo with real data returns 11-element array with all fields`() {
        // Get the test resource file
        val resourceUrl = javaClass.classLoader?.getResource("Filmliste-akt")
        assertNotNull("Filmliste-akt test resource not found", resourceUrl)

        val testFile = File(resourceUrl!!.toURI())
        parser.parseFile(testFile.absolutePath)

        // Get info for the first entry
        val info = parser.getMediaInfo("3Sat", "...von oben", "Island von oben")

        assertNotNull("Should find the entry", info)
        assertEquals("Info array should have 11 elements", 11, info!!.size)

        // Verify all array elements
        assertEquals("3Sat", info[0])                    // channel
        assertEquals("...von oben", info[1])             // theme
        assertEquals("Island von oben", info[2])         // title
        assertEquals("30.11.2023", info[3])              // date - NOW POPULATED!
        assertEquals("14:05:00", info[4])                // time - NOW POPULATED!
        assertEquals("00:43:27", info[5])                // duration - NOW POPULATED!
        assertEquals("864", info[6])                     // sizeMB - NOW POPULATED!
        assertTrue(info[7].contains("Island: eine Insel")) // description
        assertTrue(info[8].startsWith("https://"))       // url
        assertTrue(info[9].isNotEmpty())                 // urlSmall (may be empty for some entries)
        assertEquals("1701349500", info[10])             // dateL timestamp

        println("getMediaInfo returns correct 11-element array:")
        println("  [0] channel: ${info[0]}")
        println("  [1] theme: ${info[1]}")
        println("  [2] title: ${info[2]}")
        println("  [3] date: ${info[3]}")
        println("  [4] time: ${info[4]}")
        println("  [5] duration: ${info[5]}")
        println("  [6] size: ${info[6]}")
        println("  [7] description: ${info[7].take(50)}...")
        println("  [8] url: ${info[8].take(50)}...")
        println("  [9] urlSmall: ${info[9].take(50)}...")
        println("  [10] dateL: ${info[10]}")
    }

    @Test
    fun `parseFile with real data handles entry inheritance correctly`() {
        // Get the test resource file
        val resourceUrl = javaClass.classLoader?.getResource("Filmliste-akt")
        assertNotNull("Filmliste-akt test resource not found", resourceUrl)

        val testFile = File(resourceUrl!!.toURI())
        parser.parseFile(testFile.absolutePath)

        val mediaList = parser.getMediaList()
        assertTrue("Should have multiple entries", mediaList.size >= 2)

        // Second entry has empty channel/theme, should inherit from first
        val secondEntry = mediaList[1]
        assertEquals("Should inherit channel from first entry", "3Sat", secondEntry.channel)
        assertEquals("Should inherit theme from first entry", "...von oben", secondEntry.theme)
        assertEquals("Südafrikas Osten von oben", secondEntry.title)

        // But should have its own date/time/duration
        assertEquals("19.09.2025", secondEntry.date)
        assertEquals("16:46:00", secondEntry.time)
        assertEquals("00:43:17", secondEntry.duration)
        assertEquals("882", secondEntry.sizeMB)

        println("Entry inheritance working: ${secondEntry.channel} - ${secondEntry.title}")
    }

    @Test
    fun `parseFile performance with real data completes in reasonable time`() {
        val resourceUrl = javaClass.classLoader?.getResource("Filmliste-akt")
        assertNotNull("Filmliste-akt test resource not found", resourceUrl)

        val testFile = File(resourceUrl!!.toURI())

        val startTime = System.currentTimeMillis()
        val result = parser.parseFile(testFile.absolutePath)
        val endTime = System.currentTimeMillis()

        val duration = endTime - startTime
        val entryCount = parser.getNumberOfLines()

        assertEquals(0, result)
        assertTrue("Should parse some entries", entryCount > 0)

        println("Performance test:")
        println("  Parsed $entryCount entries in ${duration}ms")
        println("  Average: ${duration.toDouble() / entryCount} ms per entry")

        // Should complete in under 30 seconds for 100K entries
        assertTrue("Parsing should complete in reasonable time (< 30s), took ${duration}ms",
            duration < 30_000)
    }
}
