package com.mision.biihlive.language

expect class LanguagePreferences {
    suspend fun getLanguageCode(): String
    suspend fun saveLanguageCode(code: String)
}