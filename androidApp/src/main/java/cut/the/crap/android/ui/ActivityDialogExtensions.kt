package cut.the.crap.android.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import cut.the.crap.android.R
import cut.the.crap.android.data.MediaViewModel
import cut.the.crap.android.ui.dialog.DialogFactory
import cut.the.crap.android.ui.dialog.DialogFactory.dismissSafely
import cut.the.crap.android.ui.dialog.DialogFactory.showSafely
import cut.the.crap.android.ui.dialog.DialogFactory.updateDialog
import cut.the.crap.android.ui.dialog.DialogModel
import cut.the.crap.android.util.AppConfig
import cut.the.crap.android.util.NetworkUtils

/**
 * Extension functions for MediaActivity to handle dialog presentation
 * Separated for better code organization and testability
 */

private const val TAG = "DialogExtensions"

/**
 * Handle dialog model changes from ViewModel
 * Creates, updates, or dismisses dialogs based on the model
 * Dialogs from this handler survive configuration changes
 */
fun MediaActivity.handleDialogModel(model: DialogModel?, currentDialog: AlertDialog?): AlertDialog? {
    // Prevent window leaks by checking if activity is still valid
    if (isFinishing || isDestroyed) {
        Log.w(TAG, "Activity is finishing/destroyed - not showing dialog")
        return currentDialog
    }

    return if (model == null) {
        // Dismiss current dialog
        currentDialog?.dismissSafely()
        null
    } else {
        // Create or update dialog
        if (currentDialog?.isShowing == true && model is DialogModel.Progress) {
            // Update existing progress dialog
            currentDialog.updateDialog(model)
            currentDialog
        } else {
            // Dismiss old dialog and create new one
            currentDialog?.dismissSafely()
            val newDialog = DialogFactory.createDialog(this, model)
            newDialog.showSafely()
            newDialog
        }
    }
}

/**
 * Show welcome dialog for first-time users
 * NOTE: This dialog cannot use ViewModel state management because it contains a custom View
 * that cannot be preserved across configuration changes. Instead, we show it directly
 * and track it for proper dismissal.
 *
 * @return The created dialog, so the Activity can track and dismiss it
 */
fun MediaActivity.showWelcomeDialog(viewModel: MediaViewModel): AlertDialog? {
    Log.d(TAG, "Showing welcome dialog for first-time setup")

    if (!NetworkUtils.isNetworkAvailable(this)) {
        // Show no-network dialog via ViewModel (no custom view, safe to preserve)
        viewModel.showDialog(
            DialogModel.Message(
                title = getString(R.string.dialog_title_internet_required),
                message = getString(R.string.dialog_msg_internet_required)
            )
        )
        return null
    }

    @SuppressLint("InflateParams")
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_welcome, null)
    val fileSizeMB = 73.3
    val downloadInfo = dialogView.findViewById<TextView>(R.id.download_info)
    val estimatedTime = dialogView.findViewById<TextView>(R.id.estimated_time)

    downloadInfo.text = NetworkUtils.getDownloadInfo(this, fileSizeMB)
    estimatedTime.text = getString(R.string.estimated_time, NetworkUtils.estimateDownloadTime(this, fileSizeMB))

    // Create and show welcome dialog directly (not via ViewModel)
    // Return it so Activity can track and dismiss it to prevent leaks
    val dialog = DialogFactory.createDialog(
        this,
        DialogModel.Welcome(
            view = dialogView,
            onStart = { startMediaListDownload() },
            onCancel = {
                Toast.makeText(
                    this,
                    getString(R.string.dialog_msg_download_later),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    )
    dialog.showSafely()
    return dialog
}

/**
 * Show dialog when an update is available
 */
fun MediaActivity.showUpdateAvailableDialog(viewModel: MediaViewModel) {
    viewModel.showDialog(
        DialogModel.Confirmation(
            title = getString(R.string.dialog_title_update_available),
            message = getString(R.string.dialog_msg_update_available),
            positiveLabel = getString(R.string.btn_update),
            negativeLabel = getString(R.string.btn_later),
            onPositive = {
                startMediaListDownload()
            },
            onNegative = {
                Toast.makeText(
                    this,
                    getString(R.string.dialog_msg_update_later),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.dismissDialog()
            }
        )
    )
}

/**
 * Show time period selection dialog
 */
fun MediaActivity.showTimePeriodDialog(viewModel: MediaViewModel) {
    val periods = arrayOf(
        AppConfig.getTimePeriodName(this, AppConfig.TIME_PERIOD_ALL),
        AppConfig.getTimePeriodName(this, AppConfig.TIME_PERIOD_1_DAY),
        AppConfig.getTimePeriodName(this, AppConfig.TIME_PERIOD_3_DAYS),
        AppConfig.getTimePeriodName(this, AppConfig.TIME_PERIOD_7_DAYS),
        AppConfig.getTimePeriodName(this, AppConfig.TIME_PERIOD_30_DAYS)
    )

    val currentPeriod = viewModel.timePeriodId.value

    // Create and show dialog directly for immediate user interaction
    val dialog = DialogFactory.createDialog(
        this,
        DialogModel.SingleChoice(
            title = getString(R.string.dialog_title_filter_date),
            items = periods,
            selectedIndex = currentPeriod,
            onItemSelected = { which ->
                val limitDate = AppConfig.getTimePeriodLimit(which)
                viewModel.setDateFilter(limitDate, which)
                Toast.makeText(
                    this,
                    getString(R.string.toast_filter, periods[which]),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    )
    dialog.showSafely()
}
