package com.mision.biihlive.presentation.patrocinio.screens

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import android.widget.Toast
import android.graphics.drawable.BitmapDrawable
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mision.biihlive.R
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.ui.theme.BiihliveBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatrocinarScreen(
    navController: NavController,
    userId: String? = null // ID del usuario a patrocinar
) {
    val context = LocalContext.current
    val targetUserId = userId ?: ""

    // ViewModel
    val viewModel = remember {
        com.mision.biihlive.presentation.patrocinio.viewmodel.PatrocinarViewModel(
            targetUserId = targetUserId,
            firestoreRepository = com.mision.biihlive.data.repository.FirestoreRepository(),
            sessionManager = com.mision.biihlive.utils.SessionManager,
            context = context
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    // Mostrar errores con Toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patrocinar",
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
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BiihliveBlue)
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

                // Avatar del usuario con borde del color predominante
                Box(
                    modifier = Modifier.size(120.dp)
                ) {
                    // Obtener color dominante del avatar
                    val dominantColor by rememberDominantColor(
                        imageUrl = generatePatrocinadorAvatarUrl(targetUserId),
                        fallbackColor = BiihliveBlue
                    )

                    // Avatar con borde dinámico
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = dominantColor,
                                shape = CircleShape
                            )
                            .padding(3.dp)  // Espacio para el borde
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(generatePatrocinadorAvatarUrl(targetUserId))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar de ${uiState.perfil?.nickname ?: "Usuario"}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.ic_default_avatar),
                            error = painterResource(R.drawable.ic_default_avatar),
                            fallback = painterResource(R.drawable.ic_default_avatar)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))  // ✅ Revertido a original

                // Nombre del usuario (conservando SemiBold)
                Text(
                    text = uiState.perfil?.nickname ?: "Usuario",
                    fontSize = 24.sp,  // ✅ Revertido a original
                    fontWeight = FontWeight.SemiBold,  // ✅ CONSERVADO: SemiBold (menos pesado que Bold)
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))  // ✅ Revertido a original

                // Badge de nivel (conservando padding reducido)
                Surface(
                    modifier = Modifier
                        .background(BiihliveOrangeLight, RoundedCornerShape(16.dp)),  // ✅ Revertido a original
                    shape = RoundedCornerShape(16.dp),
                    color = BiihliveOrangeLight
                ) {
                    Text(
                        text = "Nivel ${uiState.perfil?.nivel ?: 1}",
                        fontSize = 14.sp,  // ✅ Revertido a original
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)  // ✅ CONSERVADO: vertical 3dp (en lugar de 6dp original)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))  // ✅ Revertido a original

                // Sección valor del patrocinio
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Valor del patrocinio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)  // ✅ Revertido a original
                    )

                    // Input del valor (solo si no está patrocinando)
                    if (!uiState.estaPatrocinando) {
                        OutlinedTextField(
                            value = uiState.valorPatrocinio,
                            onValueChange = { viewModel.updateValorPatrocinio(it) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BiihliveBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    } else {
                        // Mostrar valor actual del patrocinio (solo lectura)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = uiState.valorPatrocinio,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))  // ✅ Revertido a original

                // Card con descripción
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = uiState.perfil?.description?.takeIf { it.isNotBlank() }
                            ?: "¿Buscas el mejor contenido? Lo encontrarás aquí. Suscríbete a mi canal en Biihlive y compruébalo tú mismo.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botón Patrocinar/Cancelar patrocinio
                Button(
                    onClick = { viewModel.togglePatrocinio() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !uiState.isProcessingPatrocinio,
                    colors = if (uiState.estaPatrocinando) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = BiihliveBlue
                        )
                    } else {
                        ButtonDefaults.buttonColors(containerColor = BiihliveBlue)
                    },
                    border = if (uiState.estaPatrocinando) {
                        BorderStroke(1.dp, BiihliveBlue)
                    } else null,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isProcessingPatrocinio) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = if (uiState.estaPatrocinando) BiihliveBlue else Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.estaPatrocinando) "Cancelar patrocinio" else "Patrocinar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.estaPatrocinando) BiihliveBlue else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Genera la URL del avatar del usuario usando el patrón CloudFront establecido
 */
private fun generatePatrocinadorAvatarUrl(userId: String): String {
    val CLOUDFRONT_URL = "https://d183hg75gdabnr.cloudfront.net"
    val DEFAULT_TIMESTAMP = "1759240530172"
    return "$CLOUDFRONT_URL/userprofile/$userId/thumbnail_$DEFAULT_TIMESTAMP.png"
}

/**
 * Extrae el color dominante de una imagen para usar como borde del avatar
 */
@Composable
fun rememberDominantColor(
    imageUrl: String?,
    fallbackColor: Color = Color.Gray.copy(alpha = 0.3f)
): State<Color> {
    val context = LocalContext.current
    val dominantColor = remember { mutableStateOf(fallbackColor) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) {
            dominantColor.value = fallbackColor
            return@LaunchedEffect
        }

        try {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                val imageLoader = coil.ImageLoader(context)
                val request = coil.request.ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                if (result is coil.request.SuccessResult) {
                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (bitmap != null && !bitmap.isRecycled) {
                        val palette = androidx.palette.graphics.Palette.from(bitmap).generate()

                        val extractedColor = when {
                            palette.vibrantSwatch != null -> palette.vibrantSwatch!!.rgb
                            palette.lightVibrantSwatch != null -> palette.lightVibrantSwatch!!.rgb
                            palette.darkVibrantSwatch != null -> palette.darkVibrantSwatch!!.rgb
                            palette.mutedSwatch != null -> palette.mutedSwatch!!.rgb
                            palette.lightMutedSwatch != null -> palette.lightMutedSwatch!!.rgb
                            palette.darkMutedSwatch != null -> palette.darkMutedSwatch!!.rgb
                            else -> palette.getDominantColor(fallbackColor.toArgb())
                        }

                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            dominantColor.value = Color(extractedColor).copy(alpha = 0.4f)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            dominantColor.value = fallbackColor
        }
    }

    return dominantColor
}