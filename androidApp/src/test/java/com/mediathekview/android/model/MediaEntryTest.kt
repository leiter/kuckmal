package com.mediathekview.android.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MediaEntry data class
 */
class MediaEntryTest {

    @Test
    fun `fromArray creates MediaEntry with all fields`() {
        val arr = arrayOf(
            "ARD",              // 0: channel
            "Nachrichten",      // 1: theme
            "Tagesschau",       // 2: title
            "30.10.2025",       // 3: date
            "20:00:00",         // 4: time
            "00:15:00",         // 5: duration
            "150",              // 6: sizeMB
            "Die Nachrichten",  // 7: description
            "https://example.com/video.mp4",  // 8: url
            "https://example.com",  // 9: website
            "https://example.com/sub.vtt",  // 10: subtitleUrl
            "",                 // 11: (unused)
            "https://example.com/small.mp4",  // 12: urlSmall
            "",                 // 13: (unused)
            "https://example.com/hd.mp4",  // 14: urlHd
            "",                 // 15: (unused)
            "1730318400",       // 16: dateL (timestamp)
            "",                 // 17: (unused)
            "DE",               // 18: geo
            "true"              // 19: isNew
        )

        val entry = MediaEntry.fromArray(arr, null)

        assertEquals("ARD", entry.channel)
        assertEquals("Nachrichten", entry.theme)
        assertEquals("Tagesschau", entry.title)
        assertEquals("30.10.2025", entry.date)
        assertEquals("20:00:00", entry.time)
        assertEquals("00:15:00", entry.duration)
        assertEquals("150", entry.sizeMB)
        assertEquals("Die Nachrichten", entry.description)
        assertEquals("https://example.com/video.mp4", entry.url)
        assertEquals("https://example.com", entry.website)
        assertEquals("https://example.com/sub.vtt", entry.subtitleUrl)
        assertEquals("https://example.com/small.mp4", entry.urlSmall)
        assertEquals("https://example.com/hd.mp4", entry.urlHd)
        assertEquals(1730318400L, entry.dateL)
        assertEquals("DE", entry.geo)
        assertTrue(entry.isNew)
        assertTrue(entry.inTimePeriod)
    }

    @Test
    fun `fromArray inherits channel and theme from previous entry when empty`() {
        val previous = MediaEntry(
            channel = "ZDF",
            theme = "Sport",
            title = "Previous Title"
        )

        val arr = arrayOf(
            "",                 // 0: channel (empty, should inherit)
            "",                 // 1: theme (empty, should inherit)
            "New Title",        // 2: title
            "30.10.2025",       // 3: date
            "21:00:00",         // 4: time
            "00:30:00"          // 5: duration
        )

        val entry = MediaEntry.fromArray(arr, previous)

        assertEquals("ZDF", entry.channel)  // Inherited
        assertEquals("Sport", entry.theme)  // Inherited
        assertEquals("New Title", entry.title)
    }

    @Test
    fun `fromArray handles minimal data`() {
        val arr = arrayOf(
            "ARD",              // 0: channel
            "News",             // 1: theme
            "Title"             // 2: title
        )

        val entry = MediaEntry.fromArray(arr, null)

        assertEquals("ARD", entry.channel)
        assertEquals("News", entry.theme)
        assertEquals("Title", entry.title)
        assertEquals("", entry.date)
        assertEquals("", entry.time)
        assertEquals("", entry.duration)
        assertEquals(0L, entry.dateL)
        assertFalse(entry.isNew)
    }

    @Test
    fun `fromArray handles invalid dateL`() {
        val arr = arrayOf(
            "ARD", "News", "Title",
            "", "", "", "", "", "", "", "", "", "", "", "", "",
            "invalid_number"    // 16: invalid dateL
        )

        val entry = MediaEntry.fromArray(arr, null)

        assertEquals(0L, entry.dateL)  // Should default to 0
    }

    @Test
    fun `fromArray handles isNew variations`() {
        // Test "true"
        val arr1 = arrayOf(
            "ARD", "News", "Title",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "true"  // 19: isNew
        )
        assertTrue(MediaEntry.fromArray(arr1, null).isNew)

        // Test "True" (case insensitive)
        val arr2 = arrayOf(
            "ARD", "News", "Title",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "True"
        )
        assertTrue(MediaEntry.fromArray(arr2, null).isNew)

        // Test "false"
        val arr3 = arrayOf(
            "ARD", "News", "Title",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "false"
        )
        assertFalse(MediaEntry.fromArray(arr3, null).isNew)

        // Test missing
        val arr4 = arrayOf("ARD", "News", "Title")
        assertFalse(MediaEntry.fromArray(arr4, null).isNew)
    }

    @Test
    fun `toDatabaseEntry converts all fields correctly`() {
        val modelEntry = MediaEntry(
            channel = "ARD",
            theme = "Nachrichten",
            title = "Tagesschau",
            date = "30.10.2025",
            time = "20:00:00",
            duration = "00:15:00",
            sizeMB = "150",
            description = "Die Nachrichten",
            url = "https://example.com/video.mp4",
            website = "https://example.com",
            subtitleUrl = "https://example.com/sub.vtt",
            urlSmall = "https://example.com/small.mp4",
            urlHd = "https://example.com/hd.mp4",
            dateL = 1730318400L,
            geo = "DE",
            isNew = true,
            inTimePeriod = true
        )

        val dbEntry = modelEntry.toDatabaseEntry()

        assertEquals("ARD", dbEntry.channel)
        assertEquals("Nachrichten", dbEntry.theme)
        assertEquals("Tagesschau", dbEntry.title)
        assertEquals("30.10.2025", dbEntry.date)
        assertEquals("20:00:00", dbEntry.time)
        assertEquals("00:15:00", dbEntry.duration)
        assertEquals("150", dbEntry.sizeMB)
        assertEquals("Die Nachrichten", dbEntry.description)
        assertEquals("https://example.com/video.mp4", dbEntry.url)
        assertEquals("https://example.com", dbEntry.website)
        assertEquals("https://example.com/sub.vtt", dbEntry.subtitleUrl)
        assertEquals("https://example.com/small.mp4", dbEntry.smallUrl)
        assertEquals("https://example.com/hd.mp4", dbEntry.hdUrl)
        assertEquals(1730318400L, dbEntry.timestamp)
        assertEquals("DE", dbEntry.geo)
        assertTrue(dbEntry.isNew)
    }

    @Test
    fun `MediaEntry default values are correct`() {
        val entry = MediaEntry()

        assertEquals("", entry.channel)
        assertEquals("", entry.theme)
        assertEquals("", entry.title)
        assertEquals("", entry.date)
        assertEquals("", entry.time)
        assertEquals("", entry.duration)
        assertEquals("", entry.sizeMB)
        assertEquals("", entry.description)
        assertEquals("", entry.url)
        assertEquals("", entry.website)
        assertEquals("", entry.subtitleUrl)
        assertEquals("", entry.urlSmall)
        assertEquals("", entry.urlHd)
        assertEquals(0L, entry.dateL)
        assertEquals("", entry.geo)
        assertFalse(entry.isNew)
        assertTrue(entry.inTimePeriod)
    }
}
