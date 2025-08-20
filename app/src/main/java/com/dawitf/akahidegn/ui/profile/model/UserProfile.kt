package com.dawitf.akahidegn.ui.profile.model

/**
 * UI model for user profile display
 */
data class UserProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val profilePhotoUrl: String? = null,
    val bio: String? = null,
    val totalTrips: Int = 0,
    val rating: Float = 0f,
    val isVerified: Boolean = false,
    val joinDate: Long = System.currentTimeMillis()
)
