package com.abhiram.music.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequest
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.abhiram.music.ui.theme.expressiveShapes

/**
 * Modal single-field editor: title, an auto-focused [OutlinedTextField] and OK/Cancel actions.
 *
 * The field is seeded with [value] (remembered across recomposition, re-seeded only when the
 * caller changes it) and receives focus as soon as the dialog opens, raising the keyboard. With
 * [singleLine] the IME shows a *Done* action that submits the current text exactly like the
 * confirm button; multi-line input keeps the standard newline action.
 *
 * Dismissal belongs to the caller: [onConfirm] only hands the text back, so validation-heavy
 * call sites (sleep timer, playlist rename) can keep the dialog open when the input is invalid —
 * every current call site closes the dialog itself inside [onConfirm]. Confirm is disabled while
 * the text is blank so an empty value can never be submitted.
 *
 * @param title headline above the field.
 * @param value initial text; changes reset the edited value.
 * @param onDismiss invoked on outside tap, back press and Cancel.
 * @param onConfirm invoked with the edited text on OK / IME *Done*.
 * @param modifier sizing/positioning modifier for the dialog.
 * @param placeholder hint shown while the field is empty; `""` hides it.
 * @param label floating label above the field; `null` hides it.
 * @param singleLine collapses the field to one row and enables the IME *Done* action.
 */
@Composable
fun TextFieldDialog(
    title: String,
    value: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    singleLine: Boolean = true,
) {
    var text by remember(value) { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val canConfirm = text.isNotBlank()

    val labelContent: (@Composable () -> Unit)? =
        label?.takeIf { it.isNotEmpty() }?.let { hint -> { Text(text = hint) } }
    val placeholderContent: (@Composable () -> Unit)? =
        placeholder.takeIf { it.isNotEmpty() }?.let { hint -> { Text(text = hint) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = MaterialTheme.expressiveShapes.squircle,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequest(focusRequester),
                label = labelContent,
                placeholder = placeholderContent,
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(
                    imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (canConfirm) onConfirm(text) },
                ),
                shape = MaterialTheme.expressiveShapes.pill,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = canConfirm,
                shape = MaterialTheme.expressiveShapes.pill,
            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.expressiveShapes.pill,
            ) {
                Text(text = "Cancel")
            }
        },
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
