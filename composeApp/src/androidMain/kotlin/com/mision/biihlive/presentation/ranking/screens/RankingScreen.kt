package com.mision.biihlive.presentation.ranking.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import android.util.Log
import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest as CoilImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mision.biihlive.R
import com.mision.biihlive.components.StandardTopBar
import com.mision.biihlive.components.BiihliveNavigationBar
import com.mision.biihlive.data.aws.S3ClientProvider
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.navigation.Screen
import com.mision.biihlive.presentation.ranking.viewmodel.RankingUser
import com.mision.biihlive.presentation.ranking.viewmodel.RankingViewModel
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.ui.theme.BiihliveOrangeLighter
import com.mision.biihlive.utils.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    navController: NavController,
    onBackClick: () -> Unit = {},
    currentRoute: String = "",
    targetUserId: String? = null,  // Usuario específico para mostrar su posición
    initialTab: Int? = null        // Tab inicial basado en preferencia del usuario
) {
    val context = LocalContext.current

    // Crear ViewModel con dependencias
    val viewModel: RankingViewModel = remember {
        RankingViewModel(
            firestoreRepository = FirestoreRepository(),
            sessionManager = SessionManager,
            context = context,
            targetUserId = targetUserId,
            initialTab = initialTab
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Local", "Provincial", "Nacional", "Mundial", "Grupo")
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            StandardTopBar(
                title = "Ranking",
                onBackClick = onBackClick
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
        ScrollableTabRow(
            selectedTabIndex = uiState.currentTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BiihliveBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab]),
                    color = BiihliveOrangeLight,
                    height = 2.dp
                )
            },
            divider = {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
            },
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.currentTab == index,
                    onClick = { viewModel.switchTab(index) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (uiState.currentTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (uiState.currentTab == index) BiihliveOrangeLight else BiihliveBlue,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        // Mostrar error si existe
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Cerrar")
                    }
                }
            }
        }

        // Contenido del tab seleccionado
        when (uiState.currentTab) {
            0 -> RankingTabContent(
                users = uiState.localRanking,
                isLoading = uiState.loadingTabs.contains(0),
                tabName = "Local",
                navController = navController,
                targetUserId = targetUserId,
                coroutineScope = coroutineScope
            )
            1 -> RankingTabContent(
                users = uiState.provincialRanking,
                isLoading = uiState.loadingTabs.contains(1),
                tabName = "Provincial",
                navController = navController,
                targetUserId = targetUserId,
                coroutineScope = coroutineScope
            )
            2 -> RankingTabContent(
                users = uiState.nacionalRanking,
                isLoading = uiState.loadingTabs.contains(2),
                tabName = "Nacional",
                navController = navController,
                targetUserId = targetUserId,
                coroutineScope = coroutineScope
            )
            3 -> RankingTabContent(
                users = uiState.mundialRanking,
                isLoading = uiState.loadingTabs.contains(3),
                tabName = "Mundial",
                navController = navController,
                targetUserId = targetUserId,
                coroutineScope = coroutineScope
            )
            4 -> GroupContent() // Tab "Grupo" sin implementar aún
        }
        }
    }
}

@Composable
private fun RankingTabContent(
    users: List<RankingUser>,
    isLoading: Boolean,
    tabName: String,
    navController: NavController,
    targetUserId: String? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    // Debug logging
    Log.d("RankingTabContent", "$tabName - users.size: ${users.size}, isLoading: $isLoading")

    when {
        isLoading -> {
            // Loading más discreto - solo un pequeño indicador en la parte superior
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Pequeña barra de progreso en la parte superior
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = BiihliveBlue,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Cargando...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                // Resto del espacio vacío pero disponible para contenido
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        users.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📊",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay usuarios en este ranking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sé el primero en aparecer aquí",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
        else -> {
            // Buscar el usuario target en la lista si existe
            val targetUser = targetUserId?.let { id ->
                users.find { it.userId == id }
            }
            val targetPosition = targetUser?.let {
                users.indexOf(it) + 1  // Position is 1-indexed
            }

            // Mostrar la tarjeta fija solo si:
            // 1. Hay un targetUserId
            // 2. El usuario fue encontrado en la lista
            // 3. El usuario NO est\u00e1 en el podio (posici\u00f3n > 3)
            val shouldShowTargetCard = targetUser != null && targetPosition != null && targetPosition > 3

            // Estado del scroll para la animación del podio
            val listState = rememberLazyListState()

            // Calcular el factor de escala basado en el scroll
            val scrollOffset = listState.firstVisibleItemScrollOffset
            val firstVisibleIndex = listState.firstVisibleItemIndex

            // Crear animación suave del factor de escala
            val scaleFactor by animateFloatAsState(
                targetValue = if (scrollOffset > 50 || firstVisibleIndex > 0) 0.75f else 1f,  // ✅ 0.7f → 0.75f (elementos más grandes)
                label = "podium_scale"
            )

            // Calcular densidad para conversión dp→px (fuera del onClick)
            val density = androidx.compose.ui.platform.LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Lista de usuarios excluyendo el podio (top 3)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = if (users.isNotEmpty()) {
                            // Altura dinámica basada en contenido: avatar(70dp) + textos + padding
                            if (scaleFactor < 1f) 95.dp else 135.dp  // ✅ Ajustado: 100dp → 95dp (más compacto por bottom=0dp)
                        } else 0.dp,
                        bottom = if (shouldShowTargetCard) 140.dp else 80.dp
                    )
                ) {
                    // Calcular cuántos usuarios van al podio (mínimo 0, máximo 3)
                    val podiumUserCount = minOf(users.size, 3)
                    val usersWithoutPodium = users.drop(podiumUserCount)

                    itemsIndexed(usersWithoutPodium) { index, user ->
                        RankingUserItem(
                            position = index + podiumUserCount + 1, // Empezar después del podio
                            user = user,
                            onClick = { userId ->
                                navController.navigate(Screen.PerfilConsultado.createRoute(userId))
                            }
                        )
                    }
                }

                // Podio fijo encima de la lista - mostrar siempre si hay usuarios
                if (users.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .align(Alignment.TopCenter)
                    ) {
                        PodiumSection(
                            topUsers = users.take(3),
                            scaleFactor = scaleFactor,
                            onUserClick = { userId ->
                                navController.navigate(Screen.PerfilConsultado.createRoute(userId))
                            }
                        )

                        // Separador visual
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }

                // Tarjeta fija del usuario target si no est\u00e1 en el podio
                if (shouldShowTargetCard && targetUser != null && targetPosition != null) {
                    UserRankingCard(
                        user = targetUser,
                        position = targetPosition,
                        onClick = {
                            // Scroll hasta la posición real del usuario en la lista
                            val podiumUserCount = minOf(users.size, 3)
                            val targetIndexInList = targetPosition - podiumUserCount - 1

                            if (targetIndexInList >= 0) {
                                // Scroll a la posición con contexto (mostrar usuario anterior si existe)
                                val scrollToIndex = maxOf(0, targetIndexInList - 1)
                                coroutineScope.launch {
                                    listState.animateScrollToItem(
                                        index = scrollToIndex,
                                        scrollOffset = 0
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingUserItem(
    position: Int,
    user: RankingUser,
    onClick: (String) -> Unit
) {
    val context = LocalContext.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(user.userId) }
                .padding(horizontal = 16.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de perfil con borde dinámico
            val dominantColor by rememberDominantColor(
                imageUrl = user.thumbnailImageUrl ?: user.profileImageUrl
            )

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
                    model = ImageRequest.Builder(context)
                        .data(user.thumbnailImageUrl ?: user.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de Perfil",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_default_avatar),
                    error = painterResource(id = R.drawable.ic_default_avatar)
                )
            }

            // Información del usuario
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.nickname,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (user.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = BiihliveBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Ciudad y provincia
                if (user.ciudad.isNotEmpty() || user.provincia.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${user.ciudad}${if (user.ciudad.isNotEmpty() && user.provincia.isNotEmpty()) ", " else ""}${user.provincia}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 14.sp  // Reducir lineHeight para menos padding
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // País
                if (user.pais.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(1.dp))  // Reducido de 2dp a 1dp
                    Text(
                        text = user.pais,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 14.sp  // Reducir lineHeight para menos padding
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Posición y nivel uno arriba del otro, centrados
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Posición
                Text(
                    text = "${position}º",  // Formato ordinal
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,  // Cambiado a Bold
                        color = Color(0xFF4B4B4B),  // Gris oscuro
                        fontSize = 11.sp  // Reducido de 13sp a 11sp
                    )
                )

                // Nivel en badge
                Box(
                    modifier = Modifier
                        .background(
                            color = BiihliveOrangeLight.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = user.nivel.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 80.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun PodiumSection(
    topUsers: List<RankingUser>,
    scaleFactor: Float,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current

    if (topUsers.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()  // ✅ Adaptarse a la altura del contenido (cyan)
            // .background(Color(0xFFFFCCCC))  // 🔴 DEBUG: FONDO ROJO CLARO para Box principal
            // .border(2.dp, Color.Red)  // 🔴 DEBUG: Borde rojo
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (scaleFactor < 1f) 2.dp else 12.dp,
                bottom = if (scaleFactor < 1f) 0.dp else 8.dp  // ✅ Reducido: 2dp → 0dp (más compacto abajo)
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),  // ✅ Adaptarse a la altura del contenido
                // .background(Color(0xFFCCCCFF))  // 🔵 DEBUG: FONDO AZUL CLARO para Row
                // .border(2.dp, Color.Blue),  // 🔵 DEBUG: Borde azul
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            // 2do lugar (izquierda) - SIEMPRE ocupa su espacio
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()  // ✅ Adaptarse a la altura del contenido
                    // .background(Color(0xFFFFFFCC))  // 🟡 DEBUG: FONDO AMARILLO CLARO para Box 2do lugar
                    // .border(2.dp, Color.Yellow)  // 🟡 DEBUG: Borde amarillo
                ,
                contentAlignment = Alignment.Center
            ) {
                if (topUsers.size >= 2) {
                    PodiumUser(
                        user = topUsers[1],
                        position = 2,
                        modifier = Modifier,  // ✅ SIN fillMaxWidth - solo wrap_content
                        scaleFactor = scaleFactor,
                        onUserClick = onUserClick
                    )
                }
            }

            // 1er lugar (centro) - SIEMPRE ocupa su espacio
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()  // ✅ Adaptarse a la altura del contenido
                    // .background(Color(0xFFCCFFCC))  // 🟢 DEBUG: FONDO VERDE CLARO para Box 1er lugar
                    // .border(2.dp, Color.Green)  // 🟢 DEBUG: Borde verde
                ,
                contentAlignment = Alignment.Center
            ) {
                PodiumUser(
                    user = topUsers[0],
                    position = 1,
                    modifier = Modifier,  // ✅ SIN fillMaxWidth - solo wrap_content
                    isWinner = true,
                    scaleFactor = scaleFactor,
                    onUserClick = onUserClick
                )
            }

            // 3er lugar (derecha) - SIEMPRE ocupa su espacio
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()  // ✅ Adaptarse a la altura del contenido
                    // .background(Color(0xFFFFCCFF))  // 🟣 DEBUG: FONDO MAGENTA CLARO para Box 3er lugar
                    // .border(2.dp, Color.Magenta)  // 🟣 DEBUG: Borde magenta
                ,
                contentAlignment = Alignment.Center
            ) {
                if (topUsers.size >= 3) {
                    PodiumUser(
                        user = topUsers[2],
                        position = 3,
                        modifier = Modifier,  // ✅ SIN fillMaxWidth - solo wrap_content
                        scaleFactor = scaleFactor,
                        onUserClick = onUserClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    maxLines: Int = 1,
    minTextSize: androidx.compose.ui.unit.TextUnit = 8.sp,
    textAlign: TextAlign? = null
) {
    var textSize by remember { mutableStateOf(style.fontSize) }

    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = textSize),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Visible,
        textAlign = textAlign,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                val newSize = textSize * 0.9f
                if (newSize >= minTextSize) {
                    textSize = newSize
                }
            }
        }
    )
}

@Composable
private fun PodiumUser(
    user: RankingUser,
    position: Int,
    modifier: Modifier = Modifier,
    isWinner: Boolean = false,
    scaleFactor: Float = 1f,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current

    // Colores del borde del avatar - coinciden con las medallas/badges
    val borderColor = when (position) {
        1 -> Color(0xFFB8860B)  // Dorado oscuro (matching badge oro)
        2 -> Color(0xFFA8A8A8)  // Plata media (matching badge plata)
        3 -> Color(0xFFA0522D)  // Bronce rojizo (matching badge bronce)
        else -> Color.Transparent
    }

    // Colores para la medalla/ribbon de posición
    val positionColor = when (position) {
        1 -> Color(0xFFB8860B)  // Dorado oscuro
        2 -> Color(0xFFA8A8A8)  // Plata media
        3 -> Color(0xFFA0522D)  // Bronce rojizo
        else -> Color.Gray
    }

    // ✅ IGNORAR modifier externo y usar solo wrap_content
    Column(
        modifier = Modifier  // ✅ Empezar desde Modifier vacío, ignorando el parámetro
            .wrapContentSize()  // ✅ FORZAR wrap_content en ambas dimensiones
            // .background(Color(0xFFCCFFFF))  // 🔵 DEBUG: FONDO CYAN CLARO para Column
            .clickable { onUserClick(user.userId) }
            // .border(2.dp, Color.Cyan)  // 🔵 DEBUG: Borde cyan
            .graphicsLayer {
                scaleX = scaleFactor
                scaleY = scaleFactor
                transformOrigin = TransformOrigin(0.5f, 0.5f)  // ✅ Cambiar a centro-centro para mejor centrado
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(bottom = 0.dp)
                // .background(Color(0xFFFFE0B2))  // 🟠 DEBUG: FONDO NARANJA CLARO para Box del avatar
                // .border(2.dp, Color(0xFFFF9800))  // 🟠 DEBUG: Borde naranja
        ) {
            // Avatar con borde - TODOS del mismo tamaño (70.dp)
            Box(
                modifier = Modifier
                    .size(70.dp)  // Tamaño uniforme para las 3 posiciones
                    .background(
                        color = borderColor,
                        shape = CircleShape
                    )
                    .padding(if (borderColor != Color.Transparent) 3.dp else 0.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(user.profileImageUrl ?: user.thumbnailImageUrl)
                        .crossfade(true)
                        .memoryCacheKey("ranking_${user.userId}")
                        .diskCacheKey("ranking_${user.userId}")
                        .build(),
                    contentDescription = "Foto de ${user.nickname}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_default_avatar),
                    error = painterResource(id = R.drawable.ic_default_avatar)
                )
            }

            // Número de posición (superior izquierda) - tocando el borde del avatar
            Box(
                modifier = Modifier
                    .offset(
                        x = -7.dp,  // Offset uniforme para todos (antes era condicional)
                        y = -7.dp
                    )
                    .align(Alignment.TopStart)
            ) {
                // Ribbon/Listón de posición
                PositionRibbon(
                    position = position,
                    color = positionColor
                )
            }

          
            // Badge de nivel en posición 6 en punto (180° desde las 12) - dentro del avatar
            val avatarRadius = 35.dp  // Radio uniforme para todos (avatar de 70.dp)
            val borderWidth = 3.dp
            val imageRadius = avatarRadius - borderWidth // Radio real de la imagen
            val badgeHeight = 12.dp // Altura aproximada del badge
            val distanceFromCenter = (imageRadius - (badgeHeight / 2)) * 1.1f // 10% más abajo
            
            val angle = 180.0 // 6 en punto = 180° desde las 12
            val angleRad = Math.toRadians(angle)
            val offsetX = (distanceFromCenter.value * kotlin.math.sin(angleRad)).dp // = 0 para 180°
            val offsetY = -(distanceFromCenter.value * kotlin.math.cos(angleRad)).dp // = distancia hacia abajo
            
            // Color del badge según la posición - con gradientes como las medallas
            val (badgeGradient, badgeBorder) = when (position) {
                1 -> Pair(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF4E5B0), // Dorado claro superior
                            Color(0xFFB8860B)  // Dorado oscuro inferior
                        )
                    ),
                    Color(0xFF8B6914) // Borde marrón dorado
                )
                2 -> Pair(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8E8E8), // Plateado claro superior
                            Color(0xFFA8A8A8)  // Gris medio inferior
                        )
                    ),
                    Color(0xFF707070) // Borde gris oscuro
                )
                3 -> Pair(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE6A679), // Bronce claro superior
                            Color(0xFFA0522D)  // Marrón rojizo inferior
                        )
                    ),
                    Color(0xFF704214) // Borde marrón oscuro
                )
                else -> Pair(
                    Brush.verticalGradient(
                        colors = listOf(
                            BiihliveOrangeLight,
                            BiihliveOrangeLight
                        )
                    ),
                    BiihliveOrangeLight
                )
            }
            
            Box(
                modifier = Modifier
                    .offset(
                        x = offsetX,
                        y = offsetY
                    )
                    .align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = badgeGradient,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 0.5.dp,  // Borde muy fino
                            color = badgeBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = user.nivel.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.4f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                blurRadius = 1f
                            )
                        )
                    )
                }
            }
        }

        // Spacer reducido entre avatar y nombre
        Spacer(modifier = Modifier.height(if (scaleFactor < 1f) 1.dp else 2.dp))  // ✅ Reducido de 2dp/4dp a 1dp/2dp

        // Nombre con auto-resize
        AutoResizeText(
            text = user.nickname,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),  // ✅ Padding para que el centrado sea visible
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            minTextSize = 10.sp,
            textAlign = TextAlign.Center
        )

        // Ciudad y provincia
        if (user.ciudad.isNotEmpty() || user.provincia.isNotEmpty()) {
            Spacer(modifier = Modifier.height(if (scaleFactor < 1f) 0.5.dp else 1.dp))  // Reducido 1.5dp → 1dp
            Text(
                text = buildString {
                    if (user.ciudad.isNotEmpty()) append(user.ciudad)
                    if (user.ciudad.isNotEmpty() && user.provincia.isNotEmpty()) append(", ")
                    if (user.provincia.isNotEmpty()) append(user.provincia)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),  // ✅ Padding para que el centrado sea visible
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 12.sp  // Agregar lineHeight compacto
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        // País en línea separada
        if (user.pais.isNotEmpty()) {
            Spacer(modifier = Modifier.height(if (scaleFactor < 1f) 0.5.dp else 1.dp))  // Reducido 1.5dp → 1dp
            Text(
                text = user.pais,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),  // ✅ Padding para que el centrado sea visible
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 12.sp  // Agregar lineHeight compacto
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PositionRibbon(
    position: Int,
    color: Color
) {
    // ====== VERSIÓN CON IMÁGENES PNG (ACTIVA) ======
    val medalResource = when (position) {
        1 -> R.drawable.medal_gold
        2 -> R.drawable.medal_silver
        3 -> R.drawable.medal_bronze
        else -> R.drawable.medal_bronze
    }
    
    Box(
        modifier = Modifier.size(27.dp),  // Reducido de 30dp a 27dp (10% más pequeña)
        contentAlignment = Alignment.Center
    ) {
        // Imagen de la medalla/listón
        Image(
            painter = painterResource(id = medalResource),
            contentDescription = "Medalla posición $position",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Número superpuesto en blanco - centrado en el círculo
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,  // Reducido proporcionalmente de 13sp a 12sp
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 3f
                )
            )
            // Sin offset - número centrado en la medalla circular
        )
    }
    
    /* ====== VERSIÓN CON ICONOS MATERIAL - COPAS (COMENTADA) ======
    val medalColor = when (position) {
        1 -> Color(0xFFFFD700) // Oro brillante
        2 -> Color(0xFFC0C0C0) // Plata
        3 -> Color(0xFFCD7F32) // Bronce
        else -> Color.Gray
    }
    
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Icono de copa sin fondo
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = "Medalla posición $position",
            modifier = Modifier.size(36.dp),
            tint = medalColor
        )
        
        // Número dentro de la copa
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 2f
                )
            ),
            modifier = Modifier.offset(y = (-2).dp) // Ajustar posición dentro de la copa
        )
    }
    */
}

@Composable
private fun GroupContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔧",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Función en desarrollo",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "El ranking por grupos estará disponible próximamente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatScore(score: Int): String {
    return when {
        score >= 1_000_000 -> String.format("%.1fM", score / 1_000_000.0)
        score >= 1_000 -> String.format("%.1fK", score / 1_000.0)
        else -> score.toString()
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
            // Si hay error, usar color fallback
            dominantColor.value = fallbackColor
        }
    }

    return dominantColor
}

@Composable
private fun UserRankingCard(
    user: RankingUser,
    position: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ✅ ESTRUCTURA IDÉNTICA A RankingUserItem pero con fondo celeste
    Column(
        modifier = modifier
            .background(Color(0xFFF0F9FF))  // Fondo celeste clarito para destacar
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 9.dp),  // ✅ MISMO padding que RankingUserItem
            horizontalArrangement = Arrangement.spacedBy(11.dp),  // ✅ MISMO spacing que RankingUserItem
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Foto de perfil con borde dinámico (IGUAL que RankingUserItem)
            val dominantColor by rememberDominantColor(
                imageUrl = user.thumbnailImageUrl ?: user.profileImageUrl
            )

            Box(
                modifier = Modifier
                    .size(53.dp)  // ✅ MISMO tamaño que RankingUserItem
                    .background(
                        color = dominantColor,
                        shape = CircleShape
                    )
                    .padding(2.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(user.thumbnailImageUrl ?: user.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de Perfil",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_default_avatar),
                    error = painterResource(id = R.drawable.ic_default_avatar)
                )
            }

            // ✅ Información del usuario (IGUAL que RankingUserItem)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.nickname,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,  // ✅ MISMO peso que RankingUserItem
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (user.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = BiihliveBlue,
                            modifier = Modifier.size(14.dp)  // ✅ MISMO tamaño que RankingUserItem
                        )
                    }
                }

                // Ciudad y provincia
                if (user.ciudad.isNotEmpty() || user.provincia.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(1.dp))  // ✅ MISMO espaciado
                    Text(
                        text = "${user.ciudad}${if (user.ciudad.isNotEmpty() && user.provincia.isNotEmpty()) ", " else ""}${user.provincia}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // País
                if (user.pais.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(1.dp))  // ✅ MISMO espaciado
                    Text(
                        text = user.pais,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ✅ Posición y nivel (IGUAL que RankingUserItem)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Posición
                Text(
                    text = "${position}º",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B4B4B),
                        fontSize = 11.sp
                    )
                )

                // Nivel en badge
                Box(
                    modifier = Modifier
                        .background(
                            color = BiihliveOrangeLight.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(10.dp)  // ✅ MISMO radio que RankingUserItem
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)  // ✅ MISMO padding que RankingUserItem
                ) {
                    Text(
                        text = user.nivel.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // ✅ Divisor superior sutil (opcional, para separar del contenido de arriba)
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 80.dp),  // ✅ MISMO padding que RankingUserItem
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
    }
}