import SwiftUI

@main
struct KuckmalTVApp: App {
    @State private var deepLinkEntry: MediaEntry?
    @State private var showDeepLinkDetail = false

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
            deepLinkEntry = SampleData.createMediaEntry(
                channel: channel,
                theme: theme,
                title: title
            )
            showDeepLinkDetail = true

        case "browse":
            // Browse navigation - could be extended to navigate to specific channel/theme
            print("Browse deep link: \(url)")

        default:
            print("Unknown deep link: \(url)")
        }
    }
}
