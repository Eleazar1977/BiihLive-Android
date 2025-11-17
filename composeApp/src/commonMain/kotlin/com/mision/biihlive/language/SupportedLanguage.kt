package com.mision.biihlive.language

enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    SPANISH("es", "Spanish", "Español"),
    ENGLISH("en", "English", "English");
    
    companion object {
        fun fromCode(code: String): SupportedLanguage {
            return entries.find { it.code == code } ?: SPANISH
        }
        
        fun getDefault(): SupportedLanguage = SPANISH
    }
}