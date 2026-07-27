package cut.the.crap.android.util

import android.util.Log

/**
 * Utility class for Kuckmal URL processing
 *
 * Kuckmal uses a space-saving format for URLs where low/small quality URLs
 * are stored in pipe-delimited format: "prefixLength|urlPath"
 */
object MediaUrlUtils {

    private const val TAG = "MediaUrlUtils"

    /**
     * Clean media URL by removing pipe-delimited prefix and adding protocol if needed
     * Kuckmal URLs are stored in format: "index|domain/path" or just "domain/path"
     *
     * @param url The URL to clean
     * @return Cleaned URL with protocol
     */
    @JvmStatic
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
                // Default to https for modern streaming services
                "https://$urlWithoutPrefix"
            }
            else -> ""
        }
    }

    /**
     * Reconstruct URL from pipe-delimited format
     *
     * Kuckmal uses a space-saving format where low/small quality URLs are stored as:
     * "prefixLength|urlPath"
     *
     * Example:
     *   High quality: https://rodlzdf-a.akamaihd.net/path/to/video_high.mp4
     *   Low quality:  8|rodlzdf-a.akamaihd.net/path/to/video_low.mp4
     *
     * The number (8) means: "Take the first 8 characters from the base URL and append the path"
     * Result: https:// + rodlzdf-a.akamaihd.net/path/to/video_low.mp4
     *
     * @param targetUrl The URL to reconstruct (may be pipe-delimited)
     * @param baseUrl The base URL to use for prefix extraction (usually the high quality URL)
     * @return Fully reconstructed URL with protocol
     */
    @JvmStatic
    fun reconstructUrl(targetUrl: String, baseUrl: String): String {
        if (targetUrl.isEmpty()) return ""

        // Check if URL is in pipe-delimited format
        if (targetUrl.contains("|")) {
            try {
                val parts = targetUrl.split("|", limit = 2)
                if (parts.size != 2) {
                    Log.w(TAG, "Invalid pipe-delimited URL format: $targetUrl")
                    return cleanMediaUrl(targetUrl)
                }

                val prefixLength = parts[0].toIntOrNull()
                if (prefixLength == null || prefixLength <= 0) {
                    Log.w(TAG, "Invalid prefix length in URL: $targetUrl")
                    return cleanMediaUrl(targetUrl)
                }

                // Clean the base URL to ensure it has protocol
                val baseCleaned = cleanMediaUrl(baseUrl)
                if (baseCleaned.length < prefixLength) {
                    Log.w(TAG, "Base URL too short for prefix length $prefixLength: $baseCleaned")
                    return cleanMediaUrl(targetUrl)
                }

                // Extract prefix from base URL and append path
                val prefix = baseCleaned.substring(0, prefixLength)
                val reconstructed = prefix + parts[1]

                Log.d(TAG, "Reconstructed URL: $targetUrl -> $reconstructed (using base: $baseCleaned)")
                return reconstructed

            } catch (e: Exception) {
                Log.e(TAG, "Error reconstructing URL: $targetUrl", e)
                return cleanMediaUrl(targetUrl)
            }
        }

        // Not pipe-delimited, just clean normally
        return cleanMediaUrl(targetUrl)
    }

    /**
     * Whether the URL points at an HLS stream rather than a single media file.
     *
     * HLS URLs resolve to a playlist of .ts segments, so DownloadManager can only
     * fetch the few-KB manifest — never the video. Playback is unaffected; only
     * downloading has to treat these differently.
     *
     * Broadcaster URLs may contain several extensions at once (SRF serves
     * ".../name.mp4.csmil/index-f4-v1-a1.m3u8"), so this looks for the .m3u8
     * marker anywhere rather than at the end.
     */
    @JvmStatic
    fun isHlsStream(url: String): Boolean = url.contains(".m3u8", ignoreCase = true)

    /**
     * File extension to save [url] under.
     *
     * .m3u8 is checked before .mp4 because HLS URLs frequently contain both and
     * the stream marker is the more specific signal.
     */
    @JvmStatic
    fun downloadFileExtension(url: String): String = when {
        isHlsStream(url) -> ".m3u8"
        url.contains(".mp4", ignoreCase = true) -> ".mp4"
        url.contains(".webm", ignoreCase = true) -> ".webm"
        else -> ".mp4"
    }
}
