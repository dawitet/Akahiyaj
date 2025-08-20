package com.dawitf.akahidegn.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector
import com.dawitf.akahidegn.R

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Main : Screen("main", "ዋና", Icons.Default.Home)
    object Groups : Screen("groups", "ቡድኖች", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "ቅንብሮች", Icons.Default.Settings)
    // Added profile & history (not in bottom bar yet)
    object Profile : Screen("profile/{userId}", "profile", Icons.Default.Person) { // title key used for localization lookup
        fun createRoute(userId: String) = "profile/$userId"
    }
    object ActivityHistory : Screen("activityHistory", "activity_history_title", Icons.Default.History)
}
