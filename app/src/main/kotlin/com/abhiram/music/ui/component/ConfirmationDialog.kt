package com.abhiram.music.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.music.ui.theme.expressiveShapes

/**
 * Two-button confirmation dialog for destructive or irreversible actions.
 *
 * Built on the Material 3 [AlertDialog] with expressive shapes: a `squircle` container and
 * `pill` buttons, both ≥48 dp tall through the default button content padding. [text] is
 * optional — `null` (or blank) renders a title-only dialog; both actions dismiss through the
 * caller-supplied callbacks, which already fold their work into [onConfirm] / [onDismiss].
 *
 * Back press and outside taps route to [onDismiss], never to [onConfirm], so nothing destructive
 * can happen by accident.
 *
 * @param title headline, e.g. `"Clear the queue?"`.
 * @param text optional body copy explaining the consequences; `null` hides the body slot.
 * @param onConfirm invoked when the confirm button is pressed.
 * @param onDismiss invoked on cancel, back press and outside taps.
 * @param modifier sizing/positioning modifier for the dialog.
 * @param confirmText label of the affirmative action (e.g. `"Clear"`).
 * @param dismissText label of the cancelling action.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    text: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
) {
    val body: (@Composable () -> Unit)? =
        text?.takeIf { it.isNotBlank() }?.let { message ->
            {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = MaterialTheme.expressiveShapes.squircle,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = body,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shape = MaterialTheme.expressiveShapes.pill,
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.expressiveShapes.pill,
            ) {
                Text(text = dismissText)
            }
        },
    )
}
