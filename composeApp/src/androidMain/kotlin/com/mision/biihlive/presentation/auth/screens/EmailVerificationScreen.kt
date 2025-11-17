package com.mision.biihlive.presentation.auth.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.viewmodels.EmailVerificationViewModel
import com.mision.biihlive.viewmodels.VerificationStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    userEmail: String,
    userId: String,
    onVerificationComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EmailVerificationViewModel = viewModel { EmailVerificationViewModel(userEmail, userId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navegación automática cuando se completa la verificación
    LaunchedEffect(uiState.isVerificationComplete) {
        if (uiState.isVerificationComplete) {
            onVerificationComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.step == VerificationStep.ENTER_CODE) {
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
                            VerificationStep.SEND_CODE -> "Verificación de Email"
                            VerificationStep.ENTER_CODE -> "Ingresa el código"
                            VerificationStep.COMPLETED -> "Verificación completa"
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
        // ✅ GEMINI SOLUTION: windowInsets para manejo correcto del teclado
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
                    // ✅ GEMINI SOLUTION: Esperar a que layout esté listo
                    isLayoutReady = true
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            when (uiState.step) {
                VerificationStep.SEND_CODE -> {
                    SendCodeContent(
                        email = uiState.userEmail,
                        isLoading = uiState.isLoading,
                        onSendCode = { viewModel.sendVerificationCode() }
                    )
                }
                VerificationStep.ENTER_CODE -> {
                    EnterCodeContent(
                        email = uiState.userEmail,
                        enteredCode = uiState.enteredCode,
                        isVerifying = uiState.isVerifying,
                        canResend = uiState.canResend,
                        resendCooldown = uiState.resendCooldown,
                        onCodeChange = { viewModel.updateCode(it) },
                        onResendCode = { viewModel.resendCode() }
                    )
                }
                VerificationStep.COMPLETED -> {
                    CompletedContent(
                        message = uiState.message ?: "Verificación completada exitosamente"
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
                if (uiState.step != VerificationStep.COMPLETED) {
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
private fun SendCodeContent(
    email: String?,
    isLoading: Boolean,
    onSendCode: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Email,
        contentDescription = "Email",
        modifier = Modifier.size(64.dp),
        tint = BiihliveBlue
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Verificación de Email",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Te enviaremos un código de verificación a:",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = email ?: "Tu email registrado",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = BiihliveBlue,
        fontWeight = FontWeight.Medium
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
            Text("📧 Enviar código")
        }
    }
}

@Composable
private fun EnterCodeContent(
    email: String?,
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
        text = "Ingresa el código",
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
                text = "📧 Hemos enviado un código de 6 dígitos a:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email ?: "tu email",
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

    /*
     * ✅ GEMINI SOLUTION: TextField VISIBLE + LocalFocusManager
     *
     * Solución definitiva que evita crashes de BringIntoViewRequester:
     * 1. TextField visible (no invisible) evita timing issues
     * 2. LocalFocusManager en lugar de FocusRequester directo
     * 3. Sin automatismos problemáticos que causen crashes
     */

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Código de verificación",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    // ✅ TextField simplificado con números grandes en celeste
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
            if (canResend) "🔄 Reenviar código"
            else "Reenviar en ${resendCooldown}s"
        )
    }
}

@Composable
private fun CompletedContent(
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
        text = "¡Verificación exitosa!",
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
}


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