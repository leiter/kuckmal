import Foundation
import SharedTvos

/// Kotlin/Swift interop utilities for tvOS
/// This file bridges Kotlin Multiplatform types to Swift

// MARK: - Koin Initialization

/// Manager for Kotlin dependency injection
enum KotlinDI {
    private static var isInitialized = false

    /// Initialize Koin for Kotlin dependencies
    /// Call this once at app startup
    static func initialize() {
        guard !isInitialized else { return }
        KoinHelperKt.doInitKoin()
        isInitialized = true
    }

    /// Get the MediaRepository instance from Koin
    static func getRepository() -> MediaRepository {
        return KoinHelperKt.getMediaRepository()
    }
}

// MARK: - MediaEntry Conversion

/// Extension to convert Kotlin MediaEntry to Swift MediaEntry
extension SharedTvos.MediaEntry {
    /// Convert to the local Swift MediaEntry type used in views
    func toSwiftMediaEntry() -> MediaEntry {
        return MediaEntry(
            id: self.id,
            channel: self.channel,
            theme: self.theme,
            title: self.title,
            date: self.date,
            time: self.time,
            duration: self.duration,
            sizeMB: self.sizeMB,
            description: self.description_,
            url: self.url,
            smallUrl: self.smallUrl,
            hdUrl: self.hdUrl,
            subtitleUrl: self.subtitleUrl
        )
    }
}

// MARK: - Channel Conversion

/// Extension to convert Kotlin Channel to Swift Channel
extension SharedTvos.Channel {
    /// Convert to the local Swift Channel type used in views
    func toSwiftChannel() -> Channel {
        return Channel(
            name: self.name,
            displayName: self.displayName,
            brandColor: ChannelColors.brandColor(for: self.name)
        )
    }
}

// MARK: - Sample Data Bridge

/// Bridge to Kotlin SampleData for convenience
enum KotlinSampleData {
    /// Get all sample channels as Swift Channel objects
    static var channels: [Channel] {
        return SampleData.companion.sampleChannels.map { $0.toSwiftChannel() }
    }

    /// Get themes for a specific channel
    static func themes(for channel: String) -> [String] {
        return SampleData.companion.getThemesForChannel(channel: channel)
    }

    /// Get titles for a specific theme
    static func titles(for theme: String) -> [String] {
        return SampleData.companion.getTitlesForTheme(theme: theme)
    }
}
