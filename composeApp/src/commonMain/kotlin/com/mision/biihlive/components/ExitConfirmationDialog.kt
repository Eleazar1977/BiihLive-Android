package com.mision.biihlive.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ExitConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    StandardDialog(
        title = "Salir de la aplicación",
        message = "¿Realmente deseas salir?",
        confirmText = "Salir",
        dismissText = "Cancelar",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        showDialog = showDialog,
        isDangerous = true // Es una acción destructiva
    )
}

@Preview
@Composable
fun ExitConfirmationDialogPreview() {
    MaterialTheme {
        ExitConfirmationDialog(
            showDialog = true,
            onDismiss = {},
            onConfirm = {}
        )
    }
}