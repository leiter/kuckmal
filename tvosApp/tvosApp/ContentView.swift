import SwiftUI

struct ContentView: View {
    @State private var selectedChannel: Channel? = nil
    @State private var selectedTheme: String? = nil
    @State private var selectedTitle: String? = nil
    @State private var showingDetail = false

    var body: some View {
        NavigationStack {
            HStack(spacing: 0) {
                // Left panel: Channels
                ChannelListView(
                    channels: SampleData.channels,
                    selectedChannel: $selectedChannel,
                    onChannelSelected: { channel in
                        selectedChannel = channel
                        selectedTheme = nil
                        selectedTitle = nil
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

                    // Content list
                    if selectedTheme != nil {
                        TitleListView(
                            titles: currentTitles,
                            selectedTitle: $selectedTitle,
                            onTitleSelected: { title in
                                selectedTitle = title
                                showingDetail = true
                            }
                        )
                    } else {
                        ThemeListView(
                            themes: currentThemes,
                            selectedTheme: $selectedTheme,
                            onThemeSelected: { theme in
                                selectedTheme = theme
                            }
                        )
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .navigationTitle("Kuckmal")
            .sheet(isPresented: $showingDetail) {
                if let title = selectedTitle {
                    DetailView(
                        mediaItem: SampleData.createMediaItem(
                            channel: selectedChannel?.name ?? "ARD",
                            theme: selectedTheme ?? "Allgemein",
                            title: title
                        ),
                        onDismiss: { showingDetail = false }
                    )
                }
            }
            .onExitCommand {
                // Handle Menu/Back button on Apple TV remote
                if showingDetail {
                    showingDetail = false
                } else if selectedTheme != nil {
                    selectedTheme = nil
                } else if selectedChannel != nil {
                    selectedChannel = nil
                }
            }
        }
    }

    private var headerText: String {
        if let theme = selectedTheme {
            return "Titel: \(theme)"
        } else if let channel = selectedChannel {
            return "Themen (\(channel.displayName))"
        } else {
            return "Alle Themen"
        }
    }

    private var currentThemes: [String] {
        if let channel = selectedChannel {
            return SampleData.getThemesForChannel(channel.name)
        } else {
            return SampleData.allThemes
        }
    }

    private var currentTitles: [String] {
        guard let theme = selectedTheme else { return [] }
        return SampleData.getTitlesForTheme(theme)
    }
}

#Preview {
    ContentView()
}
