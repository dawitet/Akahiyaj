package com.dawitf.akahidegn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.*
import com.dawitf.akahidegn.ui.viewmodel.GroupsMapViewModel
import kotlinx.coroutines.launch
import android.location.Location

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    groups: List<Group>,
    userLocation: Location?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    
    
    onGroupClick: (Group) -> Unit,
    isLoading: Boolean,
    onRefreshGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    
    
    onNavigateToNotifications: () -> Unit,
    
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var isRefreshing by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var fabScale by remember { mutableStateOf(1f) }
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    
    // Initialize ViewModel for Firestore groups
    val groupsMapViewModel: GroupsMapViewModel = viewModel()
    val firestoreGroups by groupsMapViewModel.groups.collectAsState()
    
    // Use Firestore groups if available, otherwise fallback to passed groups
    val displayGroups = if (firestoreGroups.isNotEmpty()) firestoreGroups else groups
    
    // Apply search to displayed groups
    val filteredGroups = remember(displayGroups, searchQuery) {
        if (searchQuery.isNotBlank()) {
            displayGroups.filter { group ->
                group.destinationName?.contains(searchQuery, ignoreCase = true) == true ||
                group.originalDestination?.contains(searchQuery, ignoreCase = true) == true ||
                group.creatorId?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            displayGroups
        }
    }
    
    // Check for recent groups (created in last 30 minutes)
    val recentGroups = remember(filteredGroups) {
        val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000)
        filteredGroups.filter { group ->
            (group.timestamp ?: 0) > thirtyMinutesAgo
        }
    }
    
    // Load groups on launch and when user location changes
    LaunchedEffect(userLocation) {
        // Use location-based filtering if user location is available
        userLocation?.let {
            groupsMapViewModel.loadNearbyGroups(it.latitude, it.longitude, 500.0) // 500m radius
        } ?: groupsMapViewModel.loadAllGroups()
    }
    
    // Animated gradient background - used in the gradient effect
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_offset"
    )
    
    fun handleRefresh() {
        isRefreshing = true
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        
        // Refresh both the passed groups and ViewModel groups
        onRefreshGroups()
        
        // Also refresh the GroupsMapViewModel data
        groupsMapViewModel.refreshGroups()
        
        isRefreshing = false
        showSuccessAnimation = true
    }
    
    // Animated FAB scale for micro-interactions
    val fabAnimatedScale by animateFloatAsState(
        targetValue = fabScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale"
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // Enhanced header with better animations
        AnimatedHeader(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    onRefreshGroups()
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            },
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToNotifications = onNavigateToNotifications,
            isRefreshing = isRefreshing,
            notificationCount = 0 // This should come from notification service
        )

        // Map view showing groups as pins
        GroupsMapView(
            groups = filteredGroups,
            userLocation = userLocation,
            onGroupClick = onGroupClick,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Groups list below the map
        if (filteredGroups.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredGroups) { group ->
                    GroupCard(
                        group = group,
                        userLocation = userLocation,
                        onClick = { onGroupClick(group) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        } else {
            // Empty state
            EmptyGroupsState(
                isLoading = isLoading,
                modifier = Modifier.weight(1f)
            )
        }

        // Create Group FAB
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        fabScale = 0.8f
                        kotlinx.coroutines.delay(100)
                        fabScale = 1f
                        onCreateGroup()
                    }
                },
                modifier = Modifier.scale(fabScale),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Group",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    
}