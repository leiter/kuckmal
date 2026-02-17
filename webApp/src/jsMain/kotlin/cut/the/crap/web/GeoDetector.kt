package cut.the.crap.web

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Response
import kotlin.js.json

/**
 * Utility to detect user's geographic location using free IP geolocation API.
 * Uses ip-api.com which is free for non-commercial use (45 req/min limit).
 */
object GeoDetector {

    private const val API_URL = "http://ip-api.com/json/?fields=status,countryCode"

    // Cached country code (null = not yet detected, empty = detection failed)
    private var cachedCountryCode: String? = null

    /**
     * Get the user's country code (e.g., "DE", "AT", "CH").
     * Returns cached value if available, otherwise fetches from API.
     * Returns null if detection fails.
     */
    suspend fun getUserCountryCode(): String? {
        // Return cached value if available
        cachedCountryCode?.let {
            return it.ifEmpty { null }
        }

        return try {
            val response = window.fetch(API_URL).await<Response>()
            val text = response.text().await<String>()
            val result = JSON.parse<dynamic>(text)

            if (result.status == "success" && result.countryCode != null) {
                val countryCode = result.countryCode as String
                cachedCountryCode = countryCode
                countryCode
            } else {
                cachedCountryCode = ""
                null
            }
        } catch (e: Exception) {
            console.log("[GeoDetector] Failed to detect country: ${e.message}")
            cachedCountryCode = ""
            null
        }
    }

    /**
     * Clear cached country code (e.g., for testing or after VPN change).
     */
    fun clearCache() {
        cachedCountryCode = null
    }
}
