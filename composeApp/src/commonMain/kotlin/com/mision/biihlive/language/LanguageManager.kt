package com.mision.biihlive.language

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LanguageManager(
    private val preferences: LanguagePreferences
) {
    private val _currentLanguage = MutableStateFlow(SupportedLanguage.getDefault())
    val currentLanguage: StateFlow<SupportedLanguage> = _currentLanguage.asStateFlow()
    
    suspend fun initialize() {
        val savedCode = preferences.getLanguageCode()
        val language = SupportedLanguage.fromCode(savedCode)
        _currentLanguage.value = language
    }
    
    suspend fun setLanguage(language: SupportedLanguage) {
        preferences.saveLanguageCode(language.code)
        _currentLanguage.value = language
        // Platform-specific locale update will be handled by actual implementations
    }
    
    fun getCurrentLanguage(): SupportedLanguage {
        return _currentLanguage.value
    }
}