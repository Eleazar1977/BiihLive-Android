package com.mision.biihlive.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val mode = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return ThemeMode.valueOf(mode)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

class ThemeManager(private val preferences: ThemePreferences) {
    private val _themeMode = MutableStateFlow(preferences.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferences.setThemeMode(mode)
    }

    fun initialize() {
        _themeMode.value = preferences.getThemeMode()
    }
}

val LocalThemeManager = compositionLocalOf<ThemeManager> {
    error("ThemeManager not provided")
}