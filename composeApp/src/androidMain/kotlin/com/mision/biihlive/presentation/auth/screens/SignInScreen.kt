package com.mision.biihlive.screens

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mision.biihlive.ui.theme.TextFieldGray
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.viewmodels.FirebaseAuthViewModel
import com.mision.biihlive.utils.GoogleAccountSelector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPhoneVerification: () -> Unit,
    onNavigateToSigningIn: () -> Unit = {},
    authViewModel: FirebaseAuthViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.error.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Handle authentication states
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            Log.d("SignInScreen", "Usuario autenticado con Firebase")
            onNavigateToHome()
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
            text = "Iniciar Sesión",
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
                Icon(
                    Icons.Default.Email,
                    contentDescription = "Email",
                    tint = TextFieldGray
                )
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
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = TextFieldGray
                )
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

        // Sign in with Firebase button
        Button(
            onClick = {
                errorMessage = null
                authViewModel.signInWithEmailAndPassword(email, password)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BiihliveBlue,
                contentColor = androidx.compose.ui.graphics.Color.White,
                disabledContainerColor = BiihliveBlue, // Mismo color cuando está deshabilitado
                disabledContentColor = androidx.compose.ui.graphics.Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }

        // Divider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                text = "  O  ",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Divider(modifier = Modifier.weight(1f))
        }

        // Google Sign in
        OutlinedButton(
            onClick = {
                errorMessage = null
                scope.launch {
                    try {
                        GoogleAccountSelector.tryModernGoogleSignIn(
                            context = context,
                            onSuccess = { accountInfo ->
                                Log.d("SignInScreen", "Google Sign-In exitoso: ${accountInfo.email}")
                                // Crear perfil en Firebase con la información de Google
                                authViewModel.completeGoogleSignIn(
                                    email = accountInfo.email,
                                    displayName = accountInfo.displayName ?: accountInfo.email.substringBefore("@"),
                                    photoUrl = accountInfo.photoUrl,
                                    idToken = accountInfo.idToken // ← ID token para Firebase Auth
                                )
                            },
                            onFailure = { error ->
                                Log.e("SignInScreen", "Error en Google Sign-In: $error")
                                if (error == "Use AccountPicker") {
                                    errorMessage = "No hay cuentas Google disponibles. Agrega una cuenta Google en Configuración."
                                } else {
                                    errorMessage = "Error en Google Sign-In: $error"
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("SignInScreen", "Error iniciando Google Sign-In", e)
                        errorMessage = "Error al iniciar Google Sign-In: ${e.message}"
                    }
                }
            },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF757575) // Gris medio
            ),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)), // Border más fino y gris claro
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono de Google
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.mision.biihlive.R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified // Mantener colores originales
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Continuar con Google",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, // Menos peso
                        color = Color(0xFF757575) // Gris medio
                    )
                }
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onNavigateToForgotPassword,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF757575) // Gris medio
                )
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onNavigateToSignUp,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF757575) // Gris medio
            )
        ) {
            Text(
                text = "¿No tienes cuenta? Regístrate",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    MaterialTheme {
        SignInScreen(
            onNavigateToSignUp = {},
            onNavigateToForgotPassword = {},
            onNavigateToHome = {},
            onNavigateToPhoneVerification = {},
            onNavigateToSigningIn = {}
        )
    }
}