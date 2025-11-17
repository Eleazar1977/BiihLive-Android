package com.mision.biihlive.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun LogoutConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    StandardDialog(
        title = "Cerrar Sesión",
        message = "¿Estás seguro de que deseas cerrar sesión?",
        confirmText = "Cerrar Sesión",
        dismissText = "Cancelar",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        showDialog = showDialog,
        isDangerous = true // Es una acción destructiva (logout)
    )
}