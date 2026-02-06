import Foundation
import SharedTvos
import Combine

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

    private let repository: MediaRepository
    private var channelCollector: Kotlinx_coroutines_coreJob?
    private var themeCollector: Kotlinx_coroutines_coreJob?
    private var titleCollector: Kotlinx_coroutines_coreJob?

    init() {
        self.repository = KoinHelperKt.getMediaRepository()
        loadChannels()
    }

    // MARK: - Channel Loading

    func loadChannels() {
        // Create channels from Kotlin SampleData
        channels = SampleData.companion.sampleChannels.map { kotlinChannel in
            Channel(
                name: kotlinChannel.name,
                displayName: kotlinChannel.displayName,
                brandColor: ChannelColors.brandColor(for: kotlinChannel.name)
            )
        }

        // Load all themes initially
        loadAllThemes()
    }

    // MARK: - Theme Loading

    func loadAllThemes() {
        isLoading = true
        selectedTheme = nil
        titles = []

        Task {
            do {
                let themeFlow = repository.getAllThemesFlow(minTimestamp: 0, limit: 100, offset: 0)
                themes = try await collectStrings(from: themeFlow)
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    func loadThemes(for channelName: String) {
        isLoading = true
        selectedTheme = nil
        titles = []

        Task {
            do {
                let themeFlow = repository.getThemesForChannelFlow(
                    channel: channelName,
                    minTimestamp: 0,
                    limit: 100,
                    offset: 0
                )
                themes = try await collectStrings(from: themeFlow)
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    // MARK: - Title Loading

    func loadTitles(for theme: String) {
        isLoading = true

        Task {
            do {
                let titleFlow: Kotlinx_coroutines_coreFlow
                if let channel = selectedChannel {
                    titleFlow = repository.getTitlesForChannelAndThemeFlow(
                        channel: channel.name,
                        theme: theme,
                        minTimestamp: 0,
                        limit: 100,
                        offset: 0
                    )
                } else {
                    titleFlow = repository.getTitlesForThemeFlow(
                        theme: theme,
                        minTimestamp: 0
                    )
                }
                titles = try await collectStrings(from: titleFlow)
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    // MARK: - Media Entry Loading

    func loadMediaEntry(title: String) async -> SharedTvos.MediaEntry? {
        return await withCheckedContinuation { continuation in
            Task {
                do {
                    let entryFlow: Kotlinx_coroutines_coreFlow
                    if let channel = selectedChannel, let theme = selectedTheme {
                        entryFlow = repository.getMediaEntryFlow(
                            channel: channel.name,
                            theme: theme,
                            title: title
                        )
                    } else if let theme = selectedTheme {
                        entryFlow = repository.getMediaEntryByThemeAndTitleFlow(
                            theme: theme,
                            title: title
                        )
                    } else {
                        entryFlow = repository.getMediaEntryByTitleFlow(title: title)
                    }

                    let entry = try await collectMediaEntry(from: entryFlow)
                    continuation.resume(returning: entry)
                } catch {
                    continuation.resume(returning: nil)
                }
            }
        }
    }

    // MARK: - Navigation Actions

    func selectChannel(_ channel: Channel?) {
        selectedChannel = channel
        if let channel = channel {
            loadThemes(for: channel.name)
        } else {
            loadAllThemes()
        }
    }

    func selectTheme(_ theme: String) {
        selectedTheme = theme
        loadTitles(for: theme)
    }

    func goBack() {
        if selectedTheme != nil {
            selectedTheme = nil
            titles = []
        } else if selectedChannel != nil {
            selectedChannel = nil
            loadAllThemes()
        }
    }

    // MARK: - Flow Collection Helpers

    private func collectStrings(from flow: Kotlinx_coroutines_coreFlow) async throws -> [String] {
        return try await withCheckedThrowingContinuation { continuation in
            var result: [String] = []

            flow.collect(
                collector: FlowCollector<NSArray> { items in
                    if let strings = items as? [String] {
                        result = strings
                    }
                },
                completionHandler: { error in
                    if let error = error {
                        continuation.resume(throwing: error)
                    } else {
                        continuation.resume(returning: result)
                    }
                }
            )
        }
    }

    private func collectMediaEntry(from flow: Kotlinx_coroutines_coreFlow) async throws -> SharedTvos.MediaEntry? {
        return try await withCheckedThrowingContinuation { continuation in
            var result: SharedTvos.MediaEntry?

            flow.collect(
                collector: FlowCollector<SharedTvos.MediaEntry?> { entry in
                    result = entry
                },
                completionHandler: { error in
                    if let error = error {
                        continuation.resume(throwing: error)
                    } else {
                        continuation.resume(returning: result)
                    }
                }
            )
        }
    }
}

// MARK: - Flow Collector Helper

private class FlowCollector<T>: Kotlinx_coroutines_coreFlowCollector {
    private let onEmit: (T) -> Void

    init(onEmit: @escaping (T) -> Void) {
        self.onEmit = onEmit
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let typedValue = value as? T {
            onEmit(typedValue)
        }
        completionHandler(nil)
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

import SwiftUI
