package com.mision.biihlive.presentation.perfil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.domain.users.model.UserPreview
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.ui.theme.BiihliveOrange
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.ui.theme.DonationRed
import com.mision.biihlive.utils.formatNumber
import com.mision.biihlive.R
import com.mision.biihlive.components.CircularProgressBar
import com.mision.biihlive.components.StandardDialog
import com.mision.biihlive.navigation.Screen
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.mision.biihlive.utils.LevelCalculator
import com.mision.biihlive.components.BiihliveNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilPublicoConsultadoScreen(
    navController: NavController,
    viewModel: PerfilPublicoConsultadoViewModel,
    currentRoute: String = ""
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFullImageDialog by remember { mutableStateOf(false) }
    var showUnfollowDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Sincronizar estado de refresh con UI
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    BackHandler {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.perfil?.nickname ?: "Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón de mensaje
                    uiState.perfil?.let { perfil ->
                        val context = LocalContext.current
                        IconButton(
                            onClick = {
                                val currentUserId = com.mision.biihlive.utils.SessionManager.getUserId(context)
                                currentUserId?.let { currentId ->
                                    // Generar chatId consistente (IDs ordenados alfabéticamente)
                                    val chatId = if (currentId < perfil.userId) {
                                        "chat_${currentId}_${perfil.userId}"
                                    } else {
                                        "chat_${perfil.userId}_${currentId}"
                                    }

                                    navController.navigate(
                                        Screen.Chat.createRoute(
                                            chatId = chatId,
                                            displayName = perfil.nickname
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Message,
                                contentDescription = "Enviar mensaje",
                                tint = BiihliveBlue
                            )
                        }
                    }

                    // Botón de opciones
                    IconButton(onClick = { /* TODO: Implementar opciones */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        "profile" -> navController.navigate(Screen.PerfilUsuario.route)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.hasError -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.limpiarError() },
                        onDismiss = { viewModel.limpiarError() }
                    )
                }

                uiState.isContentVisible -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            // Recargar perfil usando el userId del perfil actual
                            uiState.perfil?.let { perfil ->
                                viewModel.cargarPerfilDeUsuario(perfil.userId)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                        ) {
                        item {
                            PerfilPublicoConsultadoInfo(
                                perfil = uiState.perfil!!,
                                uiState = uiState,
                                siguienteNivel = uiState.siguienteNivel,
                                progreso = uiState.progreso,
                                followersCount = uiState.perfil!!.seguidores,
                                followingCount = uiState.perfil!!.siguiendo,
                                onShowFullImage = { showFullImageDialog = true },
                                profileImageUrl = uiState.profileImageUrl,
                                profileThumbnailUrl = uiState.profileThumbnailUrl,
                                isFollowing = uiState.isFollowing,
                                isLoadingFollow = uiState.isLoadingFollow,
                                onToggleFollow = {
                                    if (uiState.isFollowing) {
                                        // Si ya lo sigue, mostrar diálogo de confirmación
                                        showUnfollowDialog = true
                                    } else {
                                        // Si no lo sigue, seguir directamente
                                        viewModel.toggleFollow()
                                    }
                                },
                                onNavigateToFollowers = {
                                    navController.navigate(Screen.FollowersFollowing.createRoute(uiState.perfil!!.userId, 0))
                                },
                                onNavigateToFollowing = {
                                    navController.navigate(Screen.FollowersFollowing.createRoute(uiState.perfil!!.userId, 1))
                                },
                                onNavigateToGroups = {
                                    navController.navigate(Screen.Grupos.createRoute(uiState.perfil!!.userId))
                                },
                                onNavigateToRanking = {
                                    // Obtener la preferencia de ranking del perfil y mapearla a índice de tab
                                    val rankingPreference = uiState.perfil?.rankingPreference
                                    val initialTab = com.mision.biihlive.presentation.ranking.viewmodel.RankingViewModel
                                        .mapPreferenceToTabIndex(rankingPreference)
                                    navController.navigate(Screen.Ranking.createRoute(uiState.perfil!!.userId, initialTab))
                                },
                                onNavigateToPatrocinar = {
                                    navController.navigate(Screen.Patrocinar.createRoute(uiState.perfil!!.userId))
                                },
                                onNavigateToSuscripcion = {
                                    navController.navigate(Screen.Suscripcion.createRoute(uiState.perfil!!.userId))
                                },
                                isSuscrito = uiState.isSuscrito,
                                isLoadingSuscripcion = uiState.isLoadingSuscripcion,
                                previewFollowers = uiState.previewFollowers,
                                isLoadingPreviewFollowers = uiState.isLoadingPreviewFollowers
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))  // Reducido de 12dp a 4dp para consistencia
                        }

                        item {
                            SeccionMultimediaConsultado(
                                perfil = uiState.perfil!!,
                                galleryImages = uiState.galleryImages,
                                isLoadingGallery = uiState.isLoadingGallery,
                                hasMoreImages = uiState.hasMoreGalleryImages,
                                onLoadGallery = { viewModel.loadGalleryImages() },
                                onLoadMoreImages = { viewModel.loadGalleryImages(loadMore = true) },
                                isSuscrito = uiState.isSuscrito,  // ✅ Agregar estado de suscripción
                                navController = navController
                            )
                        }
                    }
                }
            }

                else -> {
                    CircularProgressIndicator(color = BiihliveOrangeLight)
                }
            }
        }

        // Dialog para mostrar imagen full
        if (uiState.perfil != null) {
            FullScreenImageDialogConsultado(
                isVisible = showFullImageDialog,
                perfil = uiState.perfil!!,
                profileImageUrl = uiState.profileImageUrl,
                onDismiss = { showFullImageDialog = false }
            )
        }

        // Diálogo de confirmación para dejar de seguir
        if (showUnfollowDialog && uiState.perfil != null) {
            StandardDialog(
                title = "Dejar de seguir",
                message = "¿Quieres dejar de seguir a ${uiState.perfil!!.nickname}?",
                confirmText = "Dejar de seguir",
                dismissText = "Cancelar",
                onConfirm = {
                    viewModel.toggleFollow()
                    showUnfollowDialog = false
                },
                onDismiss = {
                    showUnfollowDialog = false
                },
                showDialog = showUnfollowDialog,
                isDangerous = true // Es una acción que podría ser no deseada
            )
        }
    }
}

@Composable
private fun PerfilPublicoConsultadoInfo(
    perfil: PerfilUsuario,
    uiState: PerfilUiState,
    siguienteNivel: Int,
    progreso: Double,
    followersCount: Int,
    followingCount: Int,
    onShowFullImage: () -> Unit,
    profileImageUrl: String? = null,
    profileThumbnailUrl: String? = null,
    isFollowing: Boolean = false,
    isLoadingFollow: Boolean = false,
    onToggleFollow: () -> Unit = {},
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onNavigateToPatrocinar: () -> Unit = {},
    onNavigateToSuscripcion: () -> Unit = {},
    isSuscrito: Boolean = false,
    isLoadingSuscripcion: Boolean = false,
    previewFollowers: List<UserPreview> = emptyList(),
    isLoadingPreviewFollowers: Boolean = false
) {
    // Variable para mostrar/ocultar bordes de debug
    val showDebugBorders = false // Cambiar a true para ver bordes de debug
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(if(showDebugBorders) Modifier.border(1.dp, Color.Magenta, RoundedCornerShape(8.dp)) else Modifier),  // BORDE MAGENTA - Contenedor Principal [DEBUG]
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)  // Espaciado uniforme entre secciones
    ) {
        // Sección superior: Avatar + Info usuario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if(showDebugBorders) Modifier.border(0.5.dp, Color.Green, RoundedCornerShape(8.dp)) else Modifier)  // BORDE VERDE - Row Principal [DEBUG]
                .padding(2.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Columna izquierda: Avatar y puntos
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .then(if(showDebugBorders) Modifier.border(0.5.dp, Color.Yellow, RoundedCornerShape(4.dp)) else Modifier)  // BORDE AMARILLO - Columna Avatar [DEBUG]
                    .padding(2.dp)
            ) {
                // Avatar con barra de progreso circular y borde dinámico
                Box(modifier = Modifier
                    .size(91.dp)  // Reducido 10%
                    .then(if(showDebugBorders) Modifier.border(0.5.dp, Color(0xFF00FFFF), CircleShape) else Modifier)  // BORDE TURQUESA - Box Avatar [DEBUG]
                ) {
                    // Obtener color dominante del avatar
                    val dominantColor by rememberDominantColor(
                        imageUrl = profileThumbnailUrl
                    )

                    CircularProgressBar(
                        progress = progreso.toFloat(),
                        size = 91.dp,  // Reducido 10%
                        strokeWidth = 7.dp,  // Ajustado proporcionalmente
                        progressColor = BiihliveOrangeLight,
                        backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
                            val imageUrl = profileThumbnailUrl

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(false)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
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
                        .then(if(showDebugBorders) Modifier.border(0.5.dp, Color(0xFFFFA500), RoundedCornerShape(4.dp)) else Modifier)  // BORDE NARANJA - Puntos [DEBUG]
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

            Spacer(modifier = Modifier.width(8.dp))

            // Columna derecha: Nickname, Badge y Descripción
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)  // Ajustado para compensar avatar más pequeño
                    .then(if(showDebugBorders) Modifier.border(0.5.dp, Color.Cyan, RoundedCornerShape(4.dp)) else Modifier)  // BORDE CYAN - Columna Info [DEBUG]
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Nombre del usuario con badge de verificado
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = perfil.nickname,
                        style = MaterialTheme.typography.titleMedium.copy(  // Reducido proporcionalmente
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Badge de verificado
                    android.util.Log.d("PerfilPublicoConsultadoScreen", "🔍 UI VERIFICACION PERFIL CONSULTADO - Usuario: ${perfil.nickname}, isVerified: ${perfil.isVerified}")
                    if (perfil.isVerified == true) {
                        android.util.Log.d("PerfilPublicoConsultadoScreen", "✅ MOSTRANDO BADGE para: ${perfil.nickname}")
                        Spacer(modifier = Modifier.width(5.dp))  // Ajustado proporcionalmente
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = BiihliveBlue,
                            modifier = Modifier.size(16.dp)  // Reducido proporcionalmente
                        )
                    } else {
                        android.util.Log.d("PerfilPublicoConsultadoScreen", "❌ NO MOSTRANDO BADGE para: ${perfil.nickname}")
                    }
                }

                // Badge de nivel
                Box(
                    modifier = Modifier
                        .background(BiihliveOrangeLight, CircleShape)
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

                // Descripción
                Text(
                    text = perfil.description.ifEmpty { "Sin descripción" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Sección de ubicación y botones
        fun abbreviate(text: String, maxLength: Int = 13): String {
            return if (text.length > maxLength) {
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
                .padding(top = 0.dp, bottom = 4.dp)
                .then(if(showDebugBorders) Modifier.border(0.5.dp, Color.Gray, RoundedCornerShape(8.dp)) else Modifier)  // BORDE GRIS - Row ubicación/botones [DEBUG]
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            // Columna izquierda - ubicación
            Column(
                modifier = Modifier
                    .width(109.dp)
                    .then(if(showDebugBorders) Modifier.border(0.5.dp, Color.Red, RoundedCornerShape(4.dp)) else Modifier)  // BORDE ROJO - Columna ubicación [DEBUG]
                    .padding(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Localidad
                Text(
                    text = abbreviate(perfil.ubicacion.ciudad.ifEmpty { "-" }),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Provincia
                Text(
                    text = abbreviate(perfil.ubicacion.provincia.ifEmpty { "-" }),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // País
                Text(
                    text = abbreviate(perfil.ubicacion.pais.ifEmpty { "-" }),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Columna derecha - botones
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(if(showDebugBorders) Modifier.border(0.5.dp, Color.Blue, RoundedCornerShape(4.dp)) else Modifier)  // BORDE AZUL - Row botones [DEBUG]
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estados temporales (TODO: Conectar con ViewModel para donación)
                var hasDonated by remember { mutableStateOf(false) }

                // Botón Suscribirse (estados dinámicos según suscripción)
                if (isLoadingSuscripcion) {
                    // Estado de carga
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = BiihliveOrangeLight
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = BiihliveOrangeLight
                        ),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        enabled = false
                    ) {
                        CircularProgressIndicator(
                            color = BiihliveOrangeLight,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp
                        )
                    }
                } else if (isSuscrito) {
                    // Estado suscrito (borde celeste, texto celeste)
                    OutlinedButton(
                        onClick = { onNavigateToSuscripcion() },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = BiihliveBlue
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = BiihliveBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Suscrito",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = BiihliveBlue
                        )
                    }
                } else {
                    // Estado no suscrito (borde naranja, texto naranja)
                    OutlinedButton(
                        onClick = { onNavigateToSuscripcion() },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = BiihliveOrangeLight
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = BiihliveOrangeLight
                        ),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Suscribirse",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = BiihliveOrangeLight
                        )
                    }
                }

                // Botón Seguir
                if (isLoadingFollow) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = BiihliveOrangeLight
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleFollow,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = if (isFollowing) BiihliveBlue else BiihliveOrangeLight
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isFollowing) BiihliveBlue else BiihliveOrangeLight
                        ),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isFollowing) "Siguiendo" else "Seguir",
                            fontSize = if (isFollowing) 11.sp else 12.sp,  // Dinámico: "Siguiendo" 11sp, "Seguir" 12sp
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Botón Donar/Ayuda (estilo condicional)
                if (perfil.donacion) {
                    // Modo Donar: Borde celeste, texto celeste, fondo blanco
                    OutlinedButton(
                        onClick = { hasDonated = !hasDonated },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = BiihliveBlue
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = BiihliveBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = BiihliveBlue
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Donar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = BiihliveBlue
                            )
                        }
                    }
                } else {
                    // Modo Ayuda: Rojo sólido con cruz
                    Button(
                        onClick = { hasDonated = !hasDonated },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DonationRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        color = DonationRed,
                                        shape = CircleShape
                                    )
                                    .drawBehind {
                                        val strokeWidth = 2.5.dp.toPx()
                                        val center = size.width / 2
                                        val crossSize = size.width * 0.3f

                                        // Línea horizontal de la cruz
                                        drawLine(
                                            color = Color.White,
                                            start = Offset(center - crossSize, center),
                                            end = Offset(center + crossSize, center),
                                            strokeWidth = strokeWidth,
                                            cap = StrokeCap.Round
                                        )

                                        // Línea vertical de la cruz
                                        drawLine(
                                            color = Color.White,
                                            start = Offset(center, center - crossSize),
                                            end = Offset(center, center + crossSize),
                                            strokeWidth = strokeWidth,
                                            cap = StrokeCap.Round
                                        )
                                    }
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Ayuda",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Estadísticas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if(showDebugBorders) Modifier.border(0.5.dp, Color(0xFFFF6B6B), RoundedCornerShape(8.dp)) else Modifier)  // BORDE ROJO CLARO - Estadísticas [DEBUG]
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EstadisticaItemConsultado(
                numero = formatNumber(followersCount),
                etiqueta = "Seguidores",
                onClick = onNavigateToFollowers
            )

            EstadisticaItemConsultado(
                numero = formatNumber(followingCount),
                etiqueta = "Siguiendo",
                onClick = onNavigateToFollowing
            )

            EstadisticaItemConsultado(
                numero = "0",  // TODO: Implementar conteo real de grupos
                etiqueta = "Grupos",
                onClick = onNavigateToGroups
            )

            EstadisticaItemConsultado(
                numero = uiState.rankingPosition,  // ✅ Posición real según preferencia del usuario
                etiqueta = uiState.rankingScope,   // ✅ Ámbito real según preferencia del usuario
                isRanking = true,
                onClick = onNavigateToRanking
            )
        }

        // Sección de seguidores con preview estilo Instagram
        SeccionSeguidoresPreview(
            previewFollowers = previewFollowers,
            followersCount = followersCount,
            isLoading = isLoadingPreviewFollowers,
            onNavigateToFollowers = onNavigateToFollowers,
            showDebugBorders = showDebugBorders
        )

        // Sección de patrocinio - Dinámica según usuario
        SeccionPatrocinio(
            perfil = perfil,
            profileThumbnailUrl = profileThumbnailUrl,
            uiState = uiState,
            onPatrocinioClick = onNavigateToPatrocinar,
            showDebugBorders = showDebugBorders
        )
    }
}

@Composable
private fun EstadisticaItemConsultado(
    numero: String,
    etiqueta: String,
    onClick: (() -> Unit)? = null,
    isRanking: Boolean = false
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
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall.copy(
                color = when {
                    isRanking -> BiihliveBlue
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
private fun SeccionMultimediaConsultado(
    perfil: PerfilUsuario,
    galleryImages: List<com.mision.biihlive.data.aws.S3ClientProvider.GalleryImage>,
    isLoadingGallery: Boolean,
    hasMoreImages: Boolean,
    onLoadGallery: () -> Unit,
    onLoadMoreImages: () -> Unit,
    isSuscrito: Boolean = false,  // ✅ Estado de suscripción
    navController: NavController
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
            edgePadding = 0.dp
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
                        fontSize = 13.sp
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
                        // Candado dinámico según suscripción
                        Icon(
                            imageVector = if (isSuscrito) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (isSuscrito) "Acceso desbloqueado" else "Contenido exclusivo",
                            tint = if (isSuscrito) Color(0xFF4CAF50) else if (selectedTab == 2) BiihliveOrangeLight else BiihliveBlue,  // Verde si está suscrito
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
                        // Candado dinámico según suscripción
                        Icon(
                            imageVector = if (isSuscrito) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (isSuscrito) "Acceso desbloqueado" else "Contenido exclusivo",
                            tint = if (isSuscrito) Color(0xFF4CAF50) else if (selectedTab == 3) BiihliveOrangeLight else BiihliveBlue,  // Verde si está suscrito
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> GaleriaFotosConsultado(
                    perfil = perfil,
                    galleryImages = galleryImages,
                    isLoadingGallery = isLoadingGallery,
                    hasMoreImages = hasMoreImages,
                    onLoadGallery = onLoadGallery,
                    onLoadMoreImages = onLoadMoreImages
                )
                1 -> GaleriaVideosConsultado(perfil)
                2 -> GaleriaFotosExclusivasConsultado(
                    perfil = perfil,
                    isSuscrito = isSuscrito,
                    navController = navController
                    // TODO: Agregar galleryImagesExclusive cuando esté listo
                )
                3 -> GaleriaVideosExclusivosConsultado(
                    perfil = perfil,
                    isSuscrito = isSuscrito,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun GaleriaFotosConsultado(
    perfil: PerfilUsuario,
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
                EmptyGalleryMessageConsultado(
                    icon = Icons.Default.Image,
                    message = "Este usuario no ha subido fotos"
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
            // Si hay imágenes, mostrar el grid
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Agrupar las imágenes en filas de 3
                    galleryImages.chunked(3).forEach { rowImages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            rowImages.forEach { image ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    GalleryImageItemConsultado(
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
            }
        }
    }

    // Diálogo de imagen fullscreen
    selectedImageIndex?.let { index ->
        FullScreenGalleryDialogConsultado(
            images = galleryImages,
            initialIndex = index,
            onDismiss = { selectedImageIndex = null }
        )
    }
}

@Composable
private fun GaleriaVideosConsultado(perfil: PerfilUsuario) {
    Column {
        if (perfil.videosCount == 0) {
            EmptyGalleryMessageConsultado(
                icon = Icons.Default.Videocam,
                message = "Este usuario no ha subido videos"
            )
        } else {
            // TODO: Mostrar galería de videos desde S3
            Text("Galería de videos próximamente")
        }
    }
}

@Composable
private fun EmptyGalleryMessageConsultado(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
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
fun FullScreenImageDialogConsultado(
    isVisible: Boolean,
    perfil: PerfilUsuario,
    profileImageUrl: String? = null,
    onDismiss: () -> Unit
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
                // Contenedor de la imagen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(bottom = 80.dp)
                ) {
                    val imageUrl = profileImageUrl

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(false)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
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
private fun GalleryImageItemConsultado(
    imageUrl: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(0.5.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .size(300)
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
private fun FullScreenGalleryDialogConsultado(
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

@Composable
private fun SeccionPatrocinio(
    perfil: PerfilUsuario,
    profileThumbnailUrl: String?,
    uiState: PerfilUiState,
    onPatrocinioClick: () -> Unit,
    showDebugBorders: Boolean = false
) {
    // Obtener color dominante del avatar del usuario
    val dominantColor by rememberDominantColor(
        imageUrl = profileThumbnailUrl,
        fallbackColor = BiihliveBlue
    )

    if (uiState.tienePatrocinador && uiState.patrocinadorActual != null) {
        // Card completo de patrocinio con datos reales del patrocinador
        CardPatrocinioCompleto(
            patrocinador = uiState.patrocinadorActual,
            dominantColor = dominantColor,
            onPatrocinioClick = onPatrocinioClick,
            showDebugBorders = showDebugBorders
        )
    } else {
        // Botón pequeño "Patrocíname" para usuarios sin patrocinador
        BotonPatrocinio(
            perfil = perfil,
            dominantColor = dominantColor,
            onPatrocinioClick = onPatrocinioClick,
            showDebugBorders = showDebugBorders
        )
    }
}

@Composable
private fun CardPatrocinioCompleto(
    patrocinador: PerfilUsuario,
    dominantColor: Color,
    onPatrocinioClick: () -> Unit,
    showDebugBorders: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .clickable { onPatrocinioClick() }
            .then(if(showDebugBorders) Modifier.border(0.5.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp)) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = BiihliveBlue.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Avatar de Hugo (patrocinador)
            Box(
                modifier = Modifier.size(50.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, dominantColor.copy(alpha = 0.6f), CircleShape),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(generatePatrocinadorAvatarUrl(patrocinador.userId))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar del patrocinador",
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

            Spacer(modifier = Modifier.width(12.dp))

            // Información del patrocinio
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Solo el nickname del patrocinador (sin "Patrocinado por")
                Text(
                    text = "@${patrocinador.nickname}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = dominantColor,
                        fontSize = 12.sp,  // ✅ Reducido: 14sp → 12sp
                        fontWeight = FontWeight.Bold
                    )
                )

                // Mensaje promocional
                Text(
                    text = patrocinador.description?.takeIf { it.isNotBlank() }
                        ?: "🎯 ¡Apoya contenido de calidad! Nivel ${patrocinador.nivel}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Ícono de enlace
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Abrir patrocinio",
                tint = dominantColor.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun BotonPatrocinio(
    perfil: PerfilUsuario,
    dominantColor: Color,
    onPatrocinioClick: () -> Unit,
    showDebugBorders: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clickable { onPatrocinioClick() }
            .then(if(showDebugBorders) Modifier.border(0.5.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp)) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (perfil.tipo == "empresa") {
                dominantColor.copy(alpha = 0.2f)  // Empresa: diseño actual
            } else {
                Color.White  // Persona: fondo blanco
            }
        ),
        border = BorderStroke(
            1.dp,
            if (perfil.tipo == "empresa") dominantColor else BiihliveBlue  // Persona: borde celeste
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Patrocíname",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (perfil.tipo == "empresa") {
                        dominantColor  // Empresa: color dominante
                    } else {
                        BiihliveBlue  // Persona: texto celeste
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun SeccionSeguidoresPreview(
    previewFollowers: List<UserPreview>,
    followersCount: Int,
    isLoading: Boolean,
    onNavigateToFollowers: () -> Unit,
    showDebugBorders: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onNavigateToFollowers() }
            .then(if(showDebugBorders) Modifier.border(0.5.dp, Color(0xFF9C27B0), RoundedCornerShape(8.dp)) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (isLoading) {
            // Estado de carga
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (previewFollowers.isEmpty() && followersCount == 0) {
            // Sin seguidores
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin seguidores aún",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            // Contenido con avatares superpuestos y texto
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Avatares superpuestos
                if (previewFollowers.isNotEmpty()) {
                    val avatarCount = previewFollowers.take(4).size
                    val totalWidth = 40.dp + ((avatarCount - 1) * 20).dp // 40dp base + 20dp offset por avatar adicional
                    AvataresSuperPuestos(
                        usuarios = previewFollowers.take(4),
                        modifier = Modifier.width(totalWidth)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Texto dinámico
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    TextoSeguidoresDinamico(
                        previewFollowers = previewFollowers,
                        followersCount = followersCount
                    )
                }
            }
        }
    }
}

@Composable
private fun AvataresSuperPuestos(
    usuarios: List<UserPreview>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.height(40.dp)) {
        usuarios.forEachIndexed { index, usuario ->
            val offsetX = (index * 20).dp // 50% de superposición (40dp de diámetro * 0.5)
            val zIndex = (usuarios.size - index).toFloat() // Primer avatar tiene mayor z-index

            Card(
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = offsetX)
                    .zIndex(zIndex) // El primero se ve completo, los demás atrás
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                val imageUrl = obtenerUrlDinamicaAvatar(usuario)
                Log.d("AvatareSuperPuestos", "Usuario ${usuario.nickname}: URL = $imageUrl")
                Log.d("AvatareSuperPuestos", "PhotoUrl original: ${usuario.photoUrl}")

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .listener(
                            onStart = {
                                Log.d("AvatareSuperPuestos", "Iniciando carga imagen para ${usuario.nickname}")
                            },
                            onSuccess = { _, _ ->
                                Log.d("AvatareSuperPuestos", "✅ Imagen cargada exitosamente para ${usuario.nickname}")
                            },
                            onError = { _, throwable ->
                                Log.e("AvatareSuperPuestos", "❌ Error cargando imagen para ${usuario.nickname}: ${throwable.throwable.message}")
                            }
                        )
                        .build(),
                    contentDescription = "Avatar de ${usuario.nickname}",
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
}

@Composable
private fun obtenerUrlDinamicaAvatar(usuario: UserPreview): String {
    Log.d("obtenerUrlDinamicaAvatar", "🔍 Procesando usuario: ${usuario.nickname}")
    Log.d("obtenerUrlDinamicaAvatar", "📸 PhotoUrl procesada: '${usuario.photoUrl}'")

    // El ViewModel ya cargó las URLs reales de S3, solo usarlas
    val finalUrl = if (!usuario.photoUrl.isNullOrEmpty()) {
        Log.d("obtenerUrlDinamicaAvatar", "✅ Usando photoUrl de S3")
        usuario.photoUrl!!
    } else {
        Log.d("obtenerUrlDinamicaAvatar", "⚠️ Sin photoUrl, usuario sin imagen de perfil")
        "" // Vacío para usar placeholder
    }

    Log.d("obtenerUrlDinamicaAvatar", "🎯 URL final: '$finalUrl'")
    return finalUrl
}

@Composable
private fun TextoSeguidoresDinamico(
    previewFollowers: List<UserPreview>,
    followersCount: Int
) {
    if (previewFollowers.isEmpty()) {
        Text(
            text = if (followersCount > 0) {
                "$followersCount seguidor${if (followersCount > 1) "es" else ""}"
            } else {
                "Sin seguidores"
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        )
    } else {
        // Construir texto dinámico como Instagram
        val texto = buildString {
            when (previewFollowers.size) {
                1 -> {
                    append("Le sigue ")
                    append(previewFollowers[0].nickname)
                    if (followersCount > 1) {
                        append(" y ${followersCount - 1} persona${if (followersCount - 1 > 1) "s" else ""} más")
                    }
                }
                2 -> {
                    append("Le siguen ")
                    append(previewFollowers[0].nickname)
                    append(", ")
                    append(previewFollowers[1].nickname)
                    if (followersCount > 2) {
                        append(" y ${followersCount - 2} persona${if (followersCount - 2 > 1) "s" else ""} más")
                    }
                }
                else -> {
                    append("Le siguen ")
                    append(previewFollowers[0].nickname)
                    append(", ")
                    append(previewFollowers[1].nickname)
                    if (followersCount > 3) {
                        append(" y ${followersCount - 2} persona${if (followersCount - 2 > 1) "s" else ""} más")
                    } else if (previewFollowers.size > 2) {
                        append(", ")
                        append(previewFollowers[2].nickname)
                        if (followersCount > 3) {
                            append(" y ${followersCount - 3} persona${if (followersCount - 3 > 1) "s" else ""} más")
                        }
                    }
                }
            }
        }

        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Galería de fotos exclusivas del perfil público (solo para suscriptores)
 */
@Composable
private fun GaleriaFotosExclusivasConsultado(
    perfil: PerfilUsuario,
    isSuscrito: Boolean,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSuscrito) {
            // Si está suscrito, mostrar galería (por ahora vacía)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Acceso desbloqueado",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)  // Verde
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "✅ Tienes acceso al contenido exclusivo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${perfil.nickname} aún no ha subido fotos exclusivas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Si NO está suscrito, mostrar mensaje de bloqueo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Contenido bloqueado",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Contenido exclusivo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Suscríbete para ver las fotos exclusivas de ${perfil.nickname}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        navController.navigate(Screen.Suscripcion.createRoute(perfil.userId))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiihliveOrangeLight
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suscribirse Ahora")
                }
            }
        }
    }
}

/**
 * Galería de videos exclusivos del perfil público (solo para suscriptores)
 */
@Composable
private fun GaleriaVideosExclusivosConsultado(
    perfil: PerfilUsuario,
    isSuscrito: Boolean,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSuscrito) {
            // Si está suscrito, mostrar galería (por ahora vacía)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Acceso desbloqueado",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)  // Verde
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "✅ Tienes acceso al contenido exclusivo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${perfil.nickname} aún no ha subido videos exclusivos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Si NO está suscrito, mostrar mensaje de bloqueo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Contenido bloqueado",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Contenido exclusivo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Suscríbete para ver los videos exclusivos de ${perfil.nickname}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        navController.navigate(Screen.Suscripcion.createRoute(perfil.userId))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiihliveOrangeLight
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suscribirse Ahora")
                }
            }
        }
    }
}

/**
 * Genera la URL del avatar del patrocinador usando el patrón CloudFront establecido
 */
private fun generatePatrocinadorAvatarUrl(userId: String): String {
    val CLOUDFRONT_URL = "https://d183hg75gdabnr.cloudfront.net"
    val DEFAULT_TIMESTAMP = "1759240530172"
    return "$CLOUDFRONT_URL/userprofile/$userId/thumbnail_$DEFAULT_TIMESTAMP.png"
}

