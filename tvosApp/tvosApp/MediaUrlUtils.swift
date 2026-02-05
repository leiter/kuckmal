import Foundation

/// Utility class for Kuckmal URL processing
///
/// Kuckmal uses a space-saving format for URLs where low/small/HD quality URLs
/// are stored in pipe-delimited format: "prefixLength|urlPath"
///
/// Example:
///   Main URL:     https://rodlzdf-a.akamaihd.net/path/to/video.mp4
///   Small URL:    135|video_low.mp4
///
/// The number (135) means: "Take the first 135 characters from the main URL and append the rest"
enum MediaUrlUtils {

    /// Clean media URL by removing pipe-delimited prefix and adding protocol if needed
    static func cleanMediaUrl(_ url: String) -> String {
        guard !url.isEmpty else { return "" }

        // Remove pipe-delimited prefix if present (e.g., "8|domain/path" -> "domain/path")
        let urlWithoutPrefix: String
        if let pipeIndex = url.firstIndex(of: "|") {
            urlWithoutPrefix = String(url[url.index(after: pipeIndex)...])
        } else {
            urlWithoutPrefix = url
        }

        // Add protocol if missing
        if urlWithoutPrefix.hasPrefix("http://") || urlWithoutPrefix.hasPrefix("https://") {
            return urlWithoutPrefix
        } else if !urlWithoutPrefix.isEmpty {
            return "https://\(urlWithoutPrefix)"
        } else {
            return ""
        }
    }

    /// Reconstruct URL from pipe-delimited format
    ///
    /// - Parameters:
    ///   - targetUrl: The URL to reconstruct (may be pipe-delimited like "135|video_low.mp4")
    ///   - baseUrl: The base URL to use for prefix extraction (usually the main URL)
    /// - Returns: Fully reconstructed URL with protocol
    static func reconstructUrl(targetUrl: String, baseUrl: String) -> String {
        guard !targetUrl.isEmpty else { return "" }

        // Check if URL is in pipe-delimited format
        if targetUrl.contains("|") {
            let parts = targetUrl.split(separator: "|", maxSplits: 1).map(String.init)
            guard parts.count == 2 else {
                print("MediaUrlUtils: Invalid pipe-delimited URL format: \(targetUrl)")
                return cleanMediaUrl(targetUrl)
            }

            guard let prefixLength = Int(parts[0]), prefixLength > 0 else {
                print("MediaUrlUtils: Invalid prefix length in URL: \(targetUrl)")
                return cleanMediaUrl(targetUrl)
            }

            // Clean the base URL to ensure it has protocol
            let baseCleaned = cleanMediaUrl(baseUrl)
            guard baseCleaned.count >= prefixLength else {
                print("MediaUrlUtils: Base URL too short for prefix length \(prefixLength): \(baseCleaned)")
                return cleanMediaUrl(targetUrl)
            }

            // Extract prefix from base URL and append path
            let prefixEndIndex = baseCleaned.index(baseCleaned.startIndex, offsetBy: prefixLength)
            let prefix = String(baseCleaned[..<prefixEndIndex])
            let reconstructed = prefix + parts[1]

            print("MediaUrlUtils: Reconstructed URL: \(targetUrl) -> \(reconstructed)")
            return reconstructed
        }

        // Not pipe-delimited, just clean normally
        return cleanMediaUrl(targetUrl)
    }

    /// Get the best available URL for a media entry
    ///
    /// - Parameters:
    ///   - mainUrl: The main/default URL
    ///   - targetUrl: The specific quality URL (smallUrl or hdUrl)
    /// - Returns: The resolved URL ready for playback/download
    static func resolveUrl(mainUrl: String, targetUrl: String) -> String {
        if !targetUrl.isEmpty {
            return reconstructUrl(targetUrl: targetUrl, baseUrl: mainUrl)
        } else {
            return cleanMediaUrl(mainUrl)
        }
    }
}
