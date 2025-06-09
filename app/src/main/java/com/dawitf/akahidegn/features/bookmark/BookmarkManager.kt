package com.dawitf.akahidegn.features.bookmark

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.data.datastore.dataStore
import com.dawitf.akahidegn.ui.components.ActivityType
import com.dawitf.akahidegn.ui.components.BookmarkedGroup
import com.dawitf.akahidegn.ui.components.RecentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type
import kotlinx.coroutines.flow.first

/**
 * Manager class for bookmarks and recent activity functionality
 */
class BookmarkManager {
    companion object {
        private val BOOKMARKS_KEY = stringPreferencesKey("bookmarked_groups")
        private val RECENT_ACTIVITY_KEY = stringPreferencesKey("recent_activity")
        private val gson = Gson()
        
        // Get all bookmarked groups
        fun getBookmarkedGroups(context: Context): Flow<List<BookmarkedGroup>> {
            return context.dataStore.data.map { preferences ->
                val bookmarksJson = preferences[BOOKMARKS_KEY] ?: "[]"
                val type: Type = object : TypeToken<List<BookmarkedGroup>>() {}.type
                gson.fromJson(bookmarksJson, type) ?: emptyList()
            }
        }
        
        // Add a group to bookmarks
        suspend fun addBookmark(context: Context, group: Group, notes: String? = null) {
            context.dataStore.edit { preferences ->
                val bookmarksJson = preferences[BOOKMARKS_KEY] ?: "[]"
                val type: Type = object : TypeToken<List<BookmarkedGroup>>() {}.type
                val currentBookmarks: MutableList<BookmarkedGroup> = 
                    gson.fromJson(bookmarksJson, type) ?: mutableListOf()
                
                // Check if already bookmarked to avoid duplicates
                val existingIndex = currentBookmarks.indexOfFirst { it.group.groupId == group.groupId }
                if (existingIndex >= 0) {
                    currentBookmarks[existingIndex] = BookmarkedGroup(
                        group = group,
                        bookmarkedAt = System.currentTimeMillis(),
                        notes = notes ?: currentBookmarks[existingIndex].notes
                    )
                } else {
                    currentBookmarks.add(
                        BookmarkedGroup(
                            group = group,
                            bookmarkedAt = System.currentTimeMillis(),
                            notes = notes
                        )
                    )
                }
                
                // Store updated list
                preferences[BOOKMARKS_KEY] = gson.toJson(currentBookmarks)
                
                // Add to recent activity
                addActivity(
                    context,
                    RecentActivity(
                        id = System.currentTimeMillis().toString(),
                        type = ActivityType.BOOKMARKED,
                        groupId = group.groupId ?: "",
                        groupName = group.destinationName ?: "Unknown",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        
        // Remove a group from bookmarks
        suspend fun removeBookmark(context: Context, groupId: String) {
            context.dataStore.edit { preferences ->
                val bookmarksJson = preferences[BOOKMARKS_KEY] ?: "[]"
                val type: Type = object : TypeToken<List<BookmarkedGroup>>() {}.type
                val currentBookmarks: MutableList<BookmarkedGroup> = 
                    gson.fromJson(bookmarksJson, type) ?: mutableListOf()
                
                val updatedBookmarks = currentBookmarks.filter { it.group.groupId != groupId }
                preferences[BOOKMARKS_KEY] = gson.toJson(updatedBookmarks)
            }
        }
        
        // Check if a group is bookmarked
        suspend fun isBookmarked(context: Context, groupId: String): Boolean {
            val bookmarks = getBookmarkedGroups(context).first()
            return bookmarks.any { it.group.groupId == groupId }
        }
        
        // Get recent activities
        fun getRecentActivity(context: Context): Flow<List<RecentActivity>> {
            return context.dataStore.data.map { preferences ->
                val activityJson = preferences[RECENT_ACTIVITY_KEY] ?: "[]"
                val type: Type = object : TypeToken<List<RecentActivity>>() {}.type
                gson.fromJson(activityJson, type) ?: emptyList()
            }
        }
        
        // Add an activity to the recent activity list
        suspend fun addActivity(context: Context, activity: RecentActivity) {
            context.dataStore.edit { preferences ->
                val activityJson = preferences[RECENT_ACTIVITY_KEY] ?: "[]"
                val type: Type = object : TypeToken<List<RecentActivity>>() {}.type
                val currentActivities: MutableList<RecentActivity> = 
                    gson.fromJson(activityJson, type) ?: mutableListOf()
                
                // Add the new activity and limit to 50 most recent
                currentActivities.add(0, activity)
                val limitedActivities = currentActivities.take(50)
                
                preferences[RECENT_ACTIVITY_KEY] = gson.toJson(limitedActivities)
            }
        }
        
        // Clear all recent activity
        suspend fun clearRecentActivity(context: Context) {
            context.dataStore.edit { preferences ->
                preferences[RECENT_ACTIVITY_KEY] = "[]"
            }
        }
    }
}
