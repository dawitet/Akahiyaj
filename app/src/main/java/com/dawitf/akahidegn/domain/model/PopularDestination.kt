package com.dawitf.akahidegn.domain.model

/**
 * Data class representing a popular destination with count statistics
 */
data class PopularDestination(
    val name: String,
    val count: Int,
    val distance: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)
