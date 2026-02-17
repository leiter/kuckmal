package cut.the.crap.desktop.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * Simple preferences storage for desktop using Java Properties.
 * Stores preferences in the app data directory.
 */
object DesktopPreferences {

    private val properties = Properties()
    private var preferencesFile: File? = null
    private var initialized = false

    /**
     * Initialize preferences with the app data path.
     * Must be called before using other methods.
     */
    fun initialize(appDataPath: String) {
        if (initialized) return

        preferencesFile = File(appDataPath, "preferences.properties")
        loadPreferences()
        initialized = true
    }

    private fun loadPreferences() {
        val file = preferencesFile ?: return
        if (file.exists()) {
            try {
                FileInputStream(file).use { fis ->
                    properties.load(fis)
                }
            } catch (e: Exception) {
                println("Error loading preferences: ${e.message}")
            }
        }
    }

    private fun savePreferences() {
        val file = preferencesFile ?: return
        try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { fos ->
                properties.store(fos, "Kuckmal Desktop Preferences")
            }
        } catch (e: Exception) {
            println("Error saving preferences: ${e.message}")
        }
    }

    /**
     * Get a long value from preferences.
     * @param key The preference key
     * @param default The default value if key doesn't exist
     * @return The stored value or default
     */
    fun getLong(key: String, default: Long): Long {
        return properties.getProperty(key)?.toLongOrNull() ?: default
    }

    /**
     * Set a long value in preferences.
     * @param key The preference key
     * @param value The value to store
     */
    fun setLong(key: String, value: Long) {
        properties.setProperty(key, value.toString())
        savePreferences()
    }

    /**
     * Get a string value from preferences.
     * @param key The preference key
     * @param default The default value if key doesn't exist
     * @return The stored value or default
     */
    fun getString(key: String, default: String): String {
        return properties.getProperty(key) ?: default
    }

    /**
     * Set a string value in preferences.
     * @param key The preference key
     * @param value The value to store
     */
    fun setString(key: String, value: String) {
        properties.setProperty(key, value)
        savePreferences()
    }
}
