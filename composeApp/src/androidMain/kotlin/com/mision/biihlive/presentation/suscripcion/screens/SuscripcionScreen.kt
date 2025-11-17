package com.mision.biihlive.presentation.suscripcion.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mision.biihlive.R
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.ui.theme.BiihliveBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionScreen(
    navController: NavController,
    userId: String? = null // ID del usuario a suscribirse
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val targetUserId = userId ?: ""

    // ViewModel
    val viewModel = remember {
        com.mision.biihlive.presentation.suscripcion.viewmodel.SuscripcionViewModel(
            targetUserId = targetUserId,
            firestoreRepository = com.mision.biihlive.data.repository.FirestoreRepository(),
            sessionManager = com.mision.biihlive.utils.SessionManager,
            context = context
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    // Mostrar errores con Toast
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Suscribirse",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BiihliveOrangeLight)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Avatar del usuario
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(generateThumbnailUrl(targetUserId))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar de ${uiState.perfil?.nickname ?: "Usuario"}",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_default_avatar),
                    error = painterResource(R.drawable.ic_default_avatar),
                    fallback = painterResource(R.drawable.ic_default_avatar)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nombre del usuario
                Text(
                    text = uiState.perfil?.nickname ?: "Usuario",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Badge de nivel
                Surface(
                    modifier = Modifier
                        .background(BiihliveOrangeLight, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = BiihliveOrangeLight
                ) {
                    Text(
                        text = "Nivel ${uiState.perfil?.nivel ?: 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

            Spacer(modifier = Modifier.height(32.dp))

                // Sección valor de la suscripción
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Valor de la suscripción",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Input del valor (solo lectura) - Datos reales de subscriptionConfig
                    val perfil = uiState.perfil
                    val subscriptionValue = if (perfil?.subscriptionConfig?.isEnabled == true) {
                        val config = perfil.subscriptionConfig
                        "${config.currency} ${config.options.firstOrNull()?.price ?: "9.99"}/${config.options.firstOrNull()?.duration ?: "1 mes"}"
                    } else {
                        "Suscripciones no disponibles"
                    }

                    OutlinedTextField(
                        value = subscriptionValue,
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

            Spacer(modifier = Modifier.height(24.dp))

            // Card con descripción
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = uiState.perfil?.subscriptionConfig?.description ?: "¡Únete a mi mundo en Biihlive! Suscríbete y no te pierdas ninguna de mis transmisiones en vivo.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

                // Botón Suscribirse/Cancelar Suscripción - Solo disponible si las suscripciones están habilitadas
                val perfil = uiState.perfil
                val subscriptionsEnabled = perfil?.subscriptionConfig?.isEnabled == true

                if (!subscriptionsEnabled) {
                    // Suscripciones no disponibles
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = false
                    ) {
                        Text(
                            text = "Suscripciones no disponibles",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else if (uiState.estaSuscrito) {
                    // Botón para cancelar suscripción (borde celeste, fondo blanco, texto celeste)
                    OutlinedButton(
                        onClick = { viewModel.toggleSuscripcion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = BiihliveBlue
                        ),
                        border = BorderStroke(2.dp, BiihliveBlue),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !uiState.isProcessingSuscripcion
                    ) {
                        if (uiState.isProcessingSuscripcion) {
                            CircularProgressIndicator(
                                color = BiihliveBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Cancelar suscripción",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = BiihliveBlue
                            )
                        }
                    }
                } else {
                    // Botón para suscribirse (fondo naranja, texto blanco)
                    Button(
                        onClick = { viewModel.toggleSuscripcion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BiihliveOrangeLight
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !uiState.isProcessingSuscripcion
                    ) {
                        if (uiState.isProcessingSuscripcion) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Suscribirse",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// Función para generar URL de thumbnail
private fun generateThumbnailUrl(userId: String): String {
    val CLOUDFRONT_URL = "https://d183hg75gdabnr.cloudfront.net"
    val DEFAULT_TIMESTAMP = "1759240530172"
    return "$CLOUDFRONT_URL/userprofile/$userId/thumbnail_$DEFAULT_TIMESTAMP.png"
}