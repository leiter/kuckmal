package cut.the.crap.android.ui.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import cut.the.crap.android.R

/**
 * Central factory for creating and managing AlertDialogs.
 * Provides consistent dialog creation and sequential dialog updates.
 */
object DialogFactory {

    private const val TAG = "DialogFactory"

    /**
     * Create an AlertDialog from a DialogModel.
     * This is the central function for all dialog creation.
     *
     * @param context Context for creating the dialog
     * @param model DialogModel containing all dialog configuration
     * @return AlertDialog configured according to the model
     */
    fun createDialog(context: Context, model: DialogModel): AlertDialog {
        return when (model) {
            is DialogModel.Message -> createMessageDialog(context, model)
            is DialogModel.Confirmation -> createConfirmationDialog(context, model)
            is DialogModel.Progress -> createProgressDialog(context, model)
            is DialogModel.Error -> createErrorDialog(context, model)
            is DialogModel.SingleChoice -> createSingleChoiceDialog(context, model)
            is DialogModel.CustomView -> createCustomViewDialog(context, model)
            is DialogModel.Welcome -> createWelcomeDialog(context, model)
        }
    }

    /**
     * Extension function to update an existing AlertDialog with new DialogModel.
     * Useful for sequential dialogs (Welcome -> Download -> Unzip).
     *
     * Usage:
     * ```
     * val dialog = createDialog(context, DialogModel.Progress(...))
     * dialog.show()
     * // Later...
     * dialog.updateDialog(DialogModel.Progress("New Title", "New Message"))
     * ```
     */
    fun AlertDialog.updateDialog(model: DialogModel) {
        when (model) {
            is DialogModel.Progress -> {
                // Update progress dialog content
                findViewById<TextView>(R.id.progress_message)?.text = model.message
                setTitle(model.title)
                setCancelable(model.cancelable)
            }
            is DialogModel.Message -> {
                setTitle(model.title)
                setMessage(model.message)
                setCancelable(model.cancelable)
            }
            is DialogModel.Confirmation -> {
                setTitle(model.title)
                setMessage(model.message)
                setCancelable(model.cancelable)
            }
            is DialogModel.Error -> {
                setTitle(model.title)
                setMessage(model.message)
                setCancelable(model.cancelable)
            }
            else -> {
                Log.w(TAG, "updateDialog not fully supported for ${model::class.simpleName}, recreate dialog instead")
            }
        }
    }

    /**
     * Safe show - shows dialog and handles exceptions
     */
    fun AlertDialog.showSafely() {
        try {
            show()
        } catch (e: Exception) {
            Log.e(TAG, "Could not show dialog", e)
        }
    }

    /**
     * Safe dismiss - dismisses dialog and handles exceptions
     */
    fun AlertDialog.dismissSafely() {
        try {
            if (isShowing) {
                dismiss()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing dialog", e)
        }
    }

    // Private helper functions for each dialog type

    private fun createMessageDialog(context: Context, model: DialogModel.Message): AlertDialog {
        val dialog = AlertDialog.Builder(context, R.style.AppDialogTheme)
            .setTitle(model.title)
            .setMessage(model.message)
            .setPositiveButton(model.positiveLabel) { dialog, _ ->
                model.onPositive?.invoke()
                dialog.dismiss()
            }
            .setCancelable(model.cancelable)
            .create()

        // Configure button for TV focus
        dialog.setOnShowListener {
            try {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring Message dialog button: ${e.message}")
            }
        }

        return dialog
    }

    private fun createConfirmationDialog(context: Context, model: DialogModel.Confirmation): AlertDialog {
        val dialog = AlertDialog.Builder(context, R.style.AppDialogTheme)
            .setTitle(model.title)
            .setMessage(model.message)
            .setPositiveButton(model.positiveLabel) { dialog, _ ->
                model.onPositive()
                dialog.dismiss()
            }
            .setNegativeButton(model.negativeLabel) { dialog, _ ->
                model.onNegative?.invoke()
                dialog.dismiss()
            }
            .setCancelable(model.cancelable)
            .create()

        // Configure buttons for TV focus
        dialog.setOnShowListener {
            try {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring Confirmation dialog buttons: ${e.message}")
            }
        }

        return dialog
    }

    private fun createProgressDialog(context: Context, model: DialogModel.Progress): AlertDialog {
        @SuppressLint("InflateParams")
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)
        dialogView.findViewById<TextView>(R.id.progress_message)?.text = model.message

        return AlertDialog.Builder(context, R.style.AppDialogTheme)
            .setTitle(model.title)
            .setView(dialogView)
            .setCancelable(model.cancelable)
            .create()
    }

    private fun createErrorDialog(context: Context, model: DialogModel.Error): AlertDialog {
        val dialog = AlertDialog.Builder(context, R.style.AppDialogTheme)
            .setTitle(model.title)
            .setMessage(model.message)
            .setPositiveButton(model.retryLabel) { dialog, _ ->
                model.onRetry()
                dialog.dismiss()
            }
            .setNegativeButton(model.cancelLabel) { dialog, _ ->
                model.onCancel?.invoke()
                dialog.dismiss()
            }
            .setCancelable(model.cancelable)
            .create()

        // Configure buttons for TV focus
        dialog.setOnShowListener {
            try {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring Error dialog buttons: ${e.message}")
            }
        }

        return dialog
    }

    private fun createSingleChoiceDialog(context: Context, model: DialogModel.SingleChoice): AlertDialog {
        val dialog = AlertDialog.Builder(context, R.style.AppDialogTheme)
            .setTitle(model.title)
            .setSingleChoiceItems(model.items, model.selectedIndex) { dialog, which ->
                model.onItemSelected(which)
                dialog.dismiss()
            }
            .setNegativeButton(model.negativeLabel) { dialog, _ ->
                model.onNegative?.invoke()
                dialog.dismiss()
            }
            .setCancelable(model.cancelable)
            .create()

        // Configure ListView for DPAD navigation after dialog is shown
        dialog.setOnShowListener {
            try {
                // Access the ListView in the dialog
                val listView = dialog.listView
                listView?.apply {
                    // Enable focus for DPAD navigation
                    isFocusable = true
                    isFocusableInTouchMode = false
                    descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS

                    // Set the currently selected item as focused
                    setSelection(model.selectedIndex)
                    setItemChecked(model.selectedIndex, true)

                    // Request focus on the selected item
                    post {
                        requestFocus()
                        // Ensure the selected item is visible
                        smoothScrollToPosition(model.selectedIndex)
                    }
                }

                // If there's a negative button, make it focusable with visible focus state
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    // Add padding for better visual appearance
                    setPadding(32, 16, 32, 16)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring SingleChoice dialog for TV navigation: ${e.message}")
            }
        }

        return dialog
    }

    private fun createCustomViewDialog(context: Context, model: DialogModel.CustomView): AlertDialog {
        val builder = AlertDialog.Builder(context, R.style.AppDialogTheme)
            .setView(model.view)
            .setCancelable(model.cancelable)

        model.title?.let { builder.setTitle(it) }

        model.positiveLabel?.let { label ->
            builder.setPositiveButton(label) { dialog, _ ->
                model.onPositive?.invoke()
                dialog.dismiss()
            }
        }

        model.negativeLabel?.let { label ->
            builder.setNegativeButton(label) { dialog, _ ->
                model.onNegative?.invoke()
                dialog.dismiss()
            }
        }

        val dialog = builder.create()

        // Configure buttons for TV focus if they exist
        dialog.setOnShowListener {
            try {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring CustomView dialog buttons: ${e.message}")
            }
        }

        return dialog
    }

    private fun createWelcomeDialog(context: Context, model: DialogModel.Welcome): AlertDialog {
        val dialog = AlertDialog.Builder(context, R.style.AppDialogTheme)
            .setView(model.view)
            .setPositiveButton(context.getString(R.string.btn_start_download)) { dialog, _ ->
                model.onStart()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.btn_cancel)) { dialog, _ ->
                model.onCancel?.invoke()
                dialog.dismiss()
            }
            .create()

        // Configure buttons for TV focus
        dialog.setOnShowListener {
            try {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setBackgroundResource(R.drawable.dialog_button_background)
                    setPadding(32, 16, 32, 16)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring Welcome dialog buttons: ${e.message}")
            }
        }

        return dialog
    }
}
