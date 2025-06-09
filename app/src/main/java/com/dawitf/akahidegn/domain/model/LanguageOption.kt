package com.dawitf.akahidegn.domain.model

/**
 * Language options for the app's localization settings
 */
enum class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    ENGLISH("en", "English", "English"),
    AMHARIC("am", "Amharic", "አማርኛ"),
    OROMO("or", "Oromo", "Afaan Oromoo"),
    SYSTEM("system", "System Default", "System Default");

    companion object {
        fun fromCode(code: String): LanguageOption {
            return values().find { it.code == code } ?: ENGLISH
        }
        
        fun getDefaultLanguage(): LanguageOption = ENGLISH
        
        fun getAllLanguages(): List<LanguageOption> = values().toList()
    }
}
