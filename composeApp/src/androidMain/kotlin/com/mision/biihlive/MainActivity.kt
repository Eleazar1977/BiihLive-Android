package com.mision.biihlive

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.mision.biihlive.utils.initializeAppContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Obtener el idioma guardado
        val prefs = newBase.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("selected_language", "es") ?: "es"
        
        // Aplicar el idioma
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitar edge-to-edge para manejar correctamente los WindowInsets
        enableEdgeToEdge()

        // Configurar que el contenido no se ajuste a las decoraciones del sistema
        // Esto es crítico para el manejo correcto del teclado y barras del sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Inicializar contexto para funciones utilitarias
        initializeAppContext(this)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}