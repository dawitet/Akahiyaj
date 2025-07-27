package com.dawitf.akahidegn.features.matching

import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.features.routing.RouteService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class SmartMatchingService @Inject constructor(
    private val groupRepository: GroupRepository,
    private val routeService: RouteService,
    private val analyticsService: AnalyticsService
) {
    
    data class UserPreferences(
        val maxWalkingDistanceKm: Double = 0.5,
        val maxDetourTimeMinutes: Int = 15,
        val preferredTravelModes: List<String> = listOf("car", "bus"),
        val smokingPreference: SmokingPreference = SmokingPreference.NO_PREFERENCE,
        val musicPreference: MusicPreference = MusicPreference.NO_PREFERENCE,
        val conversationLevel: ConversationLevel = ConversationLevel.MODERATE,
        val genderPreference: GenderPreference = GenderPreference.NO_PREFERENCE,
        val ageRangeMin: Int = 18,
        val ageRangeMax: Int = 65,
        val allowPets: Boolean = true,
        val punctualityImportance: PunctualityLevel = PunctualityLevel.MODERATE
    )
    
    enum class SmokingPreference { SMOKING_OK, NO_SMOKING, NO_PREFERENCE }
    enum class MusicPreference { LOUD_MUSIC_OK, QUIET_PREFERRED, NO_PREFERENCE }
    enum class ConversationLevel { CHATTY, MODERATE, QUIET }
    enum class GenderPreference { SAME_GENDER_ONLY, NO_PREFERENCE }
    enum class PunctualityLevel { VERY_IMPORTANT, MODERATE, FLEXIBLE }
    
    data class UserProfile(
        val userId: String,
        val name: String,
        val age: Int,
        val gender: String,
        val rating: Double,
        val completedRides: Int,
        val preferences: UserPreferences,
        val verificationLevel: VerificationLevel = VerificationLevel.BASIC
    )
    
    enum class VerificationLevel { BASIC, PHONE_VERIFIED, ID_VERIFIED, PREMIUM }
    
    data class MatchResult(
        val group: Group,
        val compatibilityScore: Double,
        val reasons: List<String>,
        
        val estimatedDetourTime: Int,
        val walkingDistanceKm: Double
    )
    
    data class MatchRequest(
        val userProfile: UserProfile,
        val originLat: Double,
        val originLng: Double,
        val destinationLat: Double,
        val destinationLng: Double,
        val departureTime: Long,
        val flexibilityMinutes: Int = 30,
        val maxResults: Int = 10
    )
    
    suspend fun findMatches(request: MatchRequest): Result<List<MatchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                analyticsService.trackCustomEvent("smart_matching_started", mapOf(
                    "user_id" to request.userProfile.userId,
                    "flexibility_minutes" to request.flexibilityMinutes,
                    "max_results" to request.maxResults
                ))
                
                // Get nearby groups
                val radiusKm = calculateSearchRadius(request.userProfile.preferences.maxWalkingDistanceKm)
                val nearbyGroupsResult = groupRepository.getNearbyGroups(
                    request.originLat, 
                    request.originLng, 
                    radiusKm
                ).first()
                
                if (nearbyGroupsResult.isFailure) {
                    val errorMsg = if (nearbyGroupsResult is com.dawitf.akahidegn.core.result.Result.Error) {
                        nearbyGroupsResult.error.message ?: "Failed to get nearby groups"
                    } else {
                        "Failed to get nearby groups"
                    }
                    return@withContext kotlin.Result.failure(Exception(errorMsg))
                }
                
                val nearbyGroups = nearbyGroupsResult.getOrNull() ?: emptyList()
                
                // Filter and score groups
                val matches = mutableListOf<MatchResult>()
                
                for (group in nearbyGroups) {
                    if (group.memberCount >= group.maxMembers) continue
                    
                    val matchResult = evaluateGroupCompatibility(request, group)
                    if (matchResult != null && matchResult.compatibilityScore > 0.3) {
                        matches.add(matchResult)
                    }
                }
                
                // Sort by compatibility score and return top results
                val topMatches = matches
                    .sortedByDescending { it.compatibilityScore }
                    .take(request.maxResults)
                
                analyticsService.trackCustomEvent("smart_matching_completed", mapOf(
                    "user_id" to request.userProfile.userId,
                    "total_groups_found" to nearbyGroups.size,
                    "compatible_matches" to topMatches.size,
                    "avg_compatibility_score" to topMatches.map { it.compatibilityScore }.average()
                ))
                
                Result.success(topMatches)
                
            } catch (e: Exception) {
                analyticsService.logError(e, "smart_matching_failed")
                Result.failure(AppError.UnknownError(e.message ?: "Matching failed"))
            }
        }
    }
    
    private suspend fun evaluateGroupCompatibility(
        request: MatchRequest, 
        group: Group
    ): MatchResult? {
        try {
            // Calculate geographical compatibility
            val walkingDistance = calculateDistance(
                request.originLat, request.originLng,
                group.pickupLat ?: 0.0, group.pickupLng ?: 0.0
            )
            
            if (walkingDistance > request.userProfile.preferences.maxWalkingDistanceKm) {
                return null
            }
            
            // Calculate time compatibility
            val groupDepartureTime = group.timestamp ?: System.currentTimeMillis()
            val timeDifference = abs(groupDepartureTime - request.departureTime) / 60000 // Convert to minutes
            
            if (timeDifference > request.flexibilityMinutes) {
                return null
            }
            
            // Calculate route compatibility
            val routeRequest = RouteService.RouteRequest(
                origin = RouteService.LatLng(request.originLat, request.originLng),
                destination = RouteService.LatLng(request.destinationLat, request.destinationLng),
                waypoints = listOf(RouteService.LatLng(group.pickupLat ?: 0.0, group.pickupLng ?: 0.0))
            )
            
            val routeResult = routeService.getRoute(routeRequest)
            if (routeResult.isFailure) {
                return null
            }
            
            val route = routeResult.getOrThrow()
            val detourTime = estimateDetourTime(route)
            
            if (detourTime > request.userProfile.preferences.maxDetourTimeMinutes) {
                return null
            }
            
            // Calculate compatibility score
            val compatibilityScore = calculateCompatibilityScore(request, group, walkingDistance, timeDifference, detourTime)
            
            // Generate compatibility reasons
            val reasons = generateCompatibilityReasons(request, group, walkingDistance, timeDifference, detourTime)
            
            
            
            return MatchResult(
                group = group,
                compatibilityScore = compatibilityScore,
                reasons = reasons,
                
                estimatedDetourTime = detourTime,
                walkingDistanceKm = walkingDistance
            )
            
        } catch (e: Exception) {
            analyticsService.logError(e, "group_compatibility_evaluation_failed")
            return null
        }
    }
    
    private fun calculateCompatibilityScore(
        request: MatchRequest,
        group: Group,
        walkingDistance: Double,
        timeDifference: Long,
        detourTime: Int
    ): Double {
        var score = 1.0
        
        // Distance penalty (closer is better)
        val distancePenalty = (walkingDistance / request.userProfile.preferences.maxWalkingDistanceKm) * 0.3
        score -= distancePenalty
        
        // Time penalty (closer departure time is better)
        val timePenalty = (timeDifference / request.flexibilityMinutes.toDouble()) * 0.2
        score -= timePenalty
        
        // Detour penalty (less detour is better)
        val detourPenalty = (detourTime / request.userProfile.preferences.maxDetourTimeMinutes.toDouble()) * 0.2
        score -= detourPenalty
        
        // Group size bonus (more people = better cost sharing, but not overcrowded)
        val optimalSize = group.maxMembers * 0.75
        val sizeFactor = 1.0 - abs(group.memberCount - optimalSize) / optimalSize
        score += sizeFactor * 0.1
        
        // Destination similarity bonus
        val destinationSimilarity = calculateDestinationSimilarity(
            request.destinationLat, request.destinationLng,
            group.pickupLat ?: 0.0, group.pickupLng ?: 0.0 // Assuming group location is pickup point
        )
        score += destinationSimilarity * 0.2
        
        return maxOf(0.0, minOf(1.0, score))
    }
    
    private fun generateCompatibilityReasons(
        request: MatchRequest,
        group: Group,
        walkingDistance: Double,
        timeDifference: Long,
        detourTime: Int
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        if (walkingDistance < 0.2) {
            reasons.add("Very close pickup point (${String.format(Locale.getDefault(), "%.1f", walkingDistance * 1000)}m walk)")
        } else {
            reasons.add("Pickup point ${String.format(Locale.getDefault(), "%.1f", walkingDistance * 1000)}m away")
        }
        
        if (timeDifference < 5) {
            reasons.add("Perfect time match")
        } else {
            reasons.add("${timeDifference}min time difference")
        }
        
        if (detourTime < 5) {
            reasons.add("Minimal route detour")
        } else {
            reasons.add("${detourTime}min estimated detour")
        }
        
        if (group.memberCount < group.maxMembers - 1) {
            reasons.add("Group has space for ${group.maxMembers - group.memberCount} more riders")
        }
        
        if (group.destinationName?.contains(request.userProfile.preferences.preferredTravelModes.firstOrNull() ?: "", true) == true) {
            reasons.add("Matches your travel preferences")
        }
        
        return reasons
    }
    
    
    
    private fun calculateSearchRadius(maxWalkingDistance: Double): Double {
        // Expand search radius based on walking tolerance
        return maxWalkingDistance * 2.0 // Search in a wider area
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
    
    private fun calculateDestinationSimilarity(
        destLat1: Double, destLng1: Double,
        destLat2: Double, destLng2: Double
    ): Double {
        val distance = calculateDistance(destLat1, destLng1, destLat2, destLng2)
        // Closer destinations get higher similarity scores
        return maxOf(0.0, 1.0 - (distance / 5.0)) // 5km max distance for similarity
    }
    
    private fun parseTime(date: String, time: String): Long {
        // Simple time parsing - in production, use proper date/time libraries
        // This is a placeholder implementation
        return System.currentTimeMillis()
    }
    
    private fun estimateDetourTime(route: RouteService.Route): Int {
        // Simple detour estimation based on route duration
        val totalDuration = route.duration.replace(Regex("[^\\d]"), "").toIntOrNull() ?: 0
        return (totalDuration * 0.2).toInt() // Assume 20% detour on average
    }
}
