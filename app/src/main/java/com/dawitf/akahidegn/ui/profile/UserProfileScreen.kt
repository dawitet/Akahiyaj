package com.dawitf.akahidegn.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.features.profile.RideStatistics
import com.dawitf.akahidegn.domain.model.UserProfile
import com.dawitf.akahidegn.ui.profile.components.ProfilePicture
import java.util.Calendar
import java.util.Locale
import java.text.NumberFormat
// Enhanced UI Components
import com.dawitf.akahidegn.ui.components.BilingualText
import com.dawitf.akahidegn.ui.components.AnimatedPressableCard
import com.dawitf.akahidegn.ui.components.GlassmorphismCard
import com.dawitf.akahidegn.ui.components.StatusBadge
import com.dawitf.akahidegn.ui.components.ShimmerProfileHeader
import com.dawitf.akahidegn.ui.components.QuickActionButton
import com.dawitf.akahidegn.ui.components.OptimizedProfileImage
import com.dawitf.akahidegn.viewmodel.UserProfileViewModel

@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = hiltViewModel(),
    onNavigateToEditProfile: () -> Unit,
    onNavigateToRideHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState(initial = null)
    val rideStats by viewModel.rideStats.collectAsState(initial = RideStatistics(
        totalRides = 0,
        totalDistance = 0.0,
        totalSpent = 0.0,
        averageRating = 0.0f,
        totalTimeSaved = 0L,
        carbonSaved = 0.0
    ))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Header
        ProfileHeader(
            userProfile = userProfile,
            onEditProfile = onNavigateToEditProfile
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Ride Statistics
        rideStats?.let { stats ->
            RideStatsSection(
                rideStats = stats,
                onViewHistory = onNavigateToRideHistory
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        QuickActionsSection(
            onViewSettings = onNavigateToSettings
        )
    }
}

@Composable
private fun ProfileHeader(
    userProfile: UserProfile?,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture
            ProfilePicture(
                photoUrl = userProfile?.profilePictureUrl,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Info
            Text(
                text = userProfile?.displayName ?: "User",
                style = MaterialTheme.typography.headlineSmall
            )

            if (userProfile?.bio != null) {
                Text(
                    text = userProfile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile Button
            Button(onClick = onEditProfile) {
                Text("Edit Profile")
            }
        }
    }
}

@Composable
private fun RideStatsSection(
    rideStats: RideStatistics,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Ride Statistics",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    title = "Total Rides",
                    value = rideStats.totalRides.toString()
                )
                StatCard(
                    title = "Total Distance",
                    value = "${rideStats.totalDistance} km"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    title = "Average Rating",
                    value = String.format("%.1f", rideStats.averageRating)
                )
                StatCard(
                    title = "Total Spent",
                    value = "$${rideStats.totalSpent}"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onViewHistory,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("View History")
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onViewSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        ActionButton(
            text = "Settings",
            onClick = onViewSettings
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text)
    }
}

// Helper functions
private fun formatMemberSince(joinDate: Long?): String {
    if (joinDate == null) return "Recently"
    
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = joinDate
    
    return when (calendar.get(Calendar.YEAR)) {
        Calendar.getInstance().get(Calendar.YEAR) -> "this year"
        else -> calendar.get(Calendar.YEAR).toString()
    }
}

private fun formatDistance(distance: Double): String {
    return when {
        distance >= 1000 -> String.format(Locale.getDefault(), "%.1f", distance / 1000)
        else -> distance.toInt().toString()
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return formatter.format(amount)
}
