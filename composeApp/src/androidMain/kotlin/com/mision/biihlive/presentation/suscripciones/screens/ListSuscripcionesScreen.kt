package com.mision.biihlive.presentation.suscripciones.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.mision.biihlive.navigation.Screen
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mision.biihlive.domain.suscripciones.model.SuscripcionPreview
import com.mision.biihlive.R
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.components.BiihliveNavigationBar
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mision.biihlive.presentation.suscripciones.viewmodel.SuscripcionesViewModel
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.core.managers.UserIdManager
import com.mision.biihlive.domain.users.model.UserPreview
import androidx.compose.material3.TextButton
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSuscripcionesScreen(
    navController: NavController,
    currentRoute: String = ""
) {
    val context = LocalContext.current
    val userIdManager = remember { UserIdManager.getInstance(context) }
    val currentUserId = remember {
        runBlocking {
            try {
                userIdManager.getCurrentUserId()
            } catch (e: Exception) {
                ""
            }
        }
    }

    val viewModel: SuscripcionesViewModel = viewModel {
        SuscripcionesViewModel(currentUserId, FirestoreRepository())
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }

    // Sincronizar selectedTab con el ViewModel
    LaunchedEffect(selectedTab) {
        viewModel.switchTab(selectedTab)
    }

    // Función helper para convertir UserPreview a SuscripcionPreview temporal
    fun userToSuscripcionPreview(user: UserPreview, subscriptionId: String): SuscripcionPreview {
        return SuscripcionPreview(
            suscripcionId = subscriptionId,
            userId = user.userId,
            nickname = user.nickname,
            imageUrl = user.photoUrl,
            isVerified = user.isVerified,
            fechaUnionFormateada = "2025-01-15", // TODO: obtener fecha real cuando se implemente estructura escalable
            fechaExpiracionFormateada = "2025-02-15", // TODO: obtener fecha real cuando se implemente estructura escalable
            diasRestantes = 30, // TODO: calcular días reales cuando se implemente estructura escalable
            estaExpirada = false // TODO: calcular estado real cuando se implemente estructura escalable
        )
    }

    // Convertir UserPreview de Firestore a SuscripcionPreview para la UI
    val suscripcionesConverted = remember(uiState.suscripciones) {
        uiState.suscripciones.mapIndexed { index, user ->
            userToSuscripcionPreview(user, "suscripcion_$index")
        }
    }

    val suscriptoresConverted = remember(uiState.suscriptores) {
        uiState.suscriptores.mapIndexed { index, user ->
            userToSuscripcionPreview(user, "suscriptor_$index")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Suscripciones",
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
                actions = {
                    // Botón de configuración de suscripciones
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.ConfiguracionSuscripcion.route)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar suscripciones",
                            tint = Color(0xFF757575) // Gris medio
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
            // Tabs igual que FollowersFollowing
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BiihliveBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BiihliveOrangeLight,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Suscripciones",
                            color = if (selectedTab == 0) BiihliveOrangeLight else BiihliveBlue,
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Suscriptores",
                            color = if (selectedTab == 1) BiihliveOrangeLight else BiihliveBlue,
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                )
            }

            // Lista de suscripciones desde Firestore
            val currentList = if (selectedTab == 0) suscripcionesConverted else suscriptoresConverted

            // Mostrar error si existe
            uiState.error?.let { errorMessage ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.clearError()
                            viewModel.refresh()
                        }
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BiihliveBlue)
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
                                imageVector = Icons.Default.Subscriptions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == 0) "No tienes suscripciones aún" else "No tienes suscriptores aún",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = currentList,
                            key = { it.suscripcionId }
                        ) { suscripcion ->
                            SuscripcionItem(
                                suscripcion = suscripcion,
                                onClick = {
                                    // Navegar al perfil del usuario
                                    navController.navigate(Screen.PerfilConsultado.createRoute(suscripcion.userId))
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuscripcionItem(
    suscripcion: SuscripcionPreview,
    onClick: () -> Unit
) {
    val verde = Color(0xFF60BF19)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con borde e indicador de online
        Box {
            // Avatar base
            Box(
                modifier = Modifier
                    .size(53.dp)
                    .background(
                        color = BiihliveBlue.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .padding(2.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(suscripcion.imageUrl)
                        .crossfade(200)
                        .size(112, 112)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCacheKey(suscripcion.userId)
                        .build(),
                    contentDescription = "Avatar de ${suscripcion.nickname}",
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

            // Indicador de online (usando el campo isOnline del usuario - TODO: implementar cuando esté disponible)
            // Por ahora, mostrar como offline hasta que se implemente el sistema de presencia
            val isOnline = false // TODO: usar suscripcion.isOnline cuando esté disponible

            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(verde, CircleShape)
                        .offset(
                            x = (-8).dp, // 50% hacia adentro horizontalmente (más superpuesto)
                            y = (-8).dp  // 50% hacia adentro verticalmente (más superpuesto)
                        )
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(11.dp))

        // Información de la suscripción
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Nickname con badge de verificado
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suscripcion.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (suscripcion.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = BiihliveBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Fecha de unión
            Text(
                text = "Unido el ${suscripcion.fechaUnionFormateada}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 14.sp
                )
            )

            // Fecha de expiración
            Text(
                text = "Expira: ${suscripcion.fechaExpiracionFormateada}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (suscripcion.diasRestantes <= 7) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    lineHeight = 14.sp
                )
            )
        }

        // Flecha para ver más detalles
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Ver detalles",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
    }
}