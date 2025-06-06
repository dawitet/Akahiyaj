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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.features.profile.*
import com.dawitf.akahidegn.ui.theme.AkahidegnColors
import java.text.NumberFormat
import java.util.*
// Enhanced UI Components
import com.dawitf.akahidegn.ui.components.BilingualText
import com.dawitf.akahidegn.ui.components.AnimatedPressableCard
import com.dawitf.akahidegn.ui.components.GlassmorphismCard
import com.dawitf.akahidegn.ui.components.StatusBadge
import com.dawitf.akahidegn.ui.components.ShimmerProfileHeader
import com.dawitf.akahidegn.ui.components.QuickActionButton
import com.dawitf.akahidegn.ui.components.OptimizedProfileImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onEditProfile: () -> Unit = {},
    onSettings: () -> Unit = {},
    onRideHistory: () -> Unit = {},
    onAchievements: () -> Unit = {},
    onReferrals: () -> Unit = {},
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val rideStats by viewModel.rideStats.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val carbonFootprint by viewModel.carbonFootprint.collectAsState()
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        ProfileHeader(
            userProfile = userProfile,
            onEditProfile = onEditProfile
        )
        
        // Ride Statistics Cards
        RideStatsSection(
            rideStats = rideStats,
            onRideHistory = onRideHistory
        )
        
        // Quick Actions
        QuickActionsSection(
            onSettings = onSettings,
            onAchievements = onAchievements,
            onReferrals = onReferrals
        )
        
        // Achievements Preview
        AchievementsPreview(
            achievements = achievements.take(3),
            onViewAll = onAchievements
        )
        
        // Carbon Footprint
        CarbonFootprintCard(carbonFootprint = carbonFootprint)
        
        // Recent Activity (placeholder)
        RecentActivityCard()
    }
}

@Composable
private fun ProfileHeader(
    userProfile: UserProfile?,
    onEditProfile: () -> Unit
) {
    if (userProfile == null) {
        ShimmerProfileHeader()
        return
    }
    
    GlassmorphismCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo with optimized image loading
            OptimizedProfileImage(
                imageUrl = userProfile?.profilePhotoUrl,
                modifier = Modifier.size(100.dp),
                contentDescription = "Profile Photo"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // User Name
            Text(
                text = "${userProfile?.firstName ?: ""} ${userProfile?.lastName ?: ""}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Member Since
            Text(
                text = "Member since ${formatMemberSince(userProfile?.joinDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Rating
            userProfile?.let { profile ->
                if (profile.totalRating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = AkahidegnColors.Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${profile.rating} (${profile.totalRating} reviews)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Edit Profile Button
            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }
        }
    }
}

@Composable
private fun RideStatsSection(
    rideStats: RideStatistics?,
    onRideHistory: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Ride Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onRideHistory) {
                Text("View History")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Main Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Rides
            StatCard(
                title = "Total Rides",
                value = rideStats?.totalRides?.toString() ?: "0",
                icon = Icons.Default.LocationOn,
                color = AkahidegnColors.Primary,
                modifier = Modifier.weight(1f)
            )
            
            // Distance Traveled
            StatCard(
                title = "Distance",
                value = "${formatDistance(rideStats?.totalDistance ?: 0.0)} km",
                icon = Icons.Default.LocationOn,
                color = AkahidegnColors.Success,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Money Spent
            StatCard(
                title = "Total Spent",
                value = formatCurrency(rideStats?.totalSpent ?: 0.0),
                icon = Icons.Default.Settings,
                color = AkahidegnColors.Warning,
                modifier = Modifier.weight(1f)
            )
            
            // Time Saved
            StatCard(
                title = "Time Saved",
                value = "${rideStats?.totalTimeSaved ?: 0} min",
                icon = Icons.Default.Settings,
                color = AkahidegnColors.Info,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Completion Rate
        if (rideStats != null && rideStats.totalRides > 0) {
            val completionRate = (rideStats.completedRides.toFloat() / rideStats.totalRides * 100)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Completion Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${completionRate.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AkahidegnColors.Success
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = completionRate / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = AkahidegnColors.Success
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${rideStats.completedRides} completed out of ${rideStats.totalRides} total rides",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onSettings: () -> Unit,
    onAchievements: () -> Unit,
    onReferrals: () -> Unit
) {
    Column {
        BilingualText(
            englishText = "Quick Actions",
                        amharicText = "ፈጣን ድርጊቶች",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                AnimatedPressableCard(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionItem(
                        title = "Settings",
                        subtitle = "Manage your preferences and theme",
                        icon = Icons.Default.Settings,
                        onClick = onSettings
                    )
                }
                
                HorizontalDivider()
                
                AnimatedPressableCard(
                    onClick = onAchievements,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionItem(
                        title = "Achievements",
                        subtitle = "View your badges and progress",
                        icon = Icons.Default.Star,
                        onClick = onAchievements
                    )
                }
                
                HorizontalDivider()
                
                AnimatedPressableCard(
                    onClick = onReferrals,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionItem(
                        title = "Invite Friends",
                        subtitle = "Share your referral code",
                        icon = Icons.Default.Share,
                        onClick = onReferrals
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AchievementsPreview(
    achievements: List<Achievement>,
    onViewAll: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Achievements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onViewAll) {
                Text("View All")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (achievements.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "No achievements",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "No achievements yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Complete rides to unlock achievements!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                achievements.forEach { achievement ->
                    AchievementBadge(
                        achievement = achievement,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) 
                AkahidegnColors.Success.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = achievement.title,
                tint = if (achievement.isUnlocked) 
                    AkahidegnColors.Success 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CarbonFootprintCard(carbonFootprint: CarbonFootprintData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AkahidegnColors.Success.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Eco friendly",
                    tint = AkahidegnColors.Success,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Environmental Impact",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${carbonFootprint?.totalCarbonSaved?.toInt() ?: 0} kg",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AkahidegnColors.Success
                    )
                    Text(
                        text = "CO₂ Saved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${carbonFootprint?.equivalentTrees ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AkahidegnColors.Success
                    )
                    Text(
                        text = "Trees Equivalent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivityCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Placeholder for recent activity items
            Text(
                text = "Your recent ride activity will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
        distance >= 1000 -> String.format("%.1f", distance / 1000)
        else -> distance.toInt().toString()
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return formatter.format(amount)
}
