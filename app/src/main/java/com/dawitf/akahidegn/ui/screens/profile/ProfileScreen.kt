package com.dawitf.akahidegn.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.viewmodel.ProfileViewModel
import com.dawitf.akahidegn.ui.animation.shared.SharedElement
import com.dawitf.akahidegn.ui.animation.shared.SharedElementKeys
import androidx.compose.animation.ExperimentalSharedTransitionApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) { 
        viewModel.load(userId) 
    }

    SharedElement(key = SharedElementKeys.PROFILE_SCREEN) { sharedModifier ->
        Scaffold(
            modifier = sharedModifier,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.profile), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_button))
                        }
                    }
                )
            }
        ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Text(state.error ?: stringResource(id = R.string.error_generic), color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) { Text(stringResource(id = R.string.retry_button)) }
                    }
                }
                state.profile != null -> {
                    val profile = state.profile!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = profile.profilePictureUrl,
                                        contentDescription = stringResource(id = R.string.profile_picture),
                                        modifier = Modifier.size(96.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        stringResource(id = R.string.total_trips) + ": ${profile.totalTrips}  " +
                                                stringResource(id = R.string.rating) + ": ${profile.rating}"
                                    )
                                }
                            }
                        }
                        if (state.tripHistory.isNotEmpty()) {
                            item { Text(stringResource(id = R.string.recent_trips), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                            items(state.tripHistory) { trip ->
                                ListItem(
                                    headlineContent = { Text(trip.destinationName) },
                                    supportingContent = {
                                        val membersLabel = stringResource(id = R.string.group_members_count, trip.memberCount, trip.memberCount)
                                        Text("${trip.memberCount} $membersLabel • ${trip.status}")
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                        if (state.recentReviews.isNotEmpty()) {
                            item { Text(stringResource(id = R.string.reviews), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                            items(state.recentReviews) { review ->
                                ListItem(
                                    headlineContent = { Text("${review.reviewerName} - ${review.rating}") },
                                    supportingContent = { Text(review.comment) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
}
