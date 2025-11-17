package com.mision.biihlive.presentation.perfil.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
fun ModernImagePreviewDialog(
    imageUri: Uri,
    isUploading: Boolean,
    uploadProgress: Float = 0f,
    isForProfile: Boolean = false, // true para foto de perfil, false para galería
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    // Animaciones
    var showContent by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    val rotation by animateFloatAsState(
        targetValue = if (isUploading) 360f else 0f,
        animationSpec = if (isUploading) {
            infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        }
    )

    LaunchedEffect(Unit) {
        showContent = true
    }

    Dialog(
        onDismissRequest = {
            if (!isUploading) {
                showContent = false
                onCancel()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isUploading,
            dismissOnClickOutside = !isUploading,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isUploading
                ) {
                    showContent = false
                    onCancel()
                }
        ) {
            // Efecto de partículas/blur en el fondo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp, BlurredEdgeTreatment.Unbounded)
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .offset(x = (-50).dp, y = 100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF7300).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-50).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1DC3FF).copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Contenido principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .scale(scale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Evita cerrar al tocar el contenido */ },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header con botón de cerrar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Título con icono
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isForProfile) Icons.Default.CameraAlt else Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isForProfile) "Foto de Perfil" else "Nueva Foto",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Botón cerrar estilizado
                    IconButton(
                        onClick = {
                            if (!isUploading) {
                                showContent = false
                                onCancel()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))

                // Imagen con efectos
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Glow effect detrás de la imagen
                    Box(
                        modifier = Modifier
                            .size(width = 350.dp, height = 450.dp)
                            .blur(50.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFF7300).copy(alpha = 0.3f),
                                        Color(0xFF1DC3FF).copy(alpha = 0.3f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    )

                    // Container de la imagen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f/4f)
                            .shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = Color(0xFFFF7300).copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = if (isForProfile) ContentScale.Crop else ContentScale.Fit
                        )

                        // Overlay de subida
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isUploading,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Indicador de progreso personalizado
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = uploadProgress,
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(0xFFFF7300),
                                            strokeWidth = 4.dp,
                                            trackColor = Color.White.copy(alpha = 0.2f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .rotate(rotation)
                                        )
                                    }
                                    Text(
                                        text = "${(uploadProgress * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Subiendo tu foto...",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                    }
                }

                Spacer(modifier = Modifier.weight(0.15f))

                // Botones de acción modernos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Botón Cancelar
                    ModernActionButton(
                        text = "Cancelar",
                        icon = Icons.Default.Close,
                        enabled = !isUploading,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showContent = false
                            onCancel()
                        }
                    )

                    // Botón Subir
                    ModernActionButton(
                        text = if (isUploading) "Subiendo" else "Publicar",
                        icon = if (isUploading) Icons.Default.CloudUpload else Icons.Default.Check,
                        enabled = !isUploading,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ModernActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = when {
                    isPrimary && enabled -> Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF7300),
                            Color(0xFFFF9500)
                        )
                    )
                    isPrimary && !enabled -> Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF7300).copy(alpha = 0.3f),
                            Color(0xFFFF9500).copy(alpha = 0.3f)
                        )
                    )
                    else -> Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (enabled) 0.15f else 0.05f),
                            Color.White.copy(alpha = if (enabled) 0.10f else 0.03f)
                        )
                    )
                }
            )
            .border(
                width = if (isPrimary) 0.dp else 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (enabled) 0.3f else 0.1f),
                        Color.White.copy(alpha = if (enabled) 0.2f else 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary || !enabled) Color.White else Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = if (isPrimary || !enabled) Color.White else Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}