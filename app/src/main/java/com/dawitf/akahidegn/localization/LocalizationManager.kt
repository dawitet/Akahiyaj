package com.dawitf.akahidegn.localization

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dawitf.akahidegn.data.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalizationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val languageKey = stringPreferencesKey("selected_language")
    
    val currentLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[languageKey] ?: "en"
    }
    
    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[languageKey] = languageCode
        }
    }
    
    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language("en", "English", "English"),
            Language("am", "አማርኛ", "Amharic")
        )
    }
    
    fun getCurrentLocale(languageCode: String): Locale {
        return when (languageCode) {
            "am" -> Locale("am", "ET")
            else -> Locale.ENGLISH
        }
    }
    
    fun updateContextLocale(context: Context, languageCode: String): Context {
        val locale = getCurrentLocale(languageCode)
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return context.createConfigurationContext(configuration)
    }
}

data class Language(
    val code: String,
    val displayName: String,
    val nativeName: String
)

@Composable
fun rememberCurrentLanguage(): String {
    val context = LocalContext.current
    val localizationManager = LocalizationManager(context)
    val currentLanguage by localizationManager.currentLanguage.collectAsState(initial = "en")
    return currentLanguage
}
