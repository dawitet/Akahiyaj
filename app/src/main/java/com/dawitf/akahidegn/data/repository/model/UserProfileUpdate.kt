package com.dawitf.akahidegn.data.repository.model

import java.util.Date

/**
 * Data class for updating user profile information
 */
data class UserProfileUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: Date? = null,
    val gender: String? = null,
    val emergencyContact: String? = null,
    val homeAddress: String? = null,
    val workAddress: String? = null,
    val preferredPaymentMethod: String? = null,
    val languages: List<String>? = null,
    val bio: String? = null,
    val interests: List<String>? = null
)
