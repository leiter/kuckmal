package cut.the.crap.shared.util

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    suspend fun getUserCountryCode(httpClient: HttpClient): String? {
        // Return cached value if available
        cachedCountryCode?.let {
            return it.ifEmpty { null }
        }

        return try {
            val response: HttpResponse = httpClient.get(API_URL)
            val body = response.bodyAsText()

            val json = Json { ignoreUnknownKeys = true }
            val result = json.decodeFromString<GeoApiResponse>(body)

            if (result.status == "success" && result.countryCode.isNotEmpty()) {
                cachedCountryCode = result.countryCode
                result.countryCode
            } else {
                cachedCountryCode = ""
                null
            }
        } catch (e: Exception) {
            println("[GeoDetector] Failed to detect country: ${e.message}")
            cachedCountryCode = ""
            null
        }
    }

    /**
     * Check if user can access content with the given geo restriction.
     *
     * @param geoRestriction The content's geo restriction (e.g., "AT", "DE-AT-CH", "")
     * @param userCountry The user's country code (e.g., "DE")
     * @return true if content is accessible, false if blocked, null if no restriction
     */
    fun canAccessContent(geoRestriction: String, userCountry: String?): Boolean? {
        // No restriction = accessible everywhere
        if (geoRestriction.isEmpty()) return null

        // Unknown user country = can't determine
        if (userCountry == null) return null

        // Check if user's country is in the allowed list
        val allowedCountries = geoRestriction.split("-")
        return userCountry in allowedCountries
    }

    /**
     * Get warning message based on geo restriction and user's location.
     *
     * @param geoRestriction The content's geo restriction
     * @param userCountry The user's country code
     * @return Appropriate warning message, or null if no warning needed
     */
    fun getGeoWarningMessage(geoRestriction: String, userCountry: String?): String? {
        if (geoRestriction.isEmpty()) return null

        val canAccess = canAccessContent(geoRestriction, userCountry)

        return when {
            canAccess == true -> null  // User can access, no warning needed
            canAccess == false -> {
                // User cannot access - show strong warning
                val regionName = getRegionName(geoRestriction)
                "Dieser Inhalt ist nur in $regionName verfuegbar. " +
                    "Wiedergabe in Ihrem Land (${getCountryName(userCountry)}) moeglicherweise nicht moeglich."
            }
            else -> {
                // Unknown user location - show info only
                val regionName = getRegionName(geoRestriction)
                "Dieser Inhalt ist nur in $regionName verfuegbar."
            }
        }
    }

    /**
     * Check if content is likely blocked for the user.
     */
    fun isLikelyBlocked(geoRestriction: String, userCountry: String?): Boolean {
        return canAccessContent(geoRestriction, userCountry) == false
    }

    /**
     * Get human-readable region name for geo restriction code.
     */
    private fun getRegionName(geo: String): String {
        return when (geo) {
            "AT" -> "Oesterreich"
            "DE" -> "Deutschland"
            "CH" -> "der Schweiz"
            "DE-AT" -> "Deutschland und Oesterreich"
            "DE-CH" -> "Deutschland und der Schweiz"
            "AT-CH" -> "Oesterreich und der Schweiz"
            "DE-AT-CH" -> "Deutschland, Oesterreich und der Schweiz"
            else -> geo
        }
    }

    /**
     * Get human-readable country name.
     */
    private fun getCountryName(countryCode: String?): String {
        return when (countryCode) {
            "AT" -> "Oesterreich"
            "DE" -> "Deutschland"
            "CH" -> "Schweiz"
            "FR" -> "Frankreich"
            "IT" -> "Italien"
            "NL" -> "Niederlande"
            "BE" -> "Belgien"
            "PL" -> "Polen"
            "CZ" -> "Tschechien"
            "GB", "UK" -> "Grossbritannien"
            "US" -> "USA"
            else -> countryCode ?: "unbekannt"
        }
    }

    /**
     * Clear cached country code (e.g., for testing or after VPN change).
     */
    fun clearCache() {
        cachedCountryCode = null
    }

    @Serializable
    private data class GeoApiResponse(
        val status: String = "",
        val countryCode: String = ""
    )
}
