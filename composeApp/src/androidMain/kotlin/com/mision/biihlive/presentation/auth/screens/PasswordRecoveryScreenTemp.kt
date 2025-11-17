package com.mision.biihlive.presentation.auth.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.viewmodels.PasswordRecoveryViewModelTemp
import com.mision.biihlive.viewmodels.RecoveryTempStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecoveryScreenTemp(
    onRecoveryComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PasswordRecoveryViewModelTemp = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navegación automática cuando se completa la recuperación
    LaunchedEffect(uiState.isRecoveryComplete) {
        if (uiState.isRecoveryComplete) {
            onRecoveryComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = when (uiState.step) {
                            RecoveryTempStep.SEND_EMAIL -> "Recuperar contraseña"
                            RecoveryTempStep.COMPLETED -> "Email enviado"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
    ) { paddingValues ->

        var isLayoutReady by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .onGloballyPositioned {
                    isLayoutReady = true
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            when (uiState.step) {
                RecoveryTempStep.SEND_EMAIL -> {
                    SendRecoveryEmailContent(
                        email = uiState.userEmail,
                        isLoading = uiState.isLoading,
                        onEmailChange = { viewModel.updateEmail(it) },
                        onSendEmail = { viewModel.sendRecoveryEmail() }
                    )
                }
                RecoveryTempStep.COMPLETED -> {
                    EmailSentContent(
                        email = uiState.userEmail,
                        message = uiState.message ?: "Email de recuperación enviado exitosamente"
                    )
                }
            }

            // Mostrar mensajes de error
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorCard(
                    error = error,
                    onDismiss = { viewModel.clearError() }
                )
            }

            // Mostrar mensajes de éxito
            uiState.message?.let { message ->
                if (uiState.step != RecoveryTempStep.COMPLETED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SuccessCard(
                        message = message,
                        onDismiss = { viewModel.clearMessage() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SendRecoveryEmailContent(
    email: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onSendEmail: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.LockReset,
        contentDescription = "Recuperar contraseña",
        modifier = Modifier.size(64.dp),
        tint = BiihliveBlue
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Recuperar contraseña",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Ingresa tu email y te enviaremos un enlace para crear una nueva contraseña",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email",
                tint = BiihliveBlue
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BiihliveBlue,
            focusedLabelColor = BiihliveBlue,
            focusedLeadingIconColor = BiihliveBlue
        )
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onSendEmail,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = BiihliveBlue
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enviando...")
        } else {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("🔐 Enviar enlace de recuperación")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ℹ️ Versión temporal",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Recibirás un enlace de Firebase para cambiar tu contraseña. Próximamente tendremos códigos de 6 dígitos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmailSentContent(
    email: String,
    message: String
) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Email enviado",
        modifier = Modifier.size(64.dp),
        tint = Color(0xFF4CAF50)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "¡Email enviado!",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📧 Revisa tu email",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hemos enviado un enlace a:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Si no lo encuentras, revisa la carpeta de spam",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Puedes cerrar esta pantalla y seguir las instrucciones del email",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Reutilizar componentes de error y éxito
@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SuccessCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
            contentColor = Color(0xFF2E7D32)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}