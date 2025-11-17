package com.mision.biihlive.presentation.chat.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mision.biihlive.domain.chat.model.ChatPreview
import com.mision.biihlive.presentation.chat.viewmodel.ChatFilter
import com.mision.biihlive.presentation.chat.viewmodel.MessagesListViewModel
import com.mision.biihlive.ui.theme.*
import com.mision.biihlive.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController: NavController,
    viewModel: MessagesListViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val filteredChats by viewModel.filteredChats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadChats()
    }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChange = viewModel::searchChats,
                    onCloseSearch = {
                        showSearchBar = false
                        viewModel.clearSearch()
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Mensajes",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        // Badge de mensajes no leídos
                        if (uiState.totalUnreadCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = if (uiState.totalUnreadCount > 99) "99+" else uiState.totalUnreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar chats",
                                tint = Gray600
                            )
                        }

                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filtrar chats",
                                    tint = Gray600
                                )
                            }

                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                ChatFilter.values().forEach { filter ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = getFilterText(filter),
                                                color = if (selectedFilter == filter) BiihliveOrangeLight else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.setFilter(filter)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = getFilterIcon(filter),
                                                contentDescription = null,
                                                tint = if (selectedFilter == filter) BiihliveOrangeLight else Gray500
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navegar a UsersSearchScreen en modo chat para seleccionar usuario y crear chat
                    navController.navigate(Screen.UsersSearch.createRoute(mode = "chat"))
                },
                containerColor = BiihliveOrangeLight,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Nuevo chat"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error ?: "Error desconocido",
                        onRetry = viewModel::loadChats
                    )
                }
                filteredChats.isEmpty() -> {
                    EmptyState(
                        hasSearch = searchQuery.isNotEmpty(),
                        filter = selectedFilter
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = filteredChats,
                            key = { it.chatId }
                        ) { chat ->
                            ChatListItem(
                                chat = chat,
                                onClick = {
                                    // Marcar como leído y navegar
                                    viewModel.markChatAsRead(chat.chatId)
                                    navController.navigate(
                                        Screen.Chat.createRoute(chat.chatId, chat.displayName)
                                    )
                                },
                                onLongClick = { /* TODO: Mostrar opciones del chat */ }
                            )
                        }
                    }
                }
            }

            // Indicador de actualización
            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = BiihliveOrangeLight
                )
            }
        }
    }

    // Manejo de errores con SnackBar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Mostrar SnackBar con error
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCloseSearch) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Cerrar búsqueda",
                tint = Gray600
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar chats...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiihliveOrangeLight,
                cursorColor = BiihliveOrangeLight
            )
        )

        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Limpiar búsqueda",
                    tint = Gray500
                )
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: ChatPreview,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar del chat
        Box {
            AsyncImage(
                model = chat.displayImage,
                contentDescription = "Avatar de ${chat.displayName}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Gray200),
                contentScale = ContentScale.Crop
            )


            // Indicador de estado en línea (puntito verde abajo izquierda)
            if (shouldShowOnlineStatus(chat)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(14.dp)
                        .background(Color.White, CircleShape)
                        .padding(2.dp)
                        .offset(
                            x = 8.dp, // 50% hacia adentro horizontalmente (más superpuesto)
                            y = (-8).dp  // 50% hacia adentro verticalmente (más superpuesto)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BiihliveGreen, CircleShape)
                    )
                }
            }

            // Indicador de chat fijado
            if (chat.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "Chat fijado",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .background(
                            Color.White,
                            CircleShape
                        )
                        .padding(2.dp),
                    tint = BiihliveOrangeLight
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Información del chat
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.displayName,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Columna para contador y fecha - alineados verticalmente
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Contador de mensajes no leídos arriba
                    if (chat.unreadCount > 0) {
                        Badge(
                            containerColor = BiihliveOrangeLight
                        ) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Fecha abajo
                    chat.lastMessageTime?.let { timestamp ->
                        Text(
                            text = formatMessageTime(timestamp),
                            fontSize = 12.sp,
                            color = if (chat.unreadCount > 0) BiihliveOrangeLight else Gray500,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de chat silenciado
                if (chat.isMuted) {
                    Icon(
                        Icons.Default.VolumeOff,
                        contentDescription = "Chat silenciado",
                        modifier = Modifier.size(14.dp),
                        tint = Gray500
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = chat.lastMessageText ?: "Sin mensajes",
                    fontSize = 14.sp,
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onSurface else Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = BiihliveOrangeLight
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando chats...",
                color = Gray500
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error cargando chats",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                fontSize = 14.sp,
                color = Gray500,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BiihliveOrangeLight
                )
            ) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun EmptyState(
    hasSearch: Boolean,
    filter: ChatFilter
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (hasSearch) Icons.Default.SearchOff else Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Gray500
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    hasSearch -> "No se encontraron chats"
                    filter != ChatFilter.ALL -> "No hay chats ${getFilterText(filter).lowercase()}"
                    else -> "No tienes conversaciones aún"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasSearch) "Intenta con otros términos de búsqueda" else "Inicia una nueva conversación",
                fontSize = 14.sp,
                color = Gray500,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun getFilterText(filter: ChatFilter): String {
    return when (filter) {
        ChatFilter.ALL -> "Todos"
        ChatFilter.UNREAD -> "No leídos"
        ChatFilter.PINNED -> "Fijados"
        ChatFilter.ARCHIVED -> "Archivados"
        ChatFilter.MUTED -> "Silenciados"
    }
}

private fun getFilterIcon(filter: ChatFilter) = when (filter) {
    ChatFilter.ALL -> Icons.Default.Chat
    ChatFilter.UNREAD -> Icons.Default.Circle
    ChatFilter.PINNED -> Icons.Default.PushPin
    ChatFilter.ARCHIVED -> Icons.Default.Archive
    ChatFilter.MUTED -> Icons.Default.VolumeOff
}

private fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val messageDate = Date(timestamp)

    return when {
        diff < 60_000 -> "Ahora" // Menos de 1 minuto
        diff < 86_400_000 -> {
            // Mismo día - mostrar hora
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
        }
        diff < 604_800_000 -> {
            // Menos de 1 semana - mostrar día de la semana
            SimpleDateFormat("E", Locale.getDefault()).format(messageDate)
        }
        else -> {
            // Más de 1 semana - mostrar fecha
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(messageDate)
        }
    }
}

// Función para determinar si mostrar el indicador de estado en línea
private fun shouldShowOnlineStatus(chat: ChatPreview): Boolean {
    // Mostrar indicador verde solo si:
    // 1. El usuario está conectado (isOnline = true)
    // 2. El usuario permite mostrar su estado (allowsStatusVisible = true)
    return chat.isOnline && chat.allowsStatusVisible
}