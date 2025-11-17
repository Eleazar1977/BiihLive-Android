package com.mision.biihlive.presentation.auth.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.viewmodels.PasswordRecoveryViewModel
import com.mision.biihlive.viewmodels.RecoveryStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecoveryScreen(
    onRecoveryComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PasswordRecoveryViewModel = viewModel()
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
                    IconButton(onClick = {
                        if (uiState.step == RecoveryStep.ENTER_CODE || uiState.step == RecoveryStep.NUEVA_PASSWORD) {
                            viewModel.goBack()
                        } else {
                            onNavigateBack()
                        }
                    }) {
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
                            RecoveryStep.SEND_CODE -> "Recuperar contraseña"
                            RecoveryStep.ENTER_CODE -> "Ingresa el código"
                            RecoveryStep.NUEVA_PASSWORD -> "Nueva contraseña"
                            RecoveryStep.COMPLETED -> "Contraseña cambiada"
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
                RecoveryStep.SEND_CODE -> {
                    SendRecoveryCodeContent(
                        email = uiState.userEmail,
                        isLoading = uiState.isLoading,
                        onEmailChange = { viewModel.updateEmail(it) },
                        onSendCode = { viewModel.sendRecoveryCode() }
                    )
                }
                RecoveryStep.ENTER_CODE -> {
                    EnterRecoveryCodeContent(
                        email = uiState.userEmail,
                        enteredCode = uiState.enteredCode,
                        isVerifying = uiState.isVerifying,
                        canResend = uiState.canResend,
                        resendCooldown = uiState.resendCooldown,
                        onCodeChange = { viewModel.updateCode(it) },
                        onResendCode = { viewModel.resendRecoveryCode() }
                    )
                }
                RecoveryStep.NUEVA_PASSWORD -> {
                    NuevaPasswordContent(
                        newPassword = uiState.newPassword,
                        confirmPassword = uiState.confirmPassword,
                        passwordErrors = uiState.passwordErrors,
                        isChangingPassword = uiState.isChangingPassword,
                        isButtonEnabled = viewModel.isChangePasswordEnabled(),
                        onNewPasswordChange = { viewModel.updateNewPassword(it) },
                        onConfirmPasswordChange = { viewModel.updateConfirmPassword(it) },
                        onChangePassword = { viewModel.changePassword() }
                    )
                }
                RecoveryStep.COMPLETED -> {
                    RecoveryCompletedContent(
                        message = uiState.message ?: "Contraseña cambiada exitosamente"
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
                if (uiState.step != RecoveryStep.COMPLETED) {
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
private fun SendRecoveryCodeContent(
    email: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onSendCode: () -> Unit
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
        text = "Ingresa tu email y te enviaremos un código para recuperar tu contraseña",
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
        onClick = onSendCode,
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
            Text("🔐 Enviar código de recuperación")
        }
    }
}

@Composable
private fun EnterRecoveryCodeContent(
    email: String,
    enteredCode: String,
    isVerifying: Boolean,
    canResend: Boolean,
    resendCooldown: Int,
    onCodeChange: (String) -> Unit,
    onResendCode: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.LockOpen,
        contentDescription = "Código",
        modifier = Modifier.size(64.dp),
        tint = BiihliveBlue
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Ingresa el código de recuperación",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔐 Hemos enviado un código de recuperación de 6 dígitos a:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = BiihliveBlue,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "📁 Si no lo encuentras, revisa la carpeta de spam",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Código de recuperación",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    OutlinedTextField(
        value = enteredCode,
        onValueChange = { newValue ->
            if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                onCodeChange(newValue)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        placeholder = {
            Text(
                "Escribe el código de 6 dígitos",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        textStyle = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BiihliveBlue,
            textAlign = TextAlign.Center,
            letterSpacing = 8.sp
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        singleLine = true,
        maxLines = 1,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BiihliveBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = BiihliveBlue,
            unfocusedTextColor = BiihliveBlue
        )
    )

    if (isVerifying) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = BiihliveBlue
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Verificando código...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    OutlinedButton(
        onClick = onResendCode,
        enabled = canResend && !isVerifying,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            1.dp,
            if (canResend) BiihliveBlue else Color(0xFF9E9E9E)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (canResend) BiihliveBlue else Color(0xFF9E9E9E)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reenviar",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (canResend) "🔄 Reenviar código de recuperación"
            else "Reenviar en ${resendCooldown}s"
        )
    }
}

@Composable
private fun NuevaPasswordContent(
    newPassword: String,
    confirmPassword: String,
    passwordErrors: List<String>,
    isChangingPassword: Boolean,
    isButtonEnabled: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onChangePassword: () -> Unit
) {
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    Icon(
        imageVector = Icons.Default.VpnKey,
        contentDescription = "Nueva contraseña",
        modifier = Modifier.size(64.dp),
        tint = BiihliveBlue
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Establece tu nueva contraseña",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Crea una contraseña segura para tu cuenta",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Campo nueva contraseña
    OutlinedTextField(
        value = newPassword,
        onValueChange = onNewPasswordChange,
        label = { Text("Nueva contraseña") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Contraseña",
                tint = BiihliveBlue
            )
        },
        trailingIcon = {
            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                Icon(
                    imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showNewPassword) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (passwordErrors.isEmpty()) BiihliveBlue else MaterialTheme.colorScheme.error,
            focusedLabelColor = if (passwordErrors.isEmpty()) BiihliveBlue else MaterialTheme.colorScheme.error,
            focusedLeadingIconColor = BiihliveBlue
        )
    )

    // Mostrar errores de validación de contraseña
    if (passwordErrors.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Requisitos de la contraseña:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                passwordErrors.forEach { error ->
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Campo confirmar contraseña
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = { Text("Confirmar contraseña") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Confirmar contraseña",
                tint = BiihliveBlue
            )
        },
        trailingIcon = {
            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                Icon(
                    imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showConfirmPassword) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
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

    // Indicador de coincidencia de contraseñas
    if (confirmPassword.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (newPassword == confirmPassword && newPassword.isNotEmpty())
                    Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (newPassword == confirmPassword && newPassword.isNotEmpty())
                    Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (newPassword == confirmPassword && newPassword.isNotEmpty())
                    "Las contraseñas coinciden" else "Las contraseñas no coinciden",
                style = MaterialTheme.typography.bodySmall,
                color = if (newPassword == confirmPassword && newPassword.isNotEmpty())
                    Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onChangePassword,
        enabled = isButtonEnabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = BiihliveBlue
        )
    ) {
        if (isChangingPassword) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cambiando...")
        } else {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("🔐 Cambiar contraseña")
        }
    }
}

@Composable
private fun RecoveryCompletedContent(
    message: String
) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Completado",
        modifier = Modifier.size(64.dp),
        tint = Color(0xFF4CAF50)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "¡Contraseña cambiada exitosamente!",
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

    Spacer(modifier = Modifier.height(16.dp))

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
                text = "✅ Tu contraseña ha sido actualizada",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ya puedes iniciar sesión con tu nueva contraseña",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

// Reutilizar ErrorCard y SuccessCard del EmailVerificationScreen
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