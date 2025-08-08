package com.dawitf.akahidegn.activityhistory

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dawitf.akahidegn.data.datastore.dataStore
import com.dawitf.akahidegn.domain.model.TripHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityHistoryRepository @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    private val KEY = stringPreferencesKey("local_trip_history")

    val history: Flow<List<TripHistoryItem>> = context.dataStore.data.map { prefs ->
        prefs[KEY]?.let { json ->
            runCatching {
                val type = object : TypeToken<List<TripHistoryItem>>() {}.type
                gson.fromJson<List<TripHistoryItem>>(json, type)
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun add(item: TripHistoryItem) {
        val current = history.firstOrNull() ?: emptyList()
        val updated = (listOf(item) + current).take(100)
        context.dataStore.edit { prefs ->
            prefs[KEY] = gson.toJson(updated)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs.remove(KEY) }
    }
}
