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
        }
    }
}
