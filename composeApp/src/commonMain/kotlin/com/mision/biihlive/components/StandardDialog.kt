package com.mision.biihlive.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.mision.biihlive.ui.theme.BiihliveOrangeLight

/**
 * Diálogo estándar de Biihlive con dimensiones optimizadas
 * Basado en Material Design 3 con ajustes para móviles
 */
@Composable
fun StandardDialog(
    title: String,
    message: String,
    confirmText: String = "Aceptar",
    dismissText: String = "Cancelar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDialog: Boolean = true,
    isDangerous: Boolean = false // Para acciones destructivas
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        modifier = Modifier
            .widthIn(min = 280.dp, max = 320.dp) // Ancho controlado
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp), // Corner radius más sutil
        title = {
            Text(
                text = title,
                fontSize = 18.sp, // Más compacto que 20sp
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDangerous)
                        MaterialTheme.colorScheme.error
                    else
                        BiihliveOrangeLight
                )
            ) {
                Text(
                    text = confirmText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = dismissText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    )
}

/**
 * Diálogo de confirmación simple (solo OK)
 */
@Composable
fun SimpleAlertDialog(
    title: String,
    message: String,
    confirmText: String = "Aceptar",
    onConfirm: () -> Unit,
    showDialog: Boolean = true
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onConfirm,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        modifier = Modifier
            .widthIn(min = 280.dp, max = 320.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BiihliveOrangeLight
                )
            ) {
                Text(
                    text = confirmText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    )
}

/**
 * Diálogo de loading compacto
 */
@Composable
fun LoadingDialog(
    message: String = "Cargando...",
    showDialog: Boolean = true
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = BiihliveOrangeLight
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    )
}