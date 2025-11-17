package com.mision.biihlive.presentation.users.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.LocalTextStyle
import androidx.palette.graphics.Palette
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest as CoilImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import com.mision.biihlive.navigation.Screen
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.mision.biihlive.domain.users.model.UserPreview
import com.mision.biihlive.presentation.users.viewmodel.UsersSearchViewModel
import com.mision.biihlive.R
import com.mision.biihlive.components.StandardDialog
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.ui.theme.BiihliveGreen
import com.mision.biihlive.ui.theme.BiihliveOrange
import com.mision.biihlive.components.BiihliveNavigationBar

/**
 * Pantalla de búsqueda y lista de usuarios para iniciar chat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersSearchScreen(
    navController: NavController,
    viewModel: UsersSearchViewModel,
    currentRoute: String = "",
    mode: String = "profile"  // "profile" para ver perfiles, "chat" para iniciar chat
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var showUnfollowDialog by remember { mutableStateOf(false) }
    var userToUnfollow by remember { mutableStateOf<UserPreview?>(null) }
    val context = LocalContext.current

    // Los estados de seguimiento ahora se cargan automáticamente con los usuarios
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (mode) {
                            "chat" -> "Nuevo mensaje"
                            else -> "Buscar usuarios"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    // Botón para filtrar solo usuarios online
                    IconButton(
                        onClick = { viewModel.toggleOnlineOnly() }
                    ) {
                        Icon(
                            imageVector = if (uiState.showOnlineOnly) {
                                Icons.Default.PersonOutline
                            } else {
                                Icons.Default.Person
                            },
                            contentDescription = if (uiState.showOnlineOnly) {
                                "Mostrar todos"
                            } else {
                                "Solo online"
                            },
                            tint = if (uiState.showOnlineOnly) BiihliveGreen else MaterialTheme.colorScheme.outline
                        )
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
                        "home" -> navController.navigate(com.mision.biihlive.navigation.Screen.Home.route)
                        "events" -> { /* Navegar a eventos */ }
                        "live" -> { /* Navegar a live */ }
                        "messages" -> navController.navigate(com.mision.biihlive.navigation.Screen.MessagesList.route)
                        "profile" -> navController.navigate(com.mision.biihlive.navigation.Screen.PerfilUsuario.route)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Barra de búsqueda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Campo de búsqueda personalizado con padding mínimo
                var isFocused by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isFocused) BiihliveBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = BiihliveBlue,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchText,
                            onValueChange = { query ->
                                searchText = query
                                viewModel.searchUsers(query)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = Color.Black
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchText.isEmpty()) {
                                    Text(
                                        text = "Buscar por nombre...",
                                        fontSize = 14.sp,
                                        color = Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (searchText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchText = ""
                                    viewModel.searchUsers("")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Indicador de filtro activo
            if (uiState.showOnlineOnly) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = BiihliveGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = BiihliveGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mostrando solo usuarios en línea",
                            fontSize = 14.sp,
                            color = BiihliveGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Lista de usuarios con pull to refresh
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = uiState.isLoading),
                onRefresh = { viewModel.refreshUsers() }
            ) {
                when {
                    uiState.isLoading && uiState.users.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BiihliveBlue)
                        }
                    }
                    
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = uiState.error ?: "Error desconocido",
                                    color = Color.Red
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.refreshUsers() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BiihliveBlue
                                    )
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                    
                    uiState.filteredUsers.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchText.isNotEmpty()) {
                                        "No se encontraron usuarios"
                                    } else if (uiState.showOnlineOnly) {
                                        "No hay usuarios en línea"
                                    } else {
                                        "No hay usuarios disponibles"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    
                    else -> {
                        val listState = rememberLazyListState()

                        // Detectar cuando llegamos al final de la lista
                        LaunchedEffect(listState) {
                            snapshotFlow {
                                val layoutInfo = listState.layoutInfo
                                val totalItemsNumber = layoutInfo.totalItemsCount
                                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

                                // Cargar más cuando estemos cerca del final (últimos 5 elementos)
                                lastVisibleItemIndex >= totalItemsNumber - 5
                            }.collect { shouldLoadMore ->
                                if (shouldLoadMore && !uiState.isLoadingMore && uiState.searchQuery.isEmpty()) {
                                    viewModel.loadMoreUsers()
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.filteredUsers,
                                key = { it.userId }
                            ) { user ->
                                UserItem(
                                    user = user,
                                    isFollowing = uiState.followingUsers.contains(user.userId),
                                    isLoadingFollow = uiState.loadingFollow.contains(user.userId),
                                    isCreatingChat = uiState.creatingChatForUser == user.userId,
                                    onClick = {
                                        when (mode) {
                                            "chat" -> {
                                                // Navegar directo SIN crear chat (enfoque optimista)
                                                // El chat se creará automáticamente cuando se envíe el primer mensaje
                                                val chatId = viewModel.generateDirectChatId(user.userId)
                                                navController.navigate(
                                                    Screen.Chat.createRoute(
                                                        chatId = chatId,
                                                        displayName = user.nickname
                                                    )
                                                ) {
                                                    // Limpiar el back stack para evitar volver a la búsqueda
                                                    popUpTo("messages_list") { inclusive = false }
                                                }
                                            }
                                            else -> {
                                                // Comportamiento por defecto: navegar al perfil del usuario
                                                navController.navigate(Screen.PerfilConsultado.createRoute(user.userId))
                                            }
                                        }
                                    },
                                    onFollowClick = {
                                        if (uiState.followingUsers.contains(user.userId)) {
                                            // Si ya lo sigue, mostrar diálogo de confirmación
                                            userToUnfollow = user
                                            showUnfollowDialog = true
                                        } else {
                                            // Si no lo sigue, seguir directamente
                                            viewModel.toggleFollow(user.userId)
                                        }
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 80.dp), // 16dp padding + 53dp avatar + 11dp spacing
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    thickness = 0.5.dp
                                )
                            }

                            // Indicador de carga de más usuarios
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = BiihliveBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Mensaje cuando no hay más datos
                            if (!uiState.hasMoreData && uiState.filteredUsers.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No hay más usuarios por cargar",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para dejar de seguir
    if (userToUnfollow != null) {
        StandardDialog(
            title = "Dejar de seguir",
            message = "¿Quieres dejar de seguir a ${userToUnfollow?.nickname}?",
            confirmText = "Dejar de seguir",
            dismissText = "Cancelar",
            onConfirm = {
                userToUnfollow?.let {
                    viewModel.toggleFollow(it.userId)
                }
                showUnfollowDialog = false
                userToUnfollow = null
            },
            onDismiss = {
                showUnfollowDialog = false
                userToUnfollow = null
            },
            showDialog = showUnfollowDialog,
            isDangerous = true // Es una acción que podría ser no deseada
        )
    }
}

@Composable
private fun UserItem(
    user: UserPreview,
    isFollowing: Boolean = false,
    isLoadingFollow: Boolean = false,
    isCreatingChat: Boolean = false,
    onClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCreatingChat) { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp), // Padding reducido para filas más compactas
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con borde dinámico e indicador de online
        Box(
            modifier = Modifier.size(53.dp) // Avatar más pequeño (48dp en lugar de 56dp)
        ) {
            // Obtener color dominante del avatar
            val dominantColor by rememberDominantColor(
                imageUrl = user.imageUrl
            )

            // Container con borde dinámico
            Box(
                modifier = Modifier
                    .size(53.dp)
                    .background(
                        color = dominantColor,
                        shape = CircleShape
                    )
                    .padding(2.dp) // Borde más fino
            ) {
                // Imagen del avatar
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(user.imageUrl)
                        .crossfade(true)
                        .size(96, 96) // Tamaño ajustado
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCacheKey("thumb_${user.userId}")
                        .build(),
                    contentDescription = "Avatar de ${user.nickname}",
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

            // Indicador de estado online - misma lógica que el chat
            val shouldShowOnlineStatus = user.isOnline && user.mostrarEstado

            // Solo mostrar el indicador si el usuario está conectado Y permite ser visualizado
            if (shouldShowOnlineStatus) {
                Box(
                    modifier = Modifier
                        .size(11.dp) // Indicador más pequeño
                        .background(
                            color = BiihliveGreen,
                            shape = CircleShape
                        )
                        .offset(
                            x = (-8).dp, // 50% hacia adentro horizontalmente (más superpuesto)
                            y = (-8).dp  // 50% hacia adentro verticalmente (más superpuesto)
                        )
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(11.dp)) // Spacing reducido

        // Información del usuario
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.nickname,
                    style = MaterialTheme.typography.titleSmall, // 14sp Medium
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Badge de verificado más pequeño
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = BiihliveBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Descripción del usuario
            if (!user.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.description,
                    style = MaterialTheme.typography.bodySmall, // 12sp Normal
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }
        
        Spacer(modifier = Modifier.width(10.dp)) // Spacing reducido

        // Indicador de creación de chat
        if (isCreatingChat) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(90.dp), // Más ancho para el texto
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = BiihliveBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chat...",
                        fontSize = 10.sp,
                        color = BiihliveBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        // Botón de seguir más compacto
        else if (isLoadingFollow) {
            Box(
                modifier = Modifier
                    .height(28.dp) // Más pequeño
                    .width(75.dp), // Más estrecho
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = com.mision.biihlive.ui.theme.BiihliveOrangeLight
                )
            }
        } else if (isFollowing) {
            OutlinedButton(
                onClick = onFollowClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = BiihliveBlue
                ),
                border = BorderStroke(1.dp, BiihliveBlue),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), // Padding reducido
                modifier = Modifier
                    .height(28.dp) // Altura reducida
                    .width(75.dp) // Ancho reducido
            ) {
                Text(
                    text = "Siguiendo",
                    fontSize = 11.sp, // Texto más pequeño
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            OutlinedButton(
                onClick = onFollowClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = com.mision.biihlive.ui.theme.BiihliveOrangeLight
                ),
                border = BorderStroke(1.dp, com.mision.biihlive.ui.theme.BiihliveOrangeLight),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), // Padding reducido
                modifier = Modifier
                    .height(28.dp) // Altura reducida
                    .width(75.dp) // Ancho reducido
            ) {
                Text(
                    text = "Seguir",
                    fontSize = 11.sp, // Texto más pequeño
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                            dominantColor.value = Color(extractedColor).copy(alpha = 0.4f)
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