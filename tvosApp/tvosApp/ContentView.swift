import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = TvOSViewModel()
    @State private var selectedTitle: String? = nil
    @State private var showingDetail = false
    @State private var detailEntry: MediaEntry? = nil

    var body: some View {
        NavigationStack {
            HStack(spacing: 0) {
                // Left panel: Channels
                ChannelListView(
                    channels: viewModel.channels,
                    selectedChannel: $viewModel.selectedChannel,
                    onChannelSelected: { channel in
                        viewModel.selectChannel(channel)
                    }
                )
                .frame(width: 300)

                // Right panel: Themes or Titles
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    Text(headerText)
                        .font(.title2)
                        .foregroundColor(.blue)
                        .padding(.horizontal)

                    // Loading indicator
                    if viewModel.isLoading {
                        ProgressView()
                            .progressViewStyle(.circular)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if let error = viewModel.errorMessage {
                        Text(error)
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        // Content list
                        if viewModel.selectedTheme != nil {
                            TitleListView(
                                titles: viewModel.titles,
                                selectedTitle: $selectedTitle,
                                onTitleSelected: { title in
                                    selectedTitle = title
                                    loadDetailEntry(for: title)
                                }
                            )
                        } else {
                            ThemeListView(
                                themes: viewModel.themes,
                                selectedTheme: $viewModel.selectedTheme,
                                onThemeSelected: { theme in
                                    viewModel.selectTheme(theme)
                                }
                            )
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .navigationTitle("Kuckmal")
            .sheet(isPresented: $showingDetail) {
                if let entry = detailEntry {
                    DetailView(
                        mediaEntry: entry,
                        onDismiss: { showingDetail = false }
                    )
                }
            }
            .onExitCommand {
                // Handle Menu/Back button on Apple TV remote
                if showingDetail {
                    showingDetail = false
                } else if viewModel.selectedTheme != nil {
                    viewModel.selectedTheme = nil
                    selectedTitle = nil
                } else if viewModel.selectedChannel != nil {
                    viewModel.selectChannel(nil)
                }
            }
        }
    }

    private var headerText: String {
        if let theme = viewModel.selectedTheme {
            return "Titel: \(theme)"
        } else if let channel = viewModel.selectedChannel {
            return "Themen (\(channel.displayName))"
        } else {
            return "Alle Themen"
        }
    }

    private func loadDetailEntry(for title: String) {
        Task {
            if let kotlinEntry = await viewModel.loadMediaEntry(title: title) {
                await MainActor.run {
                    detailEntry = kotlinEntry.toSwiftMediaEntry()
                    showingDetail = true
                }
            } else {
                // Fallback if Kotlin entry loading fails
                await MainActor.run {
                    detailEntry = createFallbackEntry(title: title)
                    showingDetail = true
                }
            }
        }
    }

    private func createFallbackEntry(title: String) -> MediaEntry {
        let channel = viewModel.selectedChannel?.name ?? "ARD"
        let theme = viewModel.selectedTheme ?? "Allgemein"
        let id = Int64(abs(title.hashValue % 1_000_000))

        return MediaEntry(
            id: id,
            channel: channel,
            theme: theme,
            title: title,
            date: "25.07.2024",
            time: "20:15 Uhr",
            duration: "45 Min",
            sizeMB: "750 MB",
            description: "Eine spannende Sendung mit interessanten Inhalten zum Thema \(theme).",
            url: "https://example.com/video/\(id).mp4",
            smallUrl: "https://example.com/video/\(id)_small.mp4",
            hdUrl: "https://example.com/video/\(id)_hd.mp4",
            subtitleUrl: ""
        )
    }
}

#Preview {
    ContentView()
}
