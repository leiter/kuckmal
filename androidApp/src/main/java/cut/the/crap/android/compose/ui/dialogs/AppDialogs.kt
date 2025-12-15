package cut.the.crap.android.compose.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cut.the.crap.android.R
import cut.the.crap.shared.viewmodel.DialogState
import cut.the.crap.shared.viewmodel.LoadingState
import cut.the.crap.shared.viewmodel.SharedViewModel

/**
 * Main dialog host that observes SharedViewModel state and displays appropriate dialogs
 */
@Composable
fun AppDialogs(
    viewModel: SharedViewModel,
    onDialogAction: (DialogState.Action, Int?) -> Unit = { _, _ -> }
) {
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val loadingProgress by viewModel.loadingProgress.collectAsStateWithLifecycle()
    val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()

    // Dialog state based dialogs
    dialogState?.let { state ->
        when (state) {
            is DialogState.Progress -> {
                // Check if we're in database loading phase
                val message = if (loadingState == LoadingState.LOADING && loadingProgress > 0) {
                    stringResource(R.string.dialog_msg_loaded_entries, loadingProgress)
                } else {
                    state.message
                }
                ProgressDialog(
                    title = state.title,
                    message = message,
                    onDismiss = if (state.cancelable) {{ viewModel.dismissDialog() }} else null
                )
            }
            is DialogState.Error -> {
                ErrorDialog(
                    title = state.title,
                    message = state.message,
                    retryLabel = state.retryLabel,
                    cancelLabel = state.cancelLabel,
                    showRetry = state.canRetry,
                    onRetry = {
                        viewModel.dismissDialog()
                        onDialogAction(state.retryAction, null)
                    },
                    onCancel = {
                        viewModel.dismissDialog()
                        onDialogAction(state.cancelAction, null)
                    }
                )
            }
            is DialogState.Message -> {
                MessageDialog(
                    title = state.title,
                    message = state.message,
                    positiveLabel = state.positiveLabel,
                    onPositive = {
                        viewModel.dismissDialog()
                        onDialogAction(state.positiveAction, null)
                    },
                    onDismiss = if (state.cancelable) {{ viewModel.dismissDialog() }} else null
                )
            }
            is DialogState.Confirmation -> {
                ConfirmationDialog(
                    title = state.title,
                    message = state.message,
                    positiveLabel = state.positiveLabel,
                    negativeLabel = state.negativeLabel,
                    onPositive = {
                        viewModel.dismissDialog()
                        onDialogAction(state.positiveAction, null)
                    },
                    onNegative = {
                        viewModel.dismissDialog()
                        onDialogAction(state.negativeAction, null)
                    },
                    onDismiss = if (state.cancelable) {{ viewModel.dismissDialog() }} else null
                )
            }
            is DialogState.SingleChoice -> {
                SingleChoiceDialog(
                    title = state.title,
                    items = state.items,
                    selectedIndex = state.selectedIndex,
                    onItemSelected = { index ->
                        viewModel.dismissDialog()
                        onDialogAction(DialogState.Action.CONFIRM, index)
                    },
                    negativeLabel = state.negativeLabel,
                    onNegative = {
                        viewModel.dismissDialog()
                        onDialogAction(DialogState.Action.CANCEL, null)
                    },
                    onDismiss = if (state.cancelable) {{ viewModel.dismissDialog() }} else null
                )
            }
        }
    }
}

/**
 * Welcome dialog shown on first app launch
 */
@Composable
fun WelcomeDialog(
    onStartDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.welcome_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Download info
                Text(
                    text = stringResource(R.string.welcome_download_size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Button(onClick = onStartDownload) {
                        Text(stringResource(R.string.btn_start_download))
                    }
                }
            }
        }
    }
}

/**
 * Progress dialog for download/decompression operations
 */
@Composable
fun ProgressDialog(
    title: String,
    message: String,
    onDismiss: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Error dialog with retry option
 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    retryLabel: String,
    cancelLabel: String,
    showRetry: Boolean = true,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (showRetry) {
                Button(onClick = onRetry) {
                    Text(retryLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(cancelLabel)
            }
        }
    )
}

/**
 * Simple message dialog
 */
@Composable
fun MessageDialog(
    title: String,
    message: String,
    positiveLabel: String,
    onPositive: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() ?: onPositive() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onPositive) {
                Text(positiveLabel)
            }
        }
    )
}

/**
 * Confirmation dialog with positive/negative buttons
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    positiveLabel: String,
    negativeLabel: String,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() ?: onNegative() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onPositive) {
                Text(positiveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onNegative) {
                Text(negativeLabel)
            }
        }
    )
}

/**
 * Single choice dialog (radio button list)
 */
@Composable
fun SingleChoiceDialog(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    negativeLabel: String,
    onNegative: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var currentSelection by remember { mutableStateOf(selectedIndex) }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() ?: onNegative() },
        title = { Text(title) },
        text = {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSelection == index,
                            onClick = { currentSelection = index }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onItemSelected(currentSelection) },
                enabled = currentSelection >= 0
            ) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onNegative) {
                Text(negativeLabel)
            }
        }
    )
}
