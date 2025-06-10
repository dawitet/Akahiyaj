package com.dawitf.akahidegn.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.features.profile.*
import com.dawitf.akahidegn.viewmodel.UserProfileViewModel
import java.text.NumberFormat
import java.util.*

@Composable
fun ProfileFeaturesScreen(
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val rideStats by viewModel.rideStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RideStatisticsCard(rideStats)
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    error?.let { errorMessage ->
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(errorMessage)
        }
    }
}

@Composable
fun RideStatisticsCard(stats: RideStatistics?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Ride Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            stats?.let {
                StatRow("Total Rides", it.totalRides.toString())
                StatRow("Total Distance", "${it.totalDistance} km")
                StatRow("Total Spent", formatCurrency(it.totalSpent))
                StatRow("Average Rating", String.format("%.1f", it.averageRating))
                StatRow("Time Saved", "${it.totalTimeSaved} min")
                StatRow("Carbon Saved", "${it.carbonSaved} kg CO₂")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
} 