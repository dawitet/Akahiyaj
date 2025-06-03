// PreferenceManager.kt
package com.dawitf.akahidegn

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson // For storing list of strings
import com.google.gson.reflect.TypeToken // For storing list of strings


object PreferenceManager {
    private const val PREFS_NAME = "akahidegn_prefs"
    private const val KEY_USER_DISPLAY_NAME = "user_display_name"
    private const val KEY_RECENT_SEARCHES = "recent_searches"
    private const val MAX_RECENT_SEARCHES = 5


    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // User Display Name
    fun saveUserDisplayName(context: Context, name: String) {
        getPreferences(context).edit().putString(KEY_USER_DISPLAY_NAME, name).apply()
    }

    fun getUserDisplayName(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_DISPLAY_NAME, null)
    }

    fun clearUserDisplayName(context: Context) {
        getPreferences(context).edit().remove(KEY_USER_DISPLAY_NAME).apply()
    }

    // Recent Searches
    fun getRecentSearches(context: Context): MutableList<String> {
        val json = getPreferences(context).getString(KEY_RECENT_SEARCHES, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun addRecentSearch(context: Context, query: String) {
        val searches = getRecentSearches(context)
        // Remove if already exists to move it to the top
        searches.remove(query)
        searches.add(0, query) // Add to the beginning
        // Keep only the last MAX_RECENT_SEARCHES
        while (searches.size > MAX_RECENT_SEARCHES) {
            searches.removeAt(searches.size - 1)
        }
        val json = Gson().toJson(searches)
        getPreferences(context).edit().putString(KEY_RECENT_SEARCHES, json).apply()
    }
}