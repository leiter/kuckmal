package com.mediathekview.desktop.util

/**
 * Utility class for MediathekView URL processing
 *
 * MediathekView uses a space-saving format for URLs where low/small/HD quality URLs
 * are stored in pipe-delimited format: "prefixLength|urlPath"
 *
 * Example:
 *   Main URL:     https://rodlzdf-a.akamaihd.net/path/to/video.mp4
 *   Small URL:    135|video_low.mp4
 *
 * The number (135) means: "Take the first 135 characters from the main URL and append the rest"
 */
object MediaUrlUtils {

    /**
     * Clean media URL by removing pipe-delimited prefix and adding protocol if needed
     */
    fun cleanMediaUrl(url: String): String {
        if (url.isEmpty()) return ""

        // Remove pipe-delimited prefix if present (e.g., "8|domain/path" -> "domain/path")
        val urlWithoutPrefix = if (url.contains("|")) {
            url.substring(url.indexOf("|") + 1)
        } else {
            url
        }

        // Add protocol if missing
        return when {
            urlWithoutPrefix.startsWith("http://") || urlWithoutPrefix.startsWith("https://") -> {
                urlWithoutPrefix
            }
            urlWithoutPrefix.isNotEmpty() -> {
                "https://$urlWithoutPrefix"
            }
            else -> ""
        }
    }

    /**
     * Reconstruct URL from pipe-delimited format
     *
     * @param targetUrl The URL to reconstruct (may be pipe-delimited like "135|video_low.mp4")
     * @param baseUrl The base URL to use for prefix extraction (usually the main URL)
     * @return Fully reconstructed URL with protocol
     */
    fun reconstructUrl(targetUrl: String, baseUrl: String): String {
        if (targetUrl.isEmpty()) return ""

        // Check if URL is in pipe-delimited format
        if (targetUrl.contains("|")) {
            try {
                val parts = targetUrl.split("|", limit = 2)
                if (parts.size != 2) {
                    println("MediaUrlUtils: Invalid pipe-delimited URL format: $targetUrl")
                    return cleanMediaUrl(targetUrl)
                }

                val prefixLength = parts[0].toIntOrNull()
                if (prefixLength == null || prefixLength <= 0) {
                    println("MediaUrlUtils: Invalid prefix length in URL: $targetUrl")
                    return cleanMediaUrl(targetUrl)
                }

                // Clean the base URL to ensure it has protocol
                val baseCleaned = cleanMediaUrl(baseUrl)
                if (baseCleaned.length < prefixLength) {
                    println("MediaUrlUtils: Base URL too short for prefix length $prefixLength: $baseCleaned")
                    return cleanMediaUrl(targetUrl)
                }

                // Extract prefix from base URL and append path
                val prefix = baseCleaned.substring(0, prefixLength)
                val reconstructed = prefix + parts[1]

                println("MediaUrlUtils: Reconstructed URL: $targetUrl -> $reconstructed")
                return reconstructed

            } catch (e: Exception) {
                println("MediaUrlUtils: Error reconstructing URL: $targetUrl - ${e.message}")
                return cleanMediaUrl(targetUrl)
            }
        }

        // Not pipe-delimited, just clean normally
        return cleanMediaUrl(targetUrl)
    }

    /**
     * Get the best available URL for a media entry
     *
     * @param mainUrl The main/default URL
     * @param targetUrl The specific quality URL (smallUrl or hdUrl)
     * @return The resolved URL ready for playback/download
     */
    fun resolveUrl(mainUrl: String, targetUrl: String): String {
        return if (targetUrl.isNotEmpty()) {
            reconstructUrl(targetUrl, mainUrl)
        } else {
            cleanMediaUrl(mainUrl)
        }
    }
}
