package com.mision.biihlive.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mision.biihlive.components.LogoutConfirmationDialog
import com.mision.biihlive.components.BiihliveNavigationBar
import com.mision.biihlive.components.LanguageSelectorListItem
import com.mision.biihlive.language.LanguageManager
import com.mision.biihlive.language.LanguagePreferences
import com.mision.biihlive.language.LocalLanguageManager
import androidx.compose.runtime.CompositionLocalProvider
import com.mision.biihlive.theme.LocalThemeManager
import com.mision.biihlive.theme.ThemeMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Brightness6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    navController: NavController,
    onLogout: () -> Unit,
    currentRoute: String = ""
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = "Ajustes y privacidad",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            /* TODO: Implementar en futuras versiones
            item {
                SettingsItem(
                    icon = Icons.Outlined.Person,
                    title = "Cuenta",
                    onClick = { /* Navegar a cuenta */ }
                )
            }
            */

            item {
                LanguageSelectorListItem(
                    onLanguageChanged = { language ->
                        // El cambio de idioma ya se maneja en el LanguageSelector
                        // No necesitamos hacer nada adicional aquí
                    }
                )
            }

            item {
                ThemeSelectorItem()
            }

            /* TODO: Implementar en futuras versiones
            item {
                SettingsItem(
                    icon = Icons.Outlined.Lock,
                    title = "Privacidad",
                    onClick = { /* Navegar a privacidad */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Security,
                    title = "Seguridad y permisos",
                    onClick = { /* Navegar a seguridad */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.QueryStats,
                    title = "Estadísticas",
                    onClick = { /* Navegar a estadísticas */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.AccountBalance,
                    title = "Saldo",
                    onClick = { /* Navegar a saldo */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Share,
                    title = "Compartir perfil",
                    onClick = { /* Compartir perfil */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notificaciones",
                    onClick = { /* Configurar notificaciones */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.VideoLibrary,
                    title = "Preferencias de contenido",
                    onClick = { /* Preferencias de contenido */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Campaign,
                    title = "Anuncios",
                    onClick = { /* Configuración de anuncios */ }
                )
            }
            */

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    icon = Icons.AutoMirrored.Outlined.ExitToApp,
                    title = "Cerrar Sesión",
                    onClick = { showLogoutDialog = true },
                    tint = Color.Red
                )
            }
        }
    }

    // Diálogo de confirmación de logout
    LogoutConfirmationDialog(
        showDialog = showLogoutDialog,
        onDismiss = { showLogoutDialog = false },
        onConfirm = {
            showLogoutDialog = false
            onLogout()
        }
    )
}

@Composable
private fun ThemeSelectorItem() {
    val themeManager = LocalThemeManager.current
    val currentTheme by themeManager.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showThemeDialog = true }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = when (currentTheme) {
                    ThemeMode.LIGHT -> Icons.Default.LightMode
                    ThemeMode.DARK -> Icons.Default.DarkMode
                    ThemeMode.SYSTEM -> Icons.Default.Brightness6
                },
                contentDescription = "Tema",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = "Tema",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (currentTheme) {
                        ThemeMode.LIGHT -> "Claro"
                        ThemeMode.DARK -> "Oscuro"
                        ThemeMode.SYSTEM -> "Sistema"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                themeManager.setThemeMode(theme)
                showThemeDialog = false
            }
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Seleccionar tema",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                ThemeOption(
                    icon = Icons.Default.LightMode,
                    text = "Claro",
                    description = "Tema claro siempre activo",
                    isSelected = currentTheme == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    icon = Icons.Default.DarkMode,
                    text = "Oscuro",
                    description = "Tema oscuro siempre activo",
                    isSelected = currentTheme == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) }
                )
                ThemeOption(
                    icon = Icons.Default.Brightness6,
                    text = "Sistema",
                    description = "Seguir configuración del sistema",
                    isSelected = currentTheme == ThemeMode.SYSTEM,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    text: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}