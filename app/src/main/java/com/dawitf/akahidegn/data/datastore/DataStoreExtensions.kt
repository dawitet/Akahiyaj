package com.dawitf.akahidegn.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Extension property for accessing DataStore from Context
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
