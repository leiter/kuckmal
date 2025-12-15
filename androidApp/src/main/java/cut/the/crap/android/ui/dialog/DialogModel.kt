package cut.the.crap.android.ui.dialog

import android.view.View

/**
 * Sealed interface for type-safe dialog configurations.
 * Each data class bundles all attributes needed for a specific dialog type.
 */
sealed interface DialogModel {

    /**
     * Simple message dialog with title, message, and single button
     */
    data class Message(
        val title: String,
        val message: String,
        val positiveLabel: String = "OK",
        val onPositive: (() -> Unit)? = null,
        val cancelable: Boolean = true
    ) : DialogModel

    /**
     * Confirmation dialog with title, message, and positive/negative buttons
     */
    data class Confirmation(
        val title: String,
        val message: String,
        val positiveLabel: String = "OK",
        val negativeLabel: String = "Cancel",
        val onPositive: () -> Unit,
        val onNegative: (() -> Unit)? = null,
        val cancelable: Boolean = true
    ) : DialogModel

    /**
     * Progress dialog showing indeterminate progress
     */
    data class Progress(
        val title: String,
        val message: String,
        val cancelable: Boolean = false
    ) : DialogModel

    /**
     * Error dialog with retry option
     */
    data class Error(
        val title: String = "Error",
        val message: String,
        val retryLabel: String = "Retry",
        val cancelLabel: String = "Cancel",
        val onRetry: () -> Unit,
        val onCancel: (() -> Unit)? = null,
        val cancelable: Boolean = false
    ) : DialogModel

    /**
     * Single choice dialog (radio buttons)
     */
    data class SingleChoice(
        val title: String,
        val items: Array<String>,
        val selectedIndex: Int = -1,
        val onItemSelected: (which: Int) -> Unit,
        val negativeLabel: String = "Cancel",
        val onNegative: (() -> Unit)? = null,
        val cancelable: Boolean = true
    ) : DialogModel {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SingleChoice

            if (title != other.title) return false
            if (!items.contentEquals(other.items)) return false
            if (selectedIndex != other.selectedIndex) return false
            if (negativeLabel != other.negativeLabel) return false
            if (cancelable != other.cancelable) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + items.contentHashCode()
            result = 31 * result + selectedIndex
            result = 31 * result + negativeLabel.hashCode()
            result = 31 * result + cancelable.hashCode()
            return result
        }
    }

    /**
     * Custom view dialog with custom layout
     */
    data class CustomView(
        val title: String? = null,
        val view: View,
        val positiveLabel: String? = null,
        val negativeLabel: String? = null,
        val onPositive: (() -> Unit)? = null,
        val onNegative: (() -> Unit)? = null,
        val cancelable: Boolean = true
    ) : DialogModel

    /**
     * Welcome dialog with custom layout (for app first start)
     */
    data class Welcome(
        val view: View,
        val onStart: () -> Unit,
        val onCancel: (() -> Unit)? = null
    ) : DialogModel
}
