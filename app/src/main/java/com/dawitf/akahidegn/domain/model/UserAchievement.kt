package com.dawitf.akahidegn.domain.model

import com.dawitf.akahidegn.features.profile.AchievementCategory

/**
 * Data class representing a user achievement
 */
data class UserAchievement(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val unlockedDate: Long? = null,
    val progress: Float = 0f,
    val maxProgress: Float = 1f,
    val category: AchievementCategory,
    val points: Int = 0,
    val isUnlocked: Boolean = false
)
