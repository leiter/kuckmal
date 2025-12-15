package cut.the.crap.shared.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Desktop-specific database builder
 */
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(getAppDataDirectory(), "mediathekview.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}

/**
 * Get the application data directory for the current OS
 */
private fun getAppDataDirectory(): File {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    val appDir = when {
        os.contains("win") -> {
            // Windows: %APPDATA%\MediathekView
            val appData = System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming"
            File(appData, "MediathekView")
        }
        os.contains("mac") -> {
            // macOS: ~/Library/Application Support/MediathekView
            File(userHome, "Library/Application Support/MediathekView")
        }
        else -> {
            // Linux/Unix: ~/.local/share/mediathekview
            val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
            File(xdgDataHome, "mediathekview")
        }
    }

    // Create directory if it doesn't exist
    if (!appDir.exists()) {
        appDir.mkdirs()
    }

    return appDir
}
