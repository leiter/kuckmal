package cut.the.crap.desktop.util

import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class to check for film list updates by comparing file size
 */
class DesktopUpdateChecker {
    companion object {
        private const val FILMLIST_URL = "https://liste.mediathekview.de/Filmliste-akt.xz"
    }

    sealed class UpdateCheckResult {
        data class UpdateAvailable(val contentLength: Long, val lastModified: String?) : UpdateCheckResult()
        object NoUpdateNeeded : UpdateCheckResult()
        data class CheckFailed(val error: String) : UpdateCheckResult()
    }

    fun checkForUpdate(currentSize: Long): UpdateCheckResult {
        return try {
            val connection = URL(FILMLIST_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return UpdateCheckResult.CheckFailed("Server returned HTTP $responseCode")
            }

            val serverSize = connection.contentLengthLong
            val lastModified = connection.getHeaderField("Last-Modified")

            connection.disconnect()

            if (serverSize > 0 && serverSize != currentSize) {
                UpdateCheckResult.UpdateAvailable(serverSize, lastModified)
            } else {
                UpdateCheckResult.NoUpdateNeeded
            }
        } catch (e: Exception) {
            UpdateCheckResult.CheckFailed(e.message ?: "Unknown error")
        }
    }
}
