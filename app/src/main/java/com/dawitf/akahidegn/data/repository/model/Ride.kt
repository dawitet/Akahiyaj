package com.dawitf.akahidegn.data.repository.model

import java.util.Date

/**
 * Basic ride model for the repository layer
 */
data class Ride(
    val id: String,
    val startLocation: String,
    val endLocation: String,
    val distance: Double,
    val duration: Long,
    val date: Date,
    val rating: Float?,
    val earnings: Double,
    val passengers: Int,
    val startTime: Date = date,
    val endTime: Date = Date(date.time + duration * 60 * 1000)
)
