package com.mision.biihlive.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mision.biihlive.ui.theme.TextFieldGray
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.viewmodels.FirebaseAuthViewModel
import com.mision.biihlive.viewmodels.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit,
    authViewModel: FirebaseAuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.error.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.SignUpRequiresConfirmation -> {
                onNavigateToConfirmation(email)
            }
            else -> { }
        }
    }

    // Handle auth errors
    LaunchedEffect(authError) {
        if (authError != null) {
            errorMessage = authError
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
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

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
            },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email", tint = TextFieldGray)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
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
                .padding(bottom = 20.dp),
            singleLine = true
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Contraseña") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password", tint = TextFieldGray)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = TextFieldGray
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
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
                .padding(bottom = 20.dp),
            singleLine = true
        )

        // Confirm Password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            label = { Text("Confirmar Contraseña") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = TextFieldGray)
            },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = TextFieldGray
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
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

        // Sign up button
        Button(
            onClick = {
                errorMessage = null
                if (password == confirmPassword) {
                    authViewModel.signUpWithEmailAndPassword(email, password)
                } else {
                    errorMessage = "Las contraseñas no coinciden"
                }
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
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = "Crear Cuenta",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign in link
        TextButton(
            onClick = onNavigateToSignIn,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF757575) // Gris medio
            )
        ) {
            Text(
                text = "¿Ya tienes cuenta? Inicia sesión",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}