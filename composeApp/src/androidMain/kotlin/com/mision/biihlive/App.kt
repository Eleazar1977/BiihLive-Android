package com.mision.biihlive

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mision.biihlive.navigation.AppNavigation
import com.mision.biihlive.language.*
import com.mision.biihlive.ui.theme.BiihliveTheme
import com.mision.biihlive.theme.*

@Composable
@Preview
fun App() {
    val context = LocalContext.current
    val languagePreferences = remember { LanguagePreferences(context) }
    val languageManager = remember { LanguageManager(languagePreferences) }
    val currentLanguage by languageManager.currentLanguage.collectAsState()

    // Theme management
    val themePreferences = remember { ThemePreferences(context) }
    val themeManager = remember { ThemeManager(themePreferences) }
    val themeMode by themeManager.themeMode.collectAsState()

    // Inicializar el idioma y tema guardados
    LaunchedEffect(Unit) {
        languageManager.initialize()
        themeManager.initialize()
    }

    // FORZAR TEMA CLARO PARA DEBUGGING
    val darkTheme = false // Siempre tema claro
    /* val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    } */

    CompositionLocalProvider(
        LocalLanguageManager provides languageManager,
        LocalCurrentLanguage provides currentLanguage,
        LocalThemeManager provides themeManager
    ) {
        BiihliveTheme(darkTheme = darkTheme) {
            // Configurar colores de la barra de navegación del sistema
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = !darkTheme

            SideEffect {
                // Configurar barra de estado
                systemUiController.setStatusBarColor(
                    color = Color.Transparent,
                    darkIcons = useDarkIcons
                )
                // Configurar barra de navegación
                systemUiController.setNavigationBarColor(
                    color = Color.Transparent,
                    darkIcons = useDarkIcons,
                    navigationBarContrastEnforced = false
                )
            }

            AppNavigation()
        }
    }
}