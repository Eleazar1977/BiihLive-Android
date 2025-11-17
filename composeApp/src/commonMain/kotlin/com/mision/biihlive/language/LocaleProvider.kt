package com.mision.biihlive.language

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLanguageManager = staticCompositionLocalOf<LanguageManager> {
    error("No LanguageManager provided")
}

val LocalCurrentLanguage = compositionLocalOf<SupportedLanguage> {
    SupportedLanguage.getDefault()
}