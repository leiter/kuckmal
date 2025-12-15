package cut.the.crap.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Utility class for network connectivity checks
 */
object NetworkUtils {

    /**
     * Check if the device has an active internet connection
     * @param context Android context
     * @return true if connected to internet, false otherwise
     */
    @JvmStatic
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6.0 (API 23) and above
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            // For older Android versions
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.isConnected == true
        }
    }

    /**
     * Get a user-friendly description of the connection type
     * @param context Android context
     * @return String describing the connection type (WiFi, Mobile, etc.)
     */
    @JvmStatic
    fun getConnectionType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unknown"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "No connection"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "No connection"

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.typeName ?: "No connection"
        }
    }

    /**
     * Estimate download time based on file size and connection type
     * @param context Android context
     * @param fileSizeMB File size in megabytes
     * @return Estimated download time as a human-readable string
     */
    @JvmStatic
    fun estimateDownloadTime(context: Context, fileSizeMB: Double): String {
        val connectionType = getConnectionType(context)

        // Estimated speeds in Mbps (conservative estimates)
        val estimatedSpeedMbps = when (connectionType) {
            "WiFi" -> 20.0  // ~20 Mbps for typical WiFi
            "Ethernet" -> 50.0  // ~50 Mbps for wired
            "Mobile Data" -> 5.0  // ~5 Mbps for mobile data (conservative)
            else -> 5.0  // Default to conservative estimate
        }

        // Convert file size to megabits and calculate time in seconds
        val fileSizeMb = fileSizeMB * 8  // Convert MB to Mb
        val timeSeconds = (fileSizeMb / estimatedSpeedMbps).toInt()

        return when {
            timeSeconds < 60 -> "$timeSeconds seconds"
            timeSeconds < 3600 -> {
                val minutes = timeSeconds / 60
                val seconds = timeSeconds % 60
                if (seconds > 0) "$minutes min $seconds sec" else "$minutes min"
            }
            else -> {
                val hours = timeSeconds / 3600
                val minutes = (timeSeconds % 3600) / 60
                if (minutes > 0) "$hours hr $minutes min" else "$hours hr"
            }
        }
    }

    /**
     * Get file size info with connection type
     * @param context Android context
     * @param fileSizeMB File size in megabytes
     * @return Formatted string with file size and connection info
     */
    @JvmStatic
    fun getDownloadInfo(context: Context, fileSizeMB: Double): String {
        val connectionType = getConnectionType(context)
        return "File size: %.1f MB\nConnection: %s".format(fileSizeMB, connectionType)
    }
}
