package com.mision.biihlive.presentation.social.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.palette.graphics.Palette
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest as CoilImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mision.biihlive.data.chat.repository.ChatFirestoreRepository
import com.mision.biihlive.navigation.Screen
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.mision.biihlive.domain.users.model.UserPreview
import com.mision.biihlive.presentation.social.viewmodel.FollowersFollowingViewModel
import com.mision.biihlive.R
import com.mision.biihlive.components.StandardDialog
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.components.BiihliveNavigationBar
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersFollowingScreen(
    navController: NavController,
    viewModel: FollowersFollowingViewModel,
    currentRoute: String = ""
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var showUnfollowDialog by remember { mutableStateOf(false) }
    var userToUnfollow by remember { mutableStateOf<UserPreview?>(null) }
    val context = LocalContext.current

    // Colores corporativos
    // Usando colores del tema global
    val naranja = Color(0xFFDC5A01)
    val verde = Color(0xFF60BF19)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.currentTab) {
                            0 -> "Seguidores"
                            1 -> "Siguiendo"
                            else -> "Seguidores"
                        },
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
            // Tabs (reducido espacio superior)
            TabRow(
                selectedTabIndex = uiState.currentTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 0.dp), // Reduce espacio superior
                contentColor = BiihliveBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab]),
                        color = BiihliveOrangeLight,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    selected = uiState.currentTab == 0,
                    onClick = { viewModel.switchTab(0) },
                    text = {
                        Text(
                            text = "Seguidores",
                            color = if (uiState.currentTab == 0) BiihliveOrangeLight else BiihliveBlue,
                            fontWeight = if (uiState.currentTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                )
                Tab(
                    selected = uiState.currentTab == 1,
                    onClick = { viewModel.switchTab(1) },
                    text = {
                        Text(
                            text = "Siguiendo",
                            color = if (uiState.currentTab == 1) BiihliveOrangeLight else BiihliveBlue,
                            fontWeight = if (uiState.currentTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                )
            }

            // Barra de búsqueda
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // Campo de búsqueda personalizado con padding mínimo (como UsersSearchScreen)
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

            // Lista con pull to refresh
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = uiState.isLoading),
                onRefresh = { viewModel.refresh() }
            ) {
                val currentList = when (uiState.currentTab) {
                    0 -> uiState.filteredFollowers
                    1 -> uiState.filteredFollowing
                    else -> uiState.filteredFollowers
                }

                when {
                    uiState.isLoading && currentList.isEmpty() -> {
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
                                    onClick = { viewModel.refresh() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BiihliveBlue
                                    )
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }

                    currentList.isEmpty() -> {
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
                                    text = when {
                                        searchText.isNotEmpty() -> "No se encontraron usuarios"
                                        uiState.currentTab == 0 -> "Sin seguidores aún"
                                        uiState.currentTab == 1 -> "No sigues a nadie aún"
                                        else -> "Sin seguidores aún"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    else -> {
                        val listState = rememberLazyListState()

                        // Detectar cuando llegamos al final de la lista para cargar más
                        LaunchedEffect(listState) {
                            snapshotFlow {
                                val layoutInfo = listState.layoutInfo
                                val totalItemsNumber = layoutInfo.totalItemsCount
                                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                // Cargar más cuando estemos cerca del final (10 items antes del final para anticipar)
                                lastVisibleItemIndex >= totalItemsNumber - 10
                            }.collect { shouldLoadMore ->
                                if (shouldLoadMore && !uiState.isLoadingMore && uiState.searchQuery.isEmpty()) {
                                    viewModel.loadMore()
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp)
                        ) {
                            items(
                                items = currentList,
                                key = { it.userId }
                            ) { user ->
                                FollowerFollowingItem(
                                    user = user,
                                    isFollowing = uiState.followingUsers.contains(user.userId),
                                    isLoadingFollow = uiState.loadingFollow.contains(user.userId),
                                    showFollowButton = uiState.currentTab == 1, // Solo mostrar menú en "Siguiendo"
                                    onClick = {
                                        // Navegar al perfil del usuario
                                        navController.navigate(Screen.PerfilConsultado.createRoute(user.userId))
                                    },
                                    onFollowClick = {
                                        if (uiState.currentTab == 1 && uiState.followingUsers.contains(user.userId)) {
                                            // Si está en "Siguiendo" y ya lo sigue, mostrar diálogo
                                            userToUnfollow = user
                                            showUnfollowDialog = true
                                        } else {
                                            viewModel.toggleFollow(user.userId)
                                        }
                                    },
                                    onSendMessage = {
                                        // Navegar directamente al chat con este usuario
                                        val currentUserId = com.mision.biihlive.utils.SessionManager.getUserId(context)
                                        currentUserId?.let { currentId ->
                                            // Generar chatId consistente (IDs ordenados alfabéticamente)
                                            val chatId = if (currentId < user.userId) {
                                                "chat_${currentId}_${user.userId}"
                                            } else {
                                                "chat_${user.userId}_${currentId}"
                                            }

                                            navController.navigate(
                                                Screen.Chat.createRoute(
                                                    chatId = chatId,
                                                    displayName = user.nickname
                                                )
                                            )
                                        }
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 80.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            // Indicador de carga de más usuarios
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = BiihliveBlue,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }

                            // Mensaje cuando no hay más datos
                            val hasMore = when (uiState.currentTab) {
                                0 -> uiState.hasMoreFollowers
                                1 -> uiState.hasMoreFollowing
                                else -> uiState.hasMoreFollowers
                            }

                            if (!hasMore && currentList.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Fin de la lista",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            fontSize = 12.sp
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
            isDangerous = true
        )
    }
}

@Composable
private fun FollowerFollowingItem(
    user: UserPreview,
    isFollowing: Boolean = false,
    isLoadingFollow: Boolean = false,
    showFollowButton: Boolean = true,
    onClick: () -> Unit,
    onFollowClick: () -> Unit,
    onSendMessage: () -> Unit = {}
) {
    val verde = Color(0xFF60BF19)
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con borde dinámico e indicador de online
        Box {
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
                    .padding(2.dp) // Espacio para el borde
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.imageUrl)
                        .crossfade(200)
                        .size(112, 112)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCacheKey(user.userId)
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

            // Indicador de online (usando el campo isOnline del usuario)
            if (user.isOnline && user.mostrarEstado) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(verde, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(11.dp))

        // Información del usuario
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Badge de verificado
                android.util.Log.d("FollowersFollowingScreen", "🔍 UI VERIFICACION FOLLOWERS/FOLLOWING - Usuario: ${user.nickname}, isVerified: ${user.isVerified}")
                if (user.isVerified) {
                    android.util.Log.d("FollowersFollowingScreen", "✅ MOSTRANDO BADGE para: ${user.nickname}")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = Color(0xFF1DC3FF),
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    android.util.Log.d("FollowersFollowingScreen", "❌ NO MOSTRANDO BADGE para: ${user.nickname}")
                }
            }

            // Descripción
            if (!user.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Menú de tres puntos (solo en pestaña "Siguiendo")
        if (showFollowButton && isFollowing) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Dejar de seguir",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PersonRemove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onFollowClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Enviar mensaje") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = null,
                                tint = BiihliveBlue
                            )
                        },
                        onClick = {
                            showMenu = false
                            onSendMessage()
                        }
                    )
                }
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