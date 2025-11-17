@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.mision.biihlive.presentation.chat.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mision.biihlive.domain.chat.model.Message
import com.mision.biihlive.domain.chat.model.MessageType
import com.mision.biihlive.presentation.chat.viewmodel.ChatViewModel
import com.mision.biihlive.data.chat.repository.ChatFirestoreRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de chat individual con manejo correcto de WindowInsets para Firebase
 */
@Composable
fun ChatScreen(
    chatId: String,
    displayName: String,
    navController: NavController,
    viewModel: ChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.allMessages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Usar colores del tema y corporativos
    val celeste = Color(0xFF1DC3FF)  // Mantener para elementos destacados
    val naranja = Color(0xFFDC5A01)  // Mantener para badges/destacados
    val verde = Color(0xFF60BF19)    // Color corporativo secundario

    // El ChatViewModel se inicializa automáticamente en su constructor
    // No necesita método initializeChat()

    // Auto-scroll cuando llegan nuevos mensajes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // SOLUCIÓN CRÍTICA: Scaffold con imePadding para manejar el teclado
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // CRÍTICO: Ajusta el padding cuando aparece el teclado
        topBar = {
            // TopBar con solo statusBarsPadding para que NO se mueva con el teclado
            TopAppBar(
                modifier = Modifier.statusBarsPadding(), // Solo padding del status bar
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar del otro usuario con indicador de presencia
                        Box {
                            val context = LocalContext.current
                            val imageUrl = uiState.otherUserId?.let { userId ->
                                ChatFirestoreRepository(context = context).generateThumbnailUrl(userId)
                            } ?: "https://ui-avatars.com/api/?name=${displayName}&background=1DC3FF&color=fff"

                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            // Puntito verde indicador de estado online
                            if (uiState.showOnlineStatus && uiState.otherUserIsOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .background(
                                            color = Color(0xFF60BF19), // Verde de Biihlive
                                            shape = CircleShape
                                        )
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            // Estado de presencia real
                            if (uiState.showOnlineStatus && uiState.otherUserIsOnline) {
                                Text(
                                    text = "en línea",
                                    fontSize = 12.sp,
                                    color = verde
                                )
                            }
                        }
                    }
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
                    IconButton(onClick = { /* TODO: Llamada de voz */ }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Llamar"
                        )
                    }
                    IconButton(onClick = { /* TODO: Videollamada */ }) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Videollamada"
                        )
                    }
                    IconButton(onClick = { /* TODO: Menú */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones"
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
            // CRÍTICO: navigationBarsPadding en el bottomBar evita el solapamiento
            ChatInputBar(
                currentMessage = messageText,
                onMessageChange = viewModel::updateMessageText,
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage()
                        // Ocultar teclado después de enviar
                        keyboardController?.hide()
                    }
                },
                isSending = uiState.isSendingMessage,
                modifier = Modifier.navigationBarsPadding() // Evita solapamiento con navigation bar
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoadingMessages -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = celeste)
                    }
                }

                uiState.chatError != null || uiState.messagesError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.chatError ?: uiState.messagesError ?: "Error desconocido"}",
                            color = Color.Red
                        )
                    }
                }

                else -> {
                    // CRÍTICO: LazyColumn con Arrangement.Bottom para comportamiento de chat
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.Bottom, // Mensajes se alinean desde abajo
                        reverseLayout = false // No invertir el layout
                    ) {
                        // Mensajes agrupados por fecha
                        val groupedMessages = messages.groupBy { message ->
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date(message.timestamp))
                        }

                        groupedMessages.forEach { (date, messagesForDate) ->
                            // Header de fecha
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = formatDateHeader(date),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Mensajes del día
                            items(
                                items = messagesForDate,
                                key = { it.id }
                            ) { message ->
                                MessageBubble(
                                    message = message,
                                    isCurrentUser = viewModel.isOwnMessage(message)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    val celeste = Color(0xFF1DC3FF)  // Color corporativo para mensajes propios

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isCurrentUser) celeste else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Contenido del mensaje
                Text(
                    text = message.content,
                    color = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Hora del mensaje
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(message.timestamp)),
                        fontSize = 11.sp,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else Color.Gray
                    )

                    // Indicador de estado para mensajes propios
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Enviado",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    currentMessage: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    val naranja = Color(0xFFDC5A01)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        // Barra de entrada de mensaje
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Campo de texto
            OutlinedTextField(
                value = currentMessage,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = {
                    Text(
                        text = "Escribe un mensaje...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = naranja,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                )
            )

            // Botón de enviar
            FilledIconButton(
                onClick = onSendMessage,
                enabled = currentMessage.isNotBlank() && !isSending,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = naranja,
                    disabledContainerColor = Color.Gray
                ),
                modifier = Modifier.size(48.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun formatDateHeader(date: String): String {
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val yesterday = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        .format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))

    return when (date) {
        today -> "Hoy"
        yesterday -> "Ayer"
        else -> date
    }
}