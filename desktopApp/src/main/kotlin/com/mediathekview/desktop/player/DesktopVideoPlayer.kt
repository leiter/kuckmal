package com.mediathekview.desktop.player

import java.awt.Desktop
import java.io.File
import java.net.URI

/**
 * Desktop video player that tries various players in order of preference
 */
object DesktopVideoPlayer {

    /**
     * Play a video URL using the best available player
     */
    fun play(url: String, title: String = ""): PlayResult {
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

    private fun tryVlc(url: String, title: String): PlayResult {
        val vlcPaths = listOf(
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

        for (vlcPath in vlcPaths) {
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
