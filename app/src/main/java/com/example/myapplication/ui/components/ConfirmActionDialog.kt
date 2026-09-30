package com.example.myapplication.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun ConfirmActionDialog(
    titleResource: Int,
    messageResource: Int,
    confirmResource: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResource)) },
        text = { Text(stringResource(messageResource)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(confirmResource))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.example.myapplication.R.string.cancel))
            }
        }
    )
}
