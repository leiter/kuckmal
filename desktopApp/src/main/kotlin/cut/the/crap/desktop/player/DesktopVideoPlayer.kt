package cut.the.crap.desktop.player

import cut.the.crap.desktop.util.DesktopPreferences
import java.awt.Desktop
import java.io.File
import java.net.URI

/**
 * Desktop video player that tries various players in order of preference
 */
object DesktopVideoPlayer {

    /**
     * Available video player options
     */
    enum class PlayerType(val displayName: String) {
        AUTO("Automatic (VLC → MPV → Browser)"),
        VLC("VLC"),
        MPV("MPV"),
        BROWSER("System Browser");

        companion object {
            fun fromString(value: String): PlayerType {
                return entries.find { it.name == value } ?: AUTO
            }
        }
    }

    private const val PREF_KEY_PLAYER = "preferred_video_player"

    /**
     * Get the currently preferred video player
     */
    fun getPreferredPlayer(): PlayerType {
        val saved = DesktopPreferences.getString(PREF_KEY_PLAYER, PlayerType.AUTO.name)
        return PlayerType.fromString(saved)
    }

    /**
     * Set the preferred video player
     */
    fun setPreferredPlayer(player: PlayerType) {
        DesktopPreferences.setString(PREF_KEY_PLAYER, player.name)
    }

    /**
     * Get list of available players on this system
     */
    fun getAvailablePlayers(): List<PlayerType> {
        val available = mutableListOf(PlayerType.AUTO)
        if (isVlcAvailable()) available.add(PlayerType.VLC)
        if (isMpvAvailable()) available.add(PlayerType.MPV)
        available.add(PlayerType.BROWSER)
        return available
    }

    /**
     * Play a video URL using the preferred or best available player
     */
    fun play(url: String, title: String = ""): PlayResult {
        return when (getPreferredPlayer()) {
            PlayerType.VLC -> {
                val result = tryVlc(url, title)
                if (result.success) result else playAuto(url, title)
            }
            PlayerType.MPV -> {
                val result = tryMpv(url, title)
                if (result.success) result else playAuto(url, title)
            }
            PlayerType.BROWSER -> {
                val result = tryDesktopBrowse(url)
                if (result.success) result else tryXdgOpen(url)
            }
            PlayerType.AUTO -> playAuto(url, title)
        }
    }

    /**
     * Automatic player selection: VLC → MPV → Browser → xdg-open
     */
    private fun playAuto(url: String, title: String): PlayResult {
        // Try VLC first (best for streaming)
        val vlcResult = tryVlc(url, title)
        if (vlcResult.success) return vlcResult

        // Try MPV (popular on Linux)
        val mpvResult = tryMpv(url, title)
        if (mpvResult.success) return mpvResult

        // Try system default for URLs
        val desktopResult = tryDesktopBrowse(url)
        if (desktopResult.success) return desktopResult

        // Last resort: xdg-open on Linux
        val xdgResult = tryXdgOpen(url)
        if (xdgResult.success) return xdgResult

        return PlayResult(false, "No suitable video player found. Please install VLC or MPV.")
    }

    private fun isVlcAvailable(): Boolean {
        val vlcPaths = getVlcPaths()
        if (vlcPaths.any { File(it).exists() }) return true
        return try {
            ProcessBuilder("which", "vlc").start().waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isMpvAvailable(): Boolean {
        val mpvPaths = listOf("/usr/bin/mpv", "/usr/local/bin/mpv", "/snap/bin/mpv")
        if (mpvPaths.any { File(it).exists() }) return true
        return try {
            ProcessBuilder("which", "mpv").start().waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun getVlcPaths(): List<String> = listOf(
        // Linux
        "/usr/bin/vlc",
        "/usr/local/bin/vlc",
        "/snap/bin/vlc",
        "/var/lib/flatpak/exports/bin/org.videolan.VLC",
        // macOS
        "/Applications/VLC.app/Contents/MacOS/VLC",
        // Windows
        "C:\\Program Files\\VideoLAN\\VLC\\vlc.exe",
        "C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe"
    )

    private fun tryVlc(url: String, title: String): PlayResult {
        for (vlcPath in getVlcPaths()) {
            if (File(vlcPath).exists()) {
                return try {
                    val args = mutableListOf(vlcPath, url)
                    if (title.isNotEmpty()) {
                        args.addAll(listOf("--meta-title", title))
                    }
                    ProcessBuilder(args).start()
                    PlayResult(true, "Playing in VLC")
                } catch (e: Exception) {
                    PlayResult(false, "VLC found but failed to launch: ${e.message}")
                }
            }
        }

        // Try vlc command directly (might be in PATH)
        return try {
            val args = mutableListOf("vlc", url)
            if (title.isNotEmpty()) {
                args.addAll(listOf("--meta-title", title))
            }
            ProcessBuilder(args).start()
            PlayResult(true, "Playing in VLC")
        } catch (e: Exception) {
            PlayResult(false, "VLC not found")
        }
    }

    private fun tryMpv(url: String, title: String): PlayResult {
        val mpvPaths = listOf(
            "/usr/bin/mpv",
            "/usr/local/bin/mpv",
            "/snap/bin/mpv"
        )

        for (mpvPath in mpvPaths) {
            if (File(mpvPath).exists()) {
                return try {
                    val args = mutableListOf(mpvPath, url)
                    if (title.isNotEmpty()) {
                        args.addAll(listOf("--title=$title"))
                    }
                    ProcessBuilder(args).start()
                    PlayResult(true, "Playing in MPV")
                } catch (e: Exception) {
                    PlayResult(false, "MPV found but failed to launch: ${e.message}")
                }
            }
        }

        // Try mpv command directly
        return try {
            val args = mutableListOf("mpv", url)
            if (title.isNotEmpty()) {
                args.addAll(listOf("--title=$title"))
            }
            ProcessBuilder(args).start()
            PlayResult(true, "Playing in MPV")
        } catch (e: Exception) {
            PlayResult(false, "MPV not found")
        }
    }

    private fun tryDesktopBrowse(url: String): PlayResult {
        return try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
                PlayResult(true, "Opening in default browser")
            } else {
                PlayResult(false, "Desktop not supported")
            }
        } catch (e: Exception) {
            PlayResult(false, "Failed to open in browser: ${e.message}")
        }
    }

    private fun tryXdgOpen(url: String): PlayResult {
        return try {
            ProcessBuilder("xdg-open", url).start()
            PlayResult(true, "Opening with xdg-open")
        } catch (e: Exception) {
            PlayResult(false, "xdg-open failed: ${e.message}")
        }
    }

    data class PlayResult(
        val success: Boolean,
        val message: String
    )
}
