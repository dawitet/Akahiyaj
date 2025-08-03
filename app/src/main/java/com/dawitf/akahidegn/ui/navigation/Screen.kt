package com.dawitf.akahidegn.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Main : Screen("main", "ዋና", Icons.Default.Home)
    object ActiveGroups : Screen("active_groups", "ንቁ ቡድኖች", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "ቅንብሮች", Icons.Default.Settings)
}
