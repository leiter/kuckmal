import SwiftUI

@main
struct KuckmalTVApp: App {
    @State private var deepLinkEntry: MediaEntry?
    @State private var showDeepLinkDetail = false

    init() {
        // Initialize Kotlin dependency injection
        KotlinDI.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    handleDeepLink(url)
                }
                .sheet(isPresented: $showDeepLinkDetail) {
                    if let entry = deepLinkEntry {
                        DetailView(
                            mediaEntry: entry,
                            onDismiss: { showDeepLinkDetail = false }
                        )
                    }
                }
        }
    }

    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "kuckmal" else { return }

        switch url.host {
        case "play":
            // Parse query parameters
            guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                  let queryItems = components.queryItems else {
                return
            }

            let channel = queryItems.first(where: { $0.name == "channel" })?.value ?? "ARD"
            let theme = queryItems.first(where: { $0.name == "theme" })?.value ?? "Allgemein"
            let title = queryItems.first(where: { $0.name == "title" })?.value ?? "Unbekannt"

            // Create entry and show detail view
            deepLinkEntry = createMediaEntry(channel: channel, theme: theme, title: title)
            showDeepLinkDetail = true

        case "browse":
            // Browse navigation - could be extended to navigate to specific channel/theme
            print("Browse deep link: \(url)")

        default:
            print("Unknown deep link: \(url)")
        }
    }

    private func createMediaEntry(channel: String, theme: String, title: String) -> MediaEntry {
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
            description: "Eine spannende Sendung mit interessanten Inhalten zum Thema \(theme). Produziert von \(channel).",
            url: "https://example.com/video/\(channel.lowercased())/\(theme.lowercased().replacingOccurrences(of: " ", with: "_")).mp4",
            smallUrl: "42|video_low.mp4",
            hdUrl: "42|video_hd.mp4",
            subtitleUrl: ""
        )
    }
}
