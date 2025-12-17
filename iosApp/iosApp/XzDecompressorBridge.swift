import Foundation
import SWCompression

/// Bridge class to expose SWCompression's XZ decompression to Kotlin/Native
@objc public class XzDecompressorBridge: NSObject {

    @objc public static let shared = XzDecompressorBridge()

    private override init() {
        super.init()
    }

    /// Decompress XZ data
    /// - Parameter data: The compressed XZ data
    /// - Returns: The decompressed data, or nil if decompression failed
    @objc public func decompress(_ data: Data) -> Data? {
        do {
            let decompressed = try XZArchive.unarchive(archive: data)
            return decompressed
        } catch {
            print("XZ decompression error: \(error)")
            return nil
        }
    }

    /// Decompress XZ file to output file
    /// - Parameters:
    ///   - inputPath: Path to the XZ compressed file
    ///   - outputPath: Path where decompressed file will be written
    /// - Returns: true if successful, false otherwise
    @objc public func decompressFile(inputPath: String, outputPath: String) -> Bool {
        do {
            let inputURL = URL(fileURLWithPath: inputPath)
            let outputURL = URL(fileURLWithPath: outputPath)

            let compressedData = try Data(contentsOf: inputURL)
            let decompressedData = try XZArchive.unarchive(archive: compressedData)

            try decompressedData.write(to: outputURL)

            print("XZ decompression: \(compressedData.count) -> \(decompressedData.count) bytes")
            return true
        } catch {
            print("XZ file decompression error: \(error)")
            return false
        }
    }

    /// Check if XZ decompression is supported
    @objc public func isSupported() -> Bool {
        return true
    }
}
