package com.mision.biihlive.language

import android.content.Context
import android.content.SharedPreferences

actual class LanguagePreferences(private val context: Context) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    }
    
    actual suspend fun getLanguageCode(): String {
        return sharedPreferences.getString(LANGUAGE_KEY, SupportedLanguage.getDefault().code) 
            ?: SupportedLanguage.getDefault().code
    }
    
    actual suspend fun saveLanguageCode(code: String) {
        sharedPreferences.edit().putString(LANGUAGE_KEY, code).apply()
    }
    
    companion object {
        private const val LANGUAGE_KEY = "selected_language"
    }
}