import SwiftUI
import AVKit

struct VideoPlayerView: UIViewControllerRepresentable {
    let videoURL: URL
    let title: String
    let subtitleURL: URL?
    @Binding var isPresented: Bool

    init(videoURL: URL, title: String, subtitleURL: URL? = nil, isPresented: Binding<Bool>) {
        self.videoURL = videoURL
        self.title = title
        self.subtitleURL = subtitleURL
        self._isPresented = isPresented
    }

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let player = AVPlayer(url: videoURL)

        let playerViewController = AVPlayerViewController()
        playerViewController.player = player
        playerViewController.delegate = context.coordinator

        // Set metadata for display
        let titleItem = AVMutableMetadataItem()
        titleItem.identifier = .commonIdentifierTitle
        titleItem.value = title as NSString
        player.currentItem?.externalMetadata = [titleItem]

        // Auto-play on appear
        player.play()

        return playerViewController
    }

    func updateUIViewController(_ uiViewController: AVPlayerViewController, context: Context) {
        // No updates needed
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, AVPlayerViewControllerDelegate {
        let parent: VideoPlayerView

        init(_ parent: VideoPlayerView) {
            self.parent = parent
        }

        func playerViewControllerShouldDismiss(_ playerViewController: AVPlayerViewController) -> Bool {
            return true
        }

        func playerViewControllerWillBeginDismissalTransition(_ playerViewController: AVPlayerViewController) {
            parent.isPresented = false
        }
    }
}
