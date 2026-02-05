import Foundation

enum VideoQuality {
    case high
    case low
}

class VideoPlayerManager: ObservableObject {

    /// Select the appropriate video URL based on quality preference
    /// - Parameters:
    ///   - entry: The media entry containing URL options
    ///   - quality: Desired video quality
    /// - Returns: The resolved URL for playback, or nil if no valid URL available
    func selectVideoURL(for entry: MediaEntry, quality: VideoQuality) -> URL? {
        let urlString: String

        switch quality {
        case .high:
            // HIGH: Try hdUrl -> fallback to url
            if !entry.hdUrl.isEmpty {
                urlString = MediaUrlUtils.resolveUrl(mainUrl: entry.url, targetUrl: entry.hdUrl)
            } else {
                urlString = MediaUrlUtils.cleanMediaUrl(entry.url)
            }

        case .low:
            // LOW: Try smallUrl -> fallback to url
            if !entry.smallUrl.isEmpty {
                urlString = MediaUrlUtils.resolveUrl(mainUrl: entry.url, targetUrl: entry.smallUrl)
            } else {
                urlString = MediaUrlUtils.cleanMediaUrl(entry.url)
            }
        }

        guard !urlString.isEmpty else {
            print("VideoPlayerManager: No valid URL found for quality \(quality)")
            return nil
        }

        guard let url = URL(string: urlString) else {
            print("VideoPlayerManager: Invalid URL string: \(urlString)")
            return nil
        }

        print("VideoPlayerManager: Selected \(quality) URL: \(urlString)")
        return url
    }

    /// Get the subtitle URL if available
    /// - Parameter entry: The media entry
    /// - Returns: The subtitle URL, or nil if not available
    func getSubtitleURL(for entry: MediaEntry) -> URL? {
        guard !entry.subtitleUrl.isEmpty else { return nil }

        let urlString = MediaUrlUtils.cleanMediaUrl(entry.subtitleUrl)
        return URL(string: urlString)
    }
}
