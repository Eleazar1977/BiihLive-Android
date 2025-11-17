package com.mision.biihlive.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mision.biihlive.ui.theme.TextFieldGray
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.viewmodels.FirebaseAuthViewModel
import com.mision.biihlive.viewmodels.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateToResetPassword: (String) -> Unit,
    onNavigateBack: () -> Unit,
    authViewModel: FirebaseAuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Handle auth states
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Error -> {
                errorMessage = state.message
                successMessage = null
            }
            is AuthState.PasswordResetSent -> {
                successMessage = state.message
                errorMessage = null
            }
            else -> {
                // Clear messages for other states if needed
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Recuperar Contraseña",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Ingresa tu email para recibir un enlace de recuperación",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Success message
        successMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
                successMessage = null
            },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email", tint = TextFieldGray)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiihliveBlue,
                focusedLabelColor = BiihliveBlue,
                cursorColor = BiihliveBlue,
                unfocusedLabelColor = TextFieldGray,
                unfocusedBorderColor = TextFieldGray,
                focusedTextColor = Color(0xFF212121),
                unfocusedTextColor = Color(0xFF212121)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )

        // Send reset email button
        Button(
            onClick = {
                errorMessage = null
                successMessage = null
                authViewModel.sendPasswordResetEmail(email)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BiihliveBlue,
                contentColor = androidx.compose.ui.graphics.Color.White,
                disabledContainerColor = BiihliveBlue,
                disabledContentColor = androidx.compose.ui.graphics.Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp),
            enabled = authState !is AuthState.Loading && email.isNotEmpty()
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = "Enviar Enlace",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }

        // Back button
        TextButton(
            onClick = onNavigateBack,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF757575) // Gris medio para consistencia
            )
        ) {
            Text(
                text = "Volver al Login",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}