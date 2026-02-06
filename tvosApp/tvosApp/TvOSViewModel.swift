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
    @Published var isOffline: Bool = false

    // Search state
    @Published var searchQuery: String = ""
    @Published var searchResults: [SearchResult] = []
    @Published var isSearching: Bool = false

    private let repository: MediaRepository

    init() {
        print("[TvOSViewModel] init starting")
        self.repository = KoinHelperKt.getMediaRepository()

        // Load initial data asynchronously
        Task {
            await loadChannels()
        }
        print("[TvOSViewModel] init complete")
    }

    // MARK: - Channel Loading

    func loadChannels() async {
        print("[TvOSViewModel] loadChannels called")
        isLoading = true
        isOffline = false
        errorMessage = nil

        do {
            // Fetch channels from repository (API)
            let channelNames = try await repository.getAllChannels()

            if channelNames.isEmpty {
                // Empty response likely means API issue
                throw NSError(domain: "TvOSViewModel", code: -1, userInfo: [NSLocalizedDescriptionKey: "Keine Verbindung zum Server"])
            }

            channels = channelNames.map { name in
                Channel(
                    name: name,
                    displayName: getDisplayName(for: name),
                    brandColor: ChannelColors.brandColor(for: name)
                )
            }
            print("[TvOSViewModel] Loaded \(channels.count) channels from API")
            isOffline = false

        } catch {
            print("[TvOSViewModel] loadChannels error: \(error)")
            isOffline = true
            errorMessage = "Keine Internetverbindung. Bitte pruefen Sie Ihre Netzwerkeinstellungen."
            channels = []
            themes = []
        }

        isLoading = false

        // Only load themes if we have channels (online)
        if !isOffline {
            await loadAllThemes()
        }
    }

    private func getDisplayName(for channelName: String) -> String {
        // Map channel names to display names
        switch channelName {
        case "3Sat": return "3sat"
        case "ARTE.DE": return "ARTE"
        case "PHOENIX": return "phoenix"
        case "ZDF-tivi": return "ZDF tivi"
        default: return channelName
        }
    }

    func retry() {
        print("[TvOSViewModel] retry called")
        Task {
            await loadChannels()
        }
    }

    // MARK: - Theme Loading

    func loadAllThemes() async {
        print("[TvOSViewModel] loadAllThemes called")
        isLoading = true
        selectedTheme = nil
        titles = []

        do {
            // Fetch all themes from repository
            let themeList = try await repository.getAllThemes(minTimestamp: 0, limit: 500, offset: 0)
            themes = themeList.sorted()
            print("[TvOSViewModel] Loaded \(themes.count) themes from API")
            isOffline = false
        } catch {
            print("[TvOSViewModel] loadAllThemes error: \(error)")
            isOffline = true
            errorMessage = "Keine Internetverbindung. Bitte pruefen Sie Ihre Netzwerkeinstellungen."
            themes = []
        }

        isLoading = false
    }

    func loadThemes(for channelName: String) async {
        print("[TvOSViewModel] loadThemes called for: \(channelName)")
        isLoading = true
        selectedTheme = nil
        titles = []

        do {
            // Fetch themes for specific channel from repository
            let themeList = try await repository.getThemesForChannel(channel: channelName, minTimestamp: 0, limit: 500, offset: 0)
            themes = themeList.sorted()
            print("[TvOSViewModel] Loaded \(themes.count) themes for \(channelName) from API")
            isOffline = false
        } catch {
            print("[TvOSViewModel] loadThemes error: \(error)")
            isOffline = true
            errorMessage = "Keine Internetverbindung. Bitte pruefen Sie Ihre Netzwerkeinstellungen."
            themes = []
        }

        isLoading = false
    }

    // MARK: - Title Loading

    func loadTitles(for theme: String) async {
        print("[TvOSViewModel] loadTitles called for: \(theme)")
        isLoading = true

        do {
            let titleList: [String]
            if let channel = selectedChannel {
                // Fetch titles for specific channel and theme
                titleList = try await repository.getTitlesForChannelAndTheme(
                    channel: channel.name,
                    theme: theme,
                    minTimestamp: 0,
                    limit: 200,
                    offset: 0
                )
            } else {
                // Fetch titles for theme across all channels
                titleList = try await repository.getTitlesForTheme(theme: theme, minTimestamp: 0)
            }
            titles = titleList
            print("[TvOSViewModel] Loaded \(titles.count) titles for \(theme) from API")
            isOffline = false
        } catch {
            print("[TvOSViewModel] loadTitles error: \(error)")
            isOffline = true
            errorMessage = "Keine Internetverbindung. Bitte pruefen Sie Ihre Netzwerkeinstellungen."
            titles = []
        }

        isLoading = false
    }

    // MARK: - Media Entry Loading

    func loadMediaEntry(title: String) async -> SharedTvos.MediaEntry? {
        let channel = selectedChannel?.name ?? ""
        let theme = selectedTheme ?? ""

        // Try to fetch from repository using search
        do {
            let results: [SharedTvos.MediaEntry]
            if !channel.isEmpty {
                results = try await repository.searchEntriesByChannel(channel: channel, query: title, limit: 10)
            } else {
                results = try await repository.searchEntries(query: title, limit: 10)
            }

            // Find exact title match (or close match)
            if let entry = results.first(where: { $0.title == title }) {
                print("[TvOSViewModel] loadMediaEntry: fetched '\(title)' from repository (exact match)")
                return entry
            } else if let entry = results.first {
                print("[TvOSViewModel] loadMediaEntry: fetched '\(title)' from repository (first result)")
                return entry
            }
        } catch {
            print("[TvOSViewModel] loadMediaEntry error: \(error)")
        }

        // Fallback: create placeholder entry if repository fails
        print("[TvOSViewModel] loadMediaEntry: using fallback for '\(title)'")
        let fallbackChannel = channel.isEmpty ? "ARD" : channel
        let fallbackTheme = theme.isEmpty ? "Allgemein" : theme
        let id = Int64(abs(title.hashValue % 1_000_000))

        return SharedTvos.MediaEntry(
            id: id,
            channel: fallbackChannel,
            theme: fallbackTheme,
            title: title,
            date: "25.07.2024",
            time: "20:15",
            duration: "45 Min",
            sizeMB: "750",
            description: "Beschreibung fuer \(title) im Thema \(fallbackTheme) von \(fallbackChannel). Eine spannende Sendung mit interessanten Inhalten.",
            url: "https://example.com/video/\(id).mp4",
            website: "https://www.\(fallbackChannel.lowercased()).de",
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
        Task {
            if let channel = channel {
                await loadThemes(for: channel.name)
            } else {
                await loadAllThemes()
            }
        }
    }

    func selectTheme(_ theme: String) {
        print("[TvOSViewModel] selectTheme: \(theme)")
        selectedTheme = theme
        Task {
            await loadTitles(for: theme)
        }
    }

    func goBack() {
        print("[TvOSViewModel] goBack called")
        if isSearching {
            clearSearch()
        } else if selectedTheme != nil {
            selectedTheme = nil
            titles = []
        } else if selectedChannel != nil {
            selectedChannel = nil
            Task {
                await loadAllThemes()
            }
        }
    }

    // MARK: - Search

    func search(query: String) {
        searchQuery = query
        guard query.count >= 2 else {
            searchResults = []
            isSearching = false
            return
        }

        print("[TvOSViewModel] search: \(query)")
        isSearching = true
        isLoading = true

        Task {
            do {
                let results: [SharedTvos.MediaEntry]
                if let channel = selectedChannel {
                    results = try await repository.searchEntriesByChannel(channel: channel.name, query: query, limit: 50)
                } else {
                    results = try await repository.searchEntries(query: query, limit: 50)
                }

                await MainActor.run {
                    searchResults = results.map { entry in
                        SearchResult(
                            id: entry.id,
                            title: entry.title,
                            theme: entry.theme,
                            channel: entry.channel,
                            date: entry.date
                        )
                    }
                    print("[TvOSViewModel] search found \(searchResults.count) results")
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    print("[TvOSViewModel] search error: \(error)")
                    errorMessage = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }

    func clearSearch() {
        print("[TvOSViewModel] clearSearch")
        searchQuery = ""
        searchResults = []
        isSearching = false
    }

    func selectSearchResult(_ result: SearchResult) {
        print("[TvOSViewModel] selectSearchResult: \(result.title)")
        // Navigate to the result
        if let channel = channels.first(where: { $0.name == result.channel }) {
            selectedChannel = channel
        }
        selectedTheme = result.theme
        Task {
            await loadTitles(for: result.theme)
        }
        clearSearch()
    }
}

// MARK: - Search Result Model

struct SearchResult: Identifiable {
    let id: Int64
    let title: String
    let theme: String
    let channel: String
    let date: String
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
