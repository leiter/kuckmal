import Foundation
import SharedTvos
import Combine
import SwiftUI

/// ViewModel for tvOS that bridges Kotlin repository to SwiftUI
@MainActor
class TvOSViewModel: ObservableObject {
    @Published var channels: [Channel] = []
    @Published var themes: [String] = []
    @Published var titles: [String] = []
    @Published var selectedChannel: Channel?
    @Published var selectedTheme: String?
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?

    private var useKotlin = true

    init() {
        print("[TvOSViewModel] init starting")
        loadChannels()
        print("[TvOSViewModel] init complete - channels: \(channels.count), themes: \(themes.count)")
    }

    // MARK: - Channel Loading

    func loadChannels() {
        print("[TvOSViewModel] loadChannels called")

        do {
            // Try Kotlin SampleData
            let kotlinChannels = SampleData.shared.sampleChannels
            print("[TvOSViewModel] Got \(kotlinChannels.count) kotlin channels")

            channels = kotlinChannels.map { kotlinChannel in
                Channel(
                    name: kotlinChannel.name,
                    displayName: kotlinChannel.displayName,
                    brandColor: ChannelColors.brandColor(for: kotlinChannel.name)
                )
            }
            print("[TvOSViewModel] Mapped to \(channels.count) Swift channels")
        } catch {
            print("[TvOSViewModel] Kotlin error: \(error)")
            useKotlin = false
            loadFallbackChannels()
        }

        // Load themes
        loadAllThemes()
    }

    private func loadFallbackChannels() {
        print("[TvOSViewModel] Using fallback channels")
        channels = [
            Channel(name: "ARD", displayName: "ARD", brandColor: ChannelColors.brandColor(for: "ARD")),
            Channel(name: "ZDF", displayName: "ZDF", brandColor: ChannelColors.brandColor(for: "ZDF")),
            Channel(name: "3Sat", displayName: "3sat", brandColor: ChannelColors.brandColor(for: "3Sat")),
            Channel(name: "ARTE.DE", displayName: "ARTE", brandColor: ChannelColors.brandColor(for: "ARTE.DE")),
            Channel(name: "BR", displayName: "BR", brandColor: ChannelColors.brandColor(for: "BR")),
            Channel(name: "NDR", displayName: "NDR", brandColor: ChannelColors.brandColor(for: "NDR")),
            Channel(name: "WDR", displayName: "WDR", brandColor: ChannelColors.brandColor(for: "WDR")),
            Channel(name: "SWR", displayName: "SWR", brandColor: ChannelColors.brandColor(for: "SWR")),
            Channel(name: "PHOENIX", displayName: "phoenix", brandColor: ChannelColors.brandColor(for: "PHOENIX")),
        ]
    }

    // MARK: - Theme Loading

    func loadAllThemes() {
        print("[TvOSViewModel] loadAllThemes called")
        isLoading = true
        selectedTheme = nil
        titles = []

        if useKotlin {
            var allThemes: Set<String> = []
            for kotlinChannel in SampleData.shared.sampleChannels {
                let channelThemes = SampleData.shared.getThemesForChannel(channel: kotlinChannel.name)
                for theme in channelThemes {
                    allThemes.insert(theme)
                }
            }
            themes = Array(allThemes).sorted()
        } else {
            themes = ["Tagesschau", "Tatort", "Terra X", "Dokumentation", "Nachrichten", "Sport", "Kultur"]
        }

        print("[TvOSViewModel] Loaded \(themes.count) themes")
        isLoading = false
    }

    func loadThemes(for channelName: String) {
        print("[TvOSViewModel] loadThemes called for: \(channelName)")
        isLoading = true
        selectedTheme = nil
        titles = []

        if useKotlin {
            let kotlinThemes = SampleData.shared.getThemesForChannel(channel: channelName)
            themes = Array(kotlinThemes)
        } else {
            themes = ["\(channelName) Nachrichten", "\(channelName) Dokumentation", "\(channelName) Sport", "\(channelName) Kultur"]
        }

        print("[TvOSViewModel] Loaded \(themes.count) themes for \(channelName)")
        isLoading = false
    }

    // MARK: - Title Loading

    func loadTitles(for theme: String) {
        print("[TvOSViewModel] loadTitles called for: \(theme)")
        isLoading = true

        if useKotlin {
            let kotlinTitles = SampleData.shared.getTitlesForTheme(theme: theme)
            titles = Array(kotlinTitles)
        } else {
            titles = ["\(theme) - Folge 1", "\(theme) - Folge 2", "\(theme) - Spezial", "\(theme) - Best of"]
        }

        print("[TvOSViewModel] Loaded \(titles.count) titles for \(theme)")
        isLoading = false
    }

    // MARK: - Media Entry Loading

    func loadMediaEntry(title: String) async -> SharedTvos.MediaEntry? {
        let channel = selectedChannel?.name ?? "ARD"
        let theme = selectedTheme ?? "Allgemein"
        let id = Int64(abs(title.hashValue % 1_000_000))

        return SharedTvos.MediaEntry(
            id: id,
            channel: channel,
            theme: theme,
            title: title,
            date: "25.07.2024",
            time: "20:15",
            duration: "45 Min",
            sizeMB: "750",
            description: "Beschreibung fuer \(title) im Thema \(theme) von \(channel). Eine spannende Sendung mit interessanten Inhalten.",
            url: "https://example.com/video/\(id).mp4",
            website: "https://www.\(channel.lowercased()).de",
            subtitleUrl: "",
            smallUrl: "https://example.com/video/\(id)_small.mp4",
            hdUrl: "https://example.com/video/\(id)_hd.mp4",
            timestamp: Int64(Date().timeIntervalSince1970),
            geo: "DE-AT-CH",
            isNew: false
        )
    }

    // MARK: - Navigation Actions

    func selectChannel(_ channel: Channel?) {
        print("[TvOSViewModel] selectChannel: \(channel?.name ?? "nil")")
        selectedChannel = channel
        if let channel = channel {
            loadThemes(for: channel.name)
        } else {
            loadAllThemes()
        }
    }

    func selectTheme(_ theme: String) {
        print("[TvOSViewModel] selectTheme: \(theme)")
        selectedTheme = theme
        loadTitles(for: theme)
    }

    func goBack() {
        print("[TvOSViewModel] goBack called")
        if selectedTheme != nil {
            selectedTheme = nil
            titles = []
        } else if selectedChannel != nil {
            selectedChannel = nil
            loadAllThemes()
        }
    }
}

// MARK: - Channel Colors

enum ChannelColors {
    static func brandColor(for channelName: String) -> Color {
        switch channelName {
        case "3Sat": return Color(red: 0.2, green: 0.2, blue: 0.2)
        case "ARD": return Color(red: 0.0, green: 0.3, blue: 0.6)
        case "ARTE.DE": return Color(red: 0.9, green: 0.3, blue: 0.1)
        case "BR": return Color(red: 0.0, green: 0.4, blue: 0.7)
        case "HR": return Color(red: 0.0, green: 0.35, blue: 0.65)
        case "KiKA": return Color(red: 0.0, green: 0.6, blue: 0.3)
        case "MDR": return Color(red: 0.0, green: 0.45, blue: 0.75)
        case "NDR": return Color(red: 0.0, green: 0.35, blue: 0.6)
        case "ORF": return Color(red: 0.7, green: 0.0, blue: 0.0)
        case "PHOENIX": return Color(red: 0.9, green: 0.5, blue: 0.0)
        case "RBB": return Color(red: 0.7, green: 0.0, blue: 0.2)
        case "SR": return Color(red: 0.0, green: 0.4, blue: 0.6)
        case "SRF": return Color(red: 0.8, green: 0.0, blue: 0.0)
        case "SWR": return Color(red: 0.0, green: 0.4, blue: 0.2)
        case "WDR": return Color(red: 0.0, green: 0.3, blue: 0.5)
        case "ZDF": return Color(red: 0.9, green: 0.4, blue: 0.0)
        case "ZDF-tivi": return Color(red: 0.9, green: 0.5, blue: 0.1)
        default: return Color(red: 0.3, green: 0.3, blue: 0.3)
        }
    }
}
