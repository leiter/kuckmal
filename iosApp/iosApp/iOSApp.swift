import SwiftUI
import shared

@main
struct iOSApp: App {

    init() {
        // Register XZ decompressor bridge before Koin initialization
        registerXzDecompressor()

        // Initialize Koin for dependency injection
        KoinHelperKt.doInitKoin()
    }

    private func registerXzDecompressor() {
        let bridge = XzDecompressorBridge.shared

        XzDecompressor.companion.registerDecompressor(
            decompressor: { nsData in
                guard let data = nsData as Data? else { return nil }
                return bridge.decompress(data)
            },
            fileDecompressor: { inputPath, outputPath in
                let result = bridge.decompressFile(inputPath: inputPath, outputPath: outputPath)
                return KotlinBoolean(bool: result)
            }
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    handleDeepLink(url)
                }
        }
    }

    /// Handle deep links from kuckmal:// URLs
    ///
    /// Supported URLs:
    /// - kuckmal://play?channel=ARD&theme=Tagesschau&title=VideoTitle
    /// - kuckmal://browse?channel=ZDF
    /// - kuckmal://search?q=tatort
    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "kuckmal" else { return }
        print("iOS deep link received: \(url)")

        // Delegate to Kotlin handler
        let handled = KoinHelperKt.handleDeepLink(urlString: url.absoluteString)
        if !handled {
            print("Deep link not handled: \(url)")
        }
    }
}
