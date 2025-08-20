package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.dawitf.akahidegn.R // Import your R file

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRideDialog(
    destinationInput: String,
    onDestinationChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: (destination: String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.dialog_create_group_title_new)) },
        text = {
            OutlinedTextField(
                value = destinationInput,
                onValueChange = onDestinationChange,
                label = { Text(stringResource(id = R.string.destination_input_label_new)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (destinationInput.isNotBlank()) {
                        focusManager.clearFocus()
                        onConfirm(destinationInput.trim())
                    }
                })
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (destinationInput.isNotBlank()) {
                        focusManager.clearFocus()
                        onConfirm(destinationInput.trim())
                    }
                },
                enabled = destinationInput.isNotBlank()
            ) { Text(stringResource(id = R.string.dialog_button_create_group)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.dialog_button_cancel)) }
        }
    )
}