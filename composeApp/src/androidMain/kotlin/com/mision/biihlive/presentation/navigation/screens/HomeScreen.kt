package com.mision.biihlive.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mision.biihlive.components.BiihliveNavigationBar
import com.mision.biihlive.components.ExitConfirmationDialog
import com.mision.biihlive.components.HomeTopBar
import com.mision.biihlive.components.BottomSheetUpdateDialog
import com.mision.biihlive.viewmodels.UpdateCheckViewModel
import com.mision.biihlive.utils.openPlayStore
import com.mision.biihlive.utils.getCurrentVersionCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToInitial: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToUsersSearch: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onNavigateToUserProfile: (String, Int) -> Unit = { _, _ -> },
    unreadMessagesCount: Int = 0
) {
    var selectedTab by remember { mutableStateOf(1) } // 1 = VIDEOS por defecto
    var currentRoute by remember { mutableStateOf("home") }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // ViewModel para verificación de actualizaciones
    val updateViewModel: UpdateCheckViewModel = viewModel()
    val updateState by updateViewModel.state.collectAsState()
    
    // Verificar actualizaciones al cargar la pantalla
    LaunchedEffect(Unit) {
        val currentVersion = getCurrentVersionCode()
        updateViewModel.checkForUpdates(currentVersion)
    }

    // Handle back button press
    BackHandler {
        showExitDialog = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Contenido según tab seleccionado
        when (selectedTab) {
            0 -> { // VIVOS
                LiveContent()
            }
            1 -> { // VIDEOS
                // TODO: Implementar contenido de videos cuando esté disponible
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Contenido de videos próximamente",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            2 -> { // FOTOS
                PhotoContent(
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToUserProfile = onNavigateToUserProfile
                )
            }
        }

        // TopBar fijo en la parte superior
        HomeTopBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onRankingClick = {
                onNavigateToRanking()
            },
            onSearchClick = {
                // Navegar a búsqueda de usuarios
                onNavigateToUsersSearch()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // NavigationBar fijo en la parte inferior
        BiihliveNavigationBar(
            currentRoute = currentRoute,
            onNavigate = { route ->
                currentRoute = route
                // Aquí puedes implementar navegación a otras pantallas
                when (route) {
                    "home" -> { /* Ya estamos en home */ }
                    "events" -> { /* Navegar a eventos */ }
                    "live" -> { /* Navegar a live */ }
                    "messages" -> {
                        // Navegar a lista de mensajes
                        onNavigateToMessages()
                    }
                    "profile" -> {
                        onNavigateToProfile()
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Exit confirmation dialog
    ExitConfirmationDialog(
        showDialog = showExitDialog,
        onDismiss = { showExitDialog = false },
        onConfirm = {
            showExitDialog = false
            // Exit the app
            (context as? android.app.Activity)?.finishAffinity()
        }
    )
    
    // Update available dialog - Bottom Sheet style
    if (updateState.showUpdateDialog && updateState.updateInfo != null) {
        BottomSheetUpdateDialog(
            onDismiss = {
                updateViewModel.dismissUpdateDialog()
            },
            onUpdate = {
                updateViewModel.dismissUpdateDialog()
                // Abrir Google Play Store
                openPlayStore("com.mision.biihlive")
            },
            versionName = updateState.updateInfo?.latestVersionName ?: "Nueva versión",
            appName = "Biihlive"
        )
    }
}

@Composable
fun LiveContent() {
    // FORZAR TEMA CLARO PARA DEBUGGING
    val isDarkTheme = false // isSystemInDarkTheme()
    val backgroundColor = Color(0xFFFFFCF9) // Siempre marfil
    val textColor = Color.Black // Siempre texto oscuro

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VIVOS",
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Contenido de transmisiones en vivo",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}