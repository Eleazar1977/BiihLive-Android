package com.mision.biihlive.presentation.perfil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest as CoilImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mision.biihlive.navigation.Screen
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.ui.theme.BiihliveOrange
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.utils.formatNumber
import com.mision.biihlive.R
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mision.biihlive.presentation.perfil.components.ModernImagePreviewDialog
import com.mision.biihlive.components.CircularProgressBar
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import coil.request.CachePolicy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.mision.biihlive.utils.LevelCalculator
import com.mision.biihlive.components.BiihliveNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilPersonalLogueadoScreen(
    navController: NavController,
    viewModel: PerfilPersonalLogueadoViewModel,
    currentRoute: String = "profile"
) {
    val uiState by viewModel.uiState.collectAsState()

    // Estados para el manejo de imagen
    var showFullImageDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }
    var isGalleryUpload by remember { mutableStateOf(false) }

    // Image picker launcher para foto de perfil
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            showImagePreview = true
            showFullImageDialog = false
            isGalleryUpload = false // Para perfil
        }
    }

    // Image picker launcher para galería
    val galleryImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            showImagePreview = true
            showFullImageDialog = false
            isGalleryUpload = true // Para galería
        }
    }

    BackHandler {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            PerfilTopBar(
                onBackClick = { navController.popBackStack() },
                onMenuClick = { navController.navigate("ajustes") },
                onPatnerClick = { navController.navigate(Screen.Patrocinios.route) } // Navegación a lista de patrocinios
            )
        },
        bottomBar = {
            BiihliveNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    when (route) {
                        "home" -> navController.navigate(Screen.Home.route)
                        "events" -> { /* Navegar a eventos */ }
                        "live" -> { /* Navegar a live */ }
                        "messages" -> navController.navigate(Screen.MessagesList.route)
                        "profile" -> { /* Ya estamos en profile */ }
                    }
                }
            )
        }
    ) { paddingValues ->
        PerfilContent(
            uiState = uiState,
            onRetry = { viewModel.cargarPerfil() },
            onUpdateNickname = viewModel::actualizarNickname,
            onUpdateDescription = viewModel::actualizarDescripcion,
            onErrorDismiss = viewModel::limpiarError,
            onSuccessDismiss = viewModel::limpiarUpdateSuccess,
            onShowFullImage = { showFullImageDialog = true },
            onSelectImage = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onUploadToGallery = {
                galleryImagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onLoadGallery = { viewModel.loadGalleryImages() },
            onLoadMoreGallery = { viewModel.loadGalleryImages(loadMore = true) },
            onNavigateToFollowers = { userId ->
                navController.navigate(Screen.FollowersFollowing.createRoute(userId, 0))
            },
            onNavigateToFollowing = { userId ->
                navController.navigate(Screen.FollowersFollowing.createRoute(userId, 1))
            },
            onNavigateToGroups = { userId ->
                navController.navigate(Screen.Grupos.createRoute(userId))
            },
            onNavigateToRanking = { userId ->
                // Obtener la preferencia de ranking del perfil y mapearla a índice de tab
                val rankingPreference = uiState.perfil?.rankingPreference
                val initialTab = com.mision.biihlive.presentation.ranking.viewmodel.RankingViewModel
                    .mapPreferenceToTabIndex(rankingPreference)
                navController.navigate(Screen.Ranking.createRoute(userId, initialTab))
            },
            onNavigateToEditarPerfil = {
                navController.navigate(Screen.EditarPerfil.route)
            },
            onNavigateToSuscripciones = {
                navController.navigate(Screen.Suscripciones.route)
            },
            modifier = Modifier.padding(paddingValues)  // Con padding completo para TopBar y BottomBar
        )
    }

    // Dialog para mostrar imagen full con opción de cambiar
    if (uiState.perfil != null) {
        FullScreenImageDialog(
            isVisible = showFullImageDialog,
            perfil = uiState.perfil!!,
            shouldBypassImageCache = uiState.shouldBypassImageCache,
            profileImageUrl = uiState.profileImageUrl,
            onDismiss = { showFullImageDialog = false },
            onChangePhoto = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    // Diálogo de preview de imagen
    selectedImageUri?.let { uri ->
        if (showImagePreview) {
            val context = LocalContext.current
            ModernImagePreviewDialog(
                imageUri = uri,
                isUploading = uiState.isUploadingImage,
                uploadProgress = uiState.uploadProgress,
                isForProfile = !isGalleryUpload,
                onConfirm = {
                    if (isGalleryUpload) {
                        viewModel.uploadGalleryImage(uri)
                    } else {
                        viewModel.uploadProfileImage(uri)
                    }
                },
                onCancel = {
                    showImagePreview = false
                    selectedImageUri = null
                    isGalleryUpload = false
                }
            )
        }
    }

    // Cerrar diálogo cuando la subida sea exitosa
    LaunchedEffect(uiState.uploadSuccess) {
        if (uiState.uploadSuccess) {
            showImagePreview = false
            selectedImageUri = null
            isGalleryUpload = false  // Resetear la bandera de galería
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerfilTopBar(
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    onPatnerClick: () -> Unit = {} // Nuevo callback para el botón Patner
) {
    TopAppBar(
        title = {
            Text(
                "Mi Perfil",
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        actions = {
            // Botón de Patner/Patrocinio
            IconButton(onClick = onPatnerClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_patner),
                    contentDescription = "Patrocinio",
                    tint = BiihliveBlue,
                    modifier = Modifier.size(36.dp) // 30% más grande (28 * 1.3 = 36.4)
                )
            }
            
            // Botón de Menú
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menú")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerfilContent(
    uiState: PerfilUiState,
    onRetry: () -> Unit,
    onUpdateNickname: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onErrorDismiss: () -> Unit,
    onSuccessDismiss: () -> Unit,
    onShowFullImage: () -> Unit,
    onSelectImage: () -> Unit,
    onUploadToGallery: () -> Unit,
    onLoadGallery: () -> Unit,
    onLoadMoreGallery: () -> Unit,
    onNavigateToFollowers: (String) -> Unit,
    onNavigateToFollowing: (String) -> Unit,
    onNavigateToGroups: (String) -> Unit,
    onNavigateToRanking: (String) -> Unit,
    onNavigateToEditarPerfil: () -> Unit,
    onNavigateToSuscripciones: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado para controlar el pull-to-refresh
    var isRefreshing by remember { mutableStateOf(false) }

    // Actualizar el estado de refreshing basado en AMBOS estados del ViewModel
    LaunchedEffect(uiState.isLoading, uiState.isRefreshing) {
        // Si ninguno está cargando, detener el indicador
        if (!uiState.isLoading && !uiState.isRefreshing) {
            isRefreshing = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.hasError -> {
                ErrorContent(
                    error = uiState.error!!,
                    onRetry = onRetry,
                    onDismiss = onErrorDismiss
                )
            }

            uiState.isContentVisible -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        onRetry() // Llamar a cargarPerfil() para actualizar todo
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                    ) {
                        item {
                            PerfilInfo(
                            perfil = uiState.perfil!!,
                            siguienteNivel = uiState.siguienteNivel,
                            progreso = uiState.progreso,
                            followersCount = uiState.perfil!!.seguidores,
                            followingCount = uiState.perfil!!.siguiendo,
                            rankingPosition = uiState.rankingPosition,  // ✅ Nuevo parámetro
                            rankingScope = uiState.rankingScope,        // ✅ Nuevo parámetro
                            onUpdateNickname = onUpdateNickname,
                            onUpdateDescription = onUpdateDescription,
                            onShowFullImage = onShowFullImage,
                            onSelectImage = onSelectImage,
                            onUploadToGallery = onUploadToGallery,
                            isUpdating = uiState.isUpdating,
                            shouldBypassImageCache = uiState.shouldBypassImageCache,
                            profileImageUrl = uiState.profileImageUrl,
                            profileThumbnailUrl = uiState.profileThumbnailUrl,
                            onNavigateToFollowers = {
                                onNavigateToFollowers(uiState.perfil!!.userId)
                            },
                            onNavigateToFollowing = {
                                onNavigateToFollowing(uiState.perfil!!.userId)
                            },
                            onNavigateToGroups = {
                                onNavigateToGroups(uiState.perfil!!.userId)
                            },
                            onNavigateToRanking = {
                                onNavigateToRanking(uiState.perfil!!.userId)
                            },
                            onNavigateToEditarPerfil = onNavigateToEditarPerfil,
                            onNavigateToSuscripciones = onNavigateToSuscripciones
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(4.dp))  // Espaciado reducido entre secciones
                    }
                    
                    item {
                        SeccionMultimedia(
                            perfil = uiState.perfil!!,
                            onUploadToGallery = onUploadToGallery,
                            galleryImages = uiState.galleryImages,
                            isLoadingGallery = uiState.isLoadingGallery,
                            hasMoreImages = uiState.hasMoreGalleryImages,
                            onLoadGallery = onLoadGallery,
                            onLoadMoreImages = onLoadMoreGallery
                        )
                    }
                }
                }
            }
        }

        // Mensaje de éxito
        if (uiState.updateSuccess) {
            LaunchedEffect(uiState.updateSuccess) {
                // Aquí podrías mostrar un Snackbar
                kotlinx.coroutines.delay(2000)
                onSuccessDismiss()
            }
        }
    }
}

@Composable
private fun PerfilInfo(
    perfil: PerfilUsuario,
    siguienteNivel: Int,
    progreso: Double,
    followersCount: Int,
    followingCount: Int,
    rankingPosition: String,  // ✅ Nuevo parámetro para posición
    rankingScope: String,     // ✅ Nuevo parámetro para ámbito
    onUpdateNickname: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onShowFullImage: () -> Unit,
    onSelectImage: () -> Unit,
    onUploadToGallery: () -> Unit,
    isUpdating: Boolean,
    shouldBypassImageCache: Boolean = false,
    profileImageUrl: String? = null,
    profileThumbnailUrl: String? = null,
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onNavigateToEditarPerfil: () -> Unit = {},
    onNavigateToSuscripciones: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),  // Padding vertical mínimo
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)  // Espaciado compacto entre secciones
    ) {
        // Sección superior: Avatar + Info usuario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),  // Padding mínimo
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Columna izquierda: Avatar y puntos
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),  // Reducido espaciado
                modifier = Modifier
                    .padding(2.dp)  // Padding mínimo
            ) {
                // Avatar con barra de progreso circular y borde dinámico
                Box(modifier = Modifier
                    .size(91.dp)  // Reducido 10%
                ) {
                    // Obtener color dominante del avatar
                    val dominantColor by rememberDominantColor(
                        imageUrl = profileThumbnailUrl
                    )

                    CircularProgressBar(
                        progress = progreso.toFloat(),
                        size = 91.dp,  // Tamaño original
                        strokeWidth = 7.dp,  // Ajustado proporcionalmente
                        progressColor = BiihliveOrangeLight,  // Naranja light
                        backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),  // Gris más claro y sutil
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onShowFullImage() }
                                .border(1.5.dp, Color.White, CircleShape),  // Borde blanco
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                                // Usar la URL del thumbnail pasada como parámetro
                                val imageUrl = profileThumbnailUrl

                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(false)
                                        // Deshabilitar caché si hay bypass activo
                                        .diskCachePolicy(
                                            if (shouldBypassImageCache) coil.request.CachePolicy.DISABLED
                                            else coil.request.CachePolicy.ENABLED
                                        )
                                        .memoryCachePolicy(
                                            if (shouldBypassImageCache) coil.request.CachePolicy.DISABLED
                                            else coil.request.CachePolicy.ENABLED
                                        )
                                        .memoryCacheKey("profile_${perfil.userId}")
                                        .diskCacheKey("profile_${perfil.userId}")
                                        .build(),
                                    contentDescription = "Foto de Perfil",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.ic_default_avatar),
                                    error = painterResource(R.drawable.ic_default_avatar),
                                    fallback = painterResource(R.drawable.ic_default_avatar)
                                )
                        }
                    }
                }

                // Texto de puntos sin la palabra "puntos"
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                ) {
                    Text(
                        text = "${formatNumber(perfil.totalScore)}/${formatNumber(siguienteNivel)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))  // Espaciado mínimo entre amarillo y cyan

            // Columna derecha: Nickname, Badge y Descripción (misma altura total que columna izquierda)
            Column(
                modifier = Modifier
                    .weight(1f)
                    // Avatar(91dp) + spacing(6dp) + texto de puntos(aprox 20dp) + paddings = ~120dp total
                    .height(120.dp)  // Ajustado para compensar avatar más pequeño
                    .padding(2.dp),  // Padding mínimo
                verticalArrangement = Arrangement.spacedBy(10.dp),  // Espaciado vertical balanceado
                horizontalAlignment = Alignment.Start
            ) {
                // Nombre del usuario con badge de verificado
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = perfil.nickname,
                        style = MaterialTheme.typography.titleMedium.copy(  // Tamaño intermedio
                            fontWeight = FontWeight.Medium,  // Menos pesado que Bold
                            fontSize = 19.sp,  // Un punto más grande que titleMedium estándar
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Badge de verificado
                    android.util.Log.d("PerfilPersonalLogueadoScreen", "🔍 UI VERIFICACION PERFIL PROPIO - Usuario: ${perfil.nickname}, isVerified: ${perfil.isVerified}")
                    if (perfil.isVerified == true) {
                        android.util.Log.d("PerfilPersonalLogueadoScreen", "✅ MOSTRANDO BADGE para: ${perfil.nickname}")
                        Spacer(modifier = Modifier.width(5.dp))  // Ajustado proporcionalmente
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = BiihliveBlue,
                            modifier = Modifier.size(16.dp)  // Reducido proporcionalmente
                        )
                    } else {
                        android.util.Log.d("PerfilPersonalLogueadoScreen", "❌ NO MOSTRANDO BADGE para: ${perfil.nickname}")
                    }
                }

                // Badge de nivel (centro)
                Box(
                    modifier = Modifier
                        .background(BiihliveOrangeLight, CircleShape)  // Naranja light
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nivel:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = LevelCalculator.calculateLevel(perfil.totalScore).toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,  // ✅ Cambiado: Bold → Medium (mismo peso que "Nivel:")
                                color = Color.White,
                                fontSize = 11.sp  // ✅ Reducido: 13.sp → 11.sp (más pequeño)
                            )
                        )
                    }
                }

                // Descripción (abajo, con más líneas para llenar espacio)
                Text(
                    text = perfil.description.ifEmpty { "Sin descripción" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 5,  // Aumentado a 5 líneas
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Nueva sección de dos columnas (placeholder)
        // Función para abreviar texto largo
        fun abbreviate(text: String, maxLength: Int = 13): String {
            return if (text.length > maxLength) {
                // Para textos con "de" lo abreviamos inteligentemente
                if (text.contains(" de ")) {
                    val parts = text.split(" de ")
                    if (parts.size >= 2) {
                        return parts[0].take(1) + ". de " + parts[1].take(6)
                    }
                }
                text.take(maxLength - 1) + "."
            } else {
                text
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 4.dp)  // Sin padding superior para estar más cerca
                .padding(horizontal = 6.dp, vertical = 2.dp),  // Padding vertical reducido
            horizontalArrangement = Arrangement.Start
        ) {
            // Columna izquierda - mismo ancho que columna del avatar
            Column(
                modifier = Modifier
                    .width(109.dp)  // Mismo ancho que sección amarilla (101dp avatar + 8dp padding total)
                    .padding(4.dp),  // Padding reducido
                horizontalAlignment = Alignment.Start  // Alineado a la izquierda
            ) {
                // Localidad
                Text(
                    text = abbreviate(perfil.ubicacion.ciudad.ifEmpty { "-" }),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,  // Líneas más juntas
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Provincia
                Text(
                    text = abbreviate(perfil.ubicacion.provincia.ifEmpty { "-" }),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,  // Líneas más juntas
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // País
                Text(
                    text = abbreviate(perfil.ubicacion.pais.ifEmpty { "-" }),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,  // Líneas más juntas
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))  // Espaciado reducido

            // Columna derecha - ocupa el resto del espacio
            Row(
                modifier = Modifier
                    .weight(1f)  // Ocupa el espacio restante
                    .padding(4.dp),  // Padding reducido
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Editar Perfil
                OutlinedButton(
                    onClick = onNavigateToEditarPerfil,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),  // Altura aumentada para mejor legibilidad
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = BiihliveBlue
                    ),
                    border = BorderStroke(1.dp, BiihliveBlue),
                    contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp),  // Padding mínimo
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Editar Perfil",
                        fontSize = 12.sp,  // Aumentado a 12sp para texto más corto
                        fontWeight = FontWeight.Medium,
                        color = BiihliveBlue,
                        maxLines = 1
                    )
                }

                // Botón Suscripciones
                OutlinedButton(
                    onClick = onNavigateToSuscripciones,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),  // Altura aumentada para mejor legibilidad
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = BiihliveBlue
                    ),
                    border = BorderStroke(1.dp, BiihliveBlue),
                    contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp),  // Padding mínimo
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Suscripciones",
                        fontSize = 11.sp,  // Mantener 11sp para texto más largo
                        fontWeight = FontWeight.Medium,
                        color = BiihliveBlue,
                        maxLines = 1
                    )
                }
            }
        }

        // Estadísticas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EstadisticaItem(
                numero = formatNumber(followersCount),
                etiqueta = "Seguidores",
                onClick = onNavigateToFollowers
            )

            EstadisticaItem(
                numero = formatNumber(followingCount),
                etiqueta = "Siguiendo",
                onClick = onNavigateToFollowing
            )

            EstadisticaItem(
                numero = "0",  // TODO: Implementar conteo real de grupos
                etiqueta = "Grupos",
                onClick = onNavigateToGroups
            )

            EstadisticaItem(
                numero = rankingPosition,  // ✅ Posición real según preferencia del usuario
                etiqueta = rankingScope,   // ✅ Ámbito real según preferencia del usuario
                isRanking = true,  // Para aplicar color celeste al ámbito
                onClick = onNavigateToRanking
            )
        }
    }
}

@Composable
private fun EstadisticaItem(
    numero: String,
    etiqueta: String,
    onClick: (() -> Unit)? = null,
    isRanking: Boolean = false  // Para aplicar color especial al ranking
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(8.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = numero,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface  // Gris medio para números importantes
            )
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall.copy(
                color = when {
                    isRanking -> BiihliveBlue  // Celeste para el ámbito del ranking
                    onClick != null -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun SeccionMultimedia(
    perfil: PerfilUsuario,
    onUploadToGallery: () -> Unit,
    galleryImages: List<com.mision.biihlive.data.aws.S3ClientProvider.GalleryImage>,
    isLoadingGallery: Boolean,
    hasMoreImages: Boolean,
    onLoadGallery: () -> Unit,
    onLoadMoreImages: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        ScrollableTabRow(  // ✅ Cambiado a ScrollableTabRow para 4 tabs
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BiihliveBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = BiihliveOrangeLight,
                    height = 2.dp
                )
            },
            edgePadding = 0.dp  // Sin padding para aprovechar espacio
        ) {
            // Tab 0: Fotos
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Fotos ${if (perfil.fotosCount > 0) "(${perfil.fotosCount})" else ""}",
                        color = if (selectedTab == 0) BiihliveOrangeLight else BiihliveBlue,
                        fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp  // ✅ Texto más pequeño para caber 4 tabs
                    )
                }
            )
            
            // Tab 1: Videos
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Videos ${if (perfil.videosCount > 0) "(${perfil.videosCount})" else ""}",
                        color = if (selectedTab == 1) BiihliveOrangeLight else BiihliveBlue,
                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
            
            // Tab 2: Fotos + (Contenido exclusivo)
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fotos +",
                            color = if (selectedTab == 2) BiihliveOrangeLight else BiihliveBlue,
                            fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Contenido exclusivo",
                            tint = if (selectedTab == 2) BiihliveOrangeLight else BiihliveBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            )
            
            // Tab 3: Videos + (Contenido exclusivo)
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Videos +",
                            color = if (selectedTab == 3) BiihliveOrangeLight else BiihliveBlue,
                            fontWeight = if (selectedTab == 3) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Contenido exclusivo",
                            tint = if (selectedTab == 3) BiihliveOrangeLight else BiihliveBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))  // Espaciado mínimo antes de tabs

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> GaleriaFotos(
                    perfil = perfil,
                    onUploadToGallery = onUploadToGallery,
                    galleryImages = galleryImages,
                    isLoadingGallery = isLoadingGallery,
                    hasMoreImages = hasMoreImages,
                    onLoadGallery = onLoadGallery,
                    onLoadMoreImages = onLoadMoreImages
                )
                1 -> GaleriaVideos(perfil)
                2 -> GaleriaFotosExclusivas(
                    perfil = perfil,
                    onUploadToGallery = onUploadToGallery
                    // TODO: Agregar parámetros para galería exclusiva cuando esté lista
                )
                3 -> GaleriaVideosExclusivos(perfil)
            }
        }
    }
}

@Composable
private fun GaleriaFotos(
    perfil: PerfilUsuario,
    onUploadToGallery: () -> Unit,
    galleryImages: List<com.mision.biihlive.data.aws.S3ClientProvider.GalleryImage>,
    isLoadingGallery: Boolean,
    hasMoreImages: Boolean,
    onLoadGallery: () -> Unit,
    onLoadMoreImages: () -> Unit
) {
    // Estado para el diálogo fullscreen
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    // Cargar galería cuando se muestra el tab por primera vez
    LaunchedEffect(Unit) {
        if (galleryImages.isEmpty() && !isLoadingGallery) {
            onLoadGallery()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp)
    ) {
        when {
            // Si no hay imágenes y no está cargando, mostrar mensaje vacío
            galleryImages.isEmpty() && !isLoadingGallery -> {
                EmptyGalleryMessage(
                    icon = Icons.Default.Image,
                    message = "Aún no has subido fotos",
                    buttonText = "Subir Primera Foto",
                    onButtonClick = onUploadToGallery
                )
            }
            // Si está cargando las primeras imágenes
            galleryImages.isEmpty() && isLoadingGallery -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = BiihliveOrangeLight
                    )
                }
            }
            // Si hay imágenes, mostrar el grid como columnas y filas
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp),  // Padding reducido
                    verticalArrangement = Arrangement.spacedBy(1.dp)  // Espaciado reducido
                ) {
                    // Agrupar las imágenes en filas de 3
                    galleryImages.chunked(3).forEach { rowImages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)  // Espaciado reducido
                        ) {
                            rowImages.forEach { image ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    GalleryImageItem(
                                        imageUrl = image.thumbnailUrl,
                                        onClick = {
                                            selectedImageIndex = galleryImages.indexOf(image)
                                        }
                                    )
                                }
                            }
                            // Rellenar espacios vacíos si la fila no está completa
                            repeat(3 - rowImages.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Botón para cargar más imágenes
                    if (hasMoreImages) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingGallery) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = BiihliveOrangeLight
                                )
                            } else {
                                TextButton(
                                    onClick = onLoadMoreImages
                                ) {
                                    Text("Cargar más fotos")
                                }
                            }
                        }
                    }
                }

                // FAB circular para subir más fotos
                FloatingActionButton(
                    onClick = onUploadToGallery,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = BiihliveOrangeLight,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Subir foto")
                }
            }
        }
    }

    // Diálogo de imagen fullscreen
    selectedImageIndex?.let { index ->
        FullScreenGalleryDialog(
            images = galleryImages,
            initialIndex = index,
            onDismiss = { selectedImageIndex = null }
        )
    }
}

@Composable
private fun GaleriaVideos(perfil: PerfilUsuario) {
    Column {
        if (perfil.videosCount == 0) {
            EmptyGalleryMessage(
                icon = Icons.Default.Videocam,
                message = "Aún no has subido videos",
                buttonText = "Subir Primer Video",
                onButtonClick = { /* TODO: Implementar subida */ }
            )
        } else {
            // TODO: Mostrar galería de videos desde S3
            Text("Galería de videos próximamente")
        }
    }
}

@Composable
private fun EmptyGalleryMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onButtonClick,
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText)
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("Cerrar")
            }
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(
    isVisible: Boolean,
    perfil: PerfilUsuario,
    shouldBypassImageCache: Boolean = false,
    profileImageUrl: String? = null,
    onDismiss: () -> Unit,
    onChangePhoto: () -> Unit
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { onDismiss() }
            ) {
                // Contenedor de la imagen con el badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(bottom = 80.dp) // Espacio para que no se superponga con nav bar
                ) {
                    // Imagen full con la URL pasada como parámetro
                    val imageUrl = profileImageUrl

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(false)  // Sin crossfade para carga instantánea
                            // Deshabilitar caché si hay bypass activo
                            .diskCachePolicy(
                                if (shouldBypassImageCache) coil.request.CachePolicy.DISABLED
                                else coil.request.CachePolicy.ENABLED
                            )
                            .memoryCachePolicy(
                                if (shouldBypassImageCache) coil.request.CachePolicy.DISABLED
                                else coil.request.CachePolicy.ENABLED
                            )
                            .memoryCacheKey("profile_full_${perfil.userId}")
                            .diskCacheKey("profile_full_${perfil.userId}")
                            .build(),
                        contentDescription = "Foto de Perfil Full",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                        placeholder = painterResource(R.drawable.ic_default_avatar),
                        error = painterResource(R.drawable.ic_default_avatar),
                        fallback = painterResource(R.drawable.ic_default_avatar)
                    )

                    // Badge flotante sobre la imagen (esquina inferior derecha de la imagen)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp) // Padding desde el borde de la imagen
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(BiihliveOrange)
                            .clickable(onClick = onChangePhoto),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Cambiar foto",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Botón cerrar
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryImageItem(
    imageUrl: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)  // Relación de aspecto 1:1 (cuadrado)
            .padding(0.5.dp)  // Padding reducido
            .clip(RoundedCornerShape(8.dp))  // Bordes redondeados como proyectobase
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .size(300)  // Tamaño fijo como proyectobase
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullScreenGalleryDialog(
    images: List<com.mision.biihlive.data.aws.S3ClientProvider.GalleryImage>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Pager vertical para swipe entre imágenes
            val pagerState = rememberPagerState(
                initialPage = initialIndex,
                pageCount = { images.size }
            )

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onDismiss()
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(images[page].fullUrl)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                    )
                }
            }

            // Indicador de página
            if (images.size > 1) {
                LaunchedEffect(pagerState.currentPage) {
                    currentIndex = pagerState.currentPage
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(images.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentIndex) Color.White
                                    else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }

            // Botón cerrar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }
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
            withContext(Dispatchers.IO) {
                val imageLoader = ImageLoader(context)
                val request = CoilImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Necesario para acceder al bitmap
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null && !bitmap.isRecycled) {
                        // Crear palette desde el bitmap
                        val palette = Palette.from(bitmap).generate()

                        // Log para debugging
                        Log.d("DominantColor", "🎨 Analizando imagen: $imageUrl")
                        Log.d("DominantColor", "🎨 Swatches disponibles - Vibrant: ${palette.vibrantSwatch != null}, LightVibrant: ${palette.lightVibrantSwatch != null}, DarkVibrant: ${palette.darkVibrantSwatch != null}")
                        Log.d("DominantColor", "🎨 Swatches disponibles - Muted: ${palette.mutedSwatch != null}, LightMuted: ${palette.lightMutedSwatch != null}, DarkMuted: ${palette.darkMutedSwatch != null}")

                        // Intentar obtener diferentes tipos de colores en orden de preferencia
                        val (extractedColor, colorType) = when {
                            palette.vibrantSwatch != null -> palette.vibrantSwatch!!.rgb to "Vibrant"
                            palette.lightVibrantSwatch != null -> palette.lightVibrantSwatch!!.rgb to "LightVibrant"
                            palette.darkVibrantSwatch != null -> palette.darkVibrantSwatch!!.rgb to "DarkVibrant"
                            palette.mutedSwatch != null -> palette.mutedSwatch!!.rgb to "Muted"
                            palette.lightMutedSwatch != null -> palette.lightMutedSwatch!!.rgb to "LightMuted"
                            palette.darkMutedSwatch != null -> palette.darkMutedSwatch!!.rgb to "DarkMuted"
                            else -> palette.getDominantColor(fallbackColor.toArgb()) to "Dominant"
                        }

                        Log.d("DominantColor", "🎨 Color seleccionado: $colorType - ${String.format("#%06X", 0xFFFFFF and extractedColor)}")

                        withContext(Dispatchers.Main) {
                            dominantColor.value = Color(extractedColor).copy(alpha = 0.8f)  // Alpha más alto para mayor visibilidad
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DominantColor", "Error extracting dominant color from $imageUrl", e)
            dominantColor.value = fallbackColor
        }
    }

    return dominantColor
}

/**
 * Galería de fotos exclusivas (solo para suscriptores)
 */
@Composable
private fun GaleriaFotosExclusivas(
    perfil: PerfilUsuario,
    onUploadToGallery: () -> Unit
) {
    // TODO: Implementar galería exclusiva con lógica de suscripción
    // Por ahora, reutilizamos la interfaz de GaleriaFotos pero sin imágenes
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp)
    ) {
        EmptyGalleryMessage(
            icon = Icons.Default.Lock,
            message = "Contenido exclusivo para suscriptores\n\nSube fotos que solo tus suscriptores podrán ver",
            buttonText = "Subir Foto Exclusiva",
            onButtonClick = onUploadToGallery
        )
    }
}

/**
 * Galería de videos exclusivos (solo para suscriptores)
 */
@Composable
private fun GaleriaVideosExclusivos(
    perfil: PerfilUsuario
) {
    // TODO: Implementar galería exclusiva de videos
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Videos exclusivos",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Contenido exclusivo para suscriptores",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Los videos subidos aquí solo serán visibles para tus suscriptores",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { /* TODO: Implementar subida de videos exclusivos */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BiihliveOrangeLight
                ),
                border = BorderStroke(1.dp, BiihliveOrangeLight)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir Video Exclusivo")
            }
        }
    }
}