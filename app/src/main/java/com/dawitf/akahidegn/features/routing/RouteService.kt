package com.dawitf.akahidegn.features.routing

import android.content.Context
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.retry.RetryMechanism
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService,
    private val retryMechanism: RetryMechanism,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    
    data class LatLng(
        val latitude: Double,
        val longitude: Double
    )
    
    data class RouteStep(
        val instruction: String,
        val distance: String,
        val duration: String,
        val startLocation: LatLng,
        val endLocation: LatLng
    )
    
    data class Route(
        val distance: String,
        val duration: String,
        val steps: List<RouteStep>,
        val polyline: String,
        val estimatedFare: Double? = null
    )
    
    data class RouteRequest(
        val origin: LatLng,
        val destination: LatLng,
        val waypoints: List<LatLng> = emptyList(),
        val mode: TravelMode = TravelMode.DRIVING,
        val optimizeWaypoints: Boolean = false
    )
    
    enum class TravelMode {
        DRIVING, WALKING, TRANSIT, BICYCLING
    }
    
    // Google Directions API response models
    data class DirectionsResponse(
        val routes: List<DirectionsRoute>,
        val status: String
    )
    
    data class DirectionsRoute(
        val legs: List<DirectionsLeg>,
        @SerializedName("overview_polyline")
        val overviewPolyline: Polyline
    )
    
    data class DirectionsLeg(
        val distance: TextValue,
        val duration: TextValue,
        val steps: List<DirectionsStep>
    )
    
    data class DirectionsStep(
        @SerializedName("html_instructions")
        val htmlInstructions: String,
        val distance: TextValue,
        val duration: TextValue,
        @SerializedName("start_location")
        val startLocation: Location,
        @SerializedName("end_location")
        val endLocation: Location
    )
    
    data class TextValue(
        val text: String,
        val value: Int
    )
    
    data class Location(
        val lat: Double,
        val lng: Double
    )
    
    data class Polyline(
        val points: String
    )
    
    suspend fun getRoute(request: RouteRequest): Result<Route> {
        return withContext(Dispatchers.IO) {
            try {
                retryMechanism.withRetry {
                    val url = buildDirectionsUrl(request)
                    val httpRequest = Request.Builder()
                        .url(url)
                        .build()
                    
                    val response = okHttpClient.newCall(httpRequest).execute()
                    
                    if (!response.isSuccessful) {
                        return@withRetry Result.failure(
                            AppError.NetworkError.UnknownNetworkError("HTTP ${response.code}")
                        )
                    }
                    
                    val responseBody = response.body?.string()
                    if (responseBody == null) {
                        return@withRetry Result.failure(
                            AppError.NetworkError.DataParsingError("Empty response body")
                        )
                    }
                    
                    val directionsResponse = gson.fromJson(responseBody, DirectionsResponse::class.java)
                    
                    if (directionsResponse.status != "OK") {
                        return@withRetry Result.failure(
                            AppError.ValidationError.NotFound("No route found: ${directionsResponse.status}")
                        )
                    }
                    
                    if (directionsResponse.routes.isEmpty()) {
                        return@withRetry Result.failure(
                            AppError.ValidationError.NotFound("No routes available")
                        )
                    }
                    
                    val route = parseRoute(directionsResponse.routes.first())
                    
                    analyticsService.trackCustomEvent("route_calculated", mapOf(
                        "origin_lat" to request.origin.latitude,
                        "origin_lng" to request.origin.longitude,
                        "destination_lat" to request.destination.latitude,
                        "destination_lng" to request.destination.longitude,
                        "travel_mode" to request.mode.name,
                        "distance" to route.distance,
                        "duration" to route.duration,
                        "waypoints_count" to request.waypoints.size
                    ))
                    
                    Result.success(route)
                }
            } catch (e: IOException) {
                analyticsService.logError(e, "route_calculation_failed")
                Result.failure(AppError.NetworkError.ConnectionTimeout)
            } catch (e: Exception) {
                analyticsService.logError(e, "route_parsing_failed")
                Result.failure(AppError.NetworkError.DataParsingError(e.message ?: "Failed to parse route"))
            }
        }
    }
    
    suspend fun getMultipleRoutes(requests: List<RouteRequest>): Result<List<Route>> {
        return withContext(Dispatchers.IO) {
            try {
                val routes = mutableListOf<Route>()
                
                for (request in requests) {
                    val result = getRoute(request)
                    if (result.isSuccess) {
                        routes.add(result.getOrThrow())
                    } else {
                        return@withContext Result.failure(result.exceptionOrNull()!!)
                    }
                }
                
                Result.success(routes)
            } catch (e: Exception) {
                analyticsService.logError(e, "multiple_routes_failed")
                Result.failure(AppError.UnknownError(e.message ?: "Failed to calculate multiple routes"))
            }
        }
    }
    
    suspend fun optimizeRoute(origin: LatLng, destination: LatLng, waypoints: List<LatLng>): Result<Route> {
        val request = RouteRequest(
            origin = origin,
            destination = destination,
            waypoints = waypoints,
            optimizeWaypoints = true
        )
        
        return getRoute(request)
    }
    
    suspend fun estimateFare(route: Route, passengers: Int = 1): Double {
        // Simple fare estimation based on distance
        // In a real app, you might integrate with pricing APIs
        val distanceValue = route.distance.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
        val baseFare = 50.0 // Base fare in Ethiopian Birr
        val perKmRate = 15.0 // Rate per kilometer
        val totalFare = baseFare + (distanceValue * perKmRate)
        
        // Split among passengers
        return totalFare / passengers
    }
    
    private fun buildDirectionsUrl(request: RouteRequest): String {
        val apiKey = "YOUR_GOOGLE_MAPS_API_KEY" // Should be stored securely
        val origin = "${request.origin.latitude},${request.origin.longitude}"
        val destination = "${request.destination.latitude},${request.destination.longitude}"
        val mode = request.mode.name.lowercase()
        
        var url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=$origin" +
                "&destination=$destination" +
                "&mode=$mode" +
                "&key=$apiKey"
        
        if (request.waypoints.isNotEmpty()) {
            val waypoints = request.waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            url += "&waypoints=$waypoints"
            
            if (request.optimizeWaypoints) {
                url += "&optimize=true"
            }
        }
        
        return url
    }
    
    private fun parseRoute(directionsRoute: DirectionsRoute): Route {
        val totalDistance = directionsRoute.legs.sumOf { it.distance.value }
        val totalDuration = directionsRoute.legs.sumOf { it.duration.value }
        
        val distanceText = formatDistance(totalDistance)
        val durationText = formatDuration(totalDuration)
        
        val steps = directionsRoute.legs.flatMap { leg ->
            leg.steps.map { step ->
                RouteStep(
                    instruction = step.htmlInstructions.replace(Regex("<[^>]*>"), ""),
                    distance = step.distance.text,
                    duration = step.duration.text,
                    startLocation = LatLng(step.startLocation.lat, step.startLocation.lng),
                    endLocation = LatLng(step.endLocation.lat, step.endLocation.lng)
                )
            }
        }
        
        return Route(
            distance = distanceText,
            duration = durationText,
            steps = steps,
            polyline = directionsRoute.overviewPolyline.points
        )
    }
    
    private fun formatDistance(meters: Int): String {
        return if (meters < 1000) {
            "$meters m"
        } else {
            val km = meters / 1000.0
            "%.1f km".format(Locale.getDefault(), km)
        }
    }
    
    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "$hours hr $minutes min"
            minutes > 0 -> "$minutes min"
            else -> "< 1 min"
        }
    }
}
