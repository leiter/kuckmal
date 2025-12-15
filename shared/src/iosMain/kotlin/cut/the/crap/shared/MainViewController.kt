package cut.the.crap.shared

import androidx.compose.ui.window.ComposeUIViewController
import cut.the.crap.shared.ui.theme.KuckmalTheme
import platform.UIKit.UIViewController

/**
 * iOS entry point for Compose UI
 * Called from Swift via MainViewControllerKt.MainViewController()
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    KuckmalTheme {
        // TODO: Add main app content here
        // For now, show a placeholder
        AppContent()
    }
}
