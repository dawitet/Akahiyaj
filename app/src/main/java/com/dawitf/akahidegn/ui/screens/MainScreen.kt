package com.dawitf.akahidegn.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.ui.components.RideGroupCard
import com.dawitf.akahidegn.ui.components.CarouselPlaceholderAd
import com.dawitf.akahidegn.ui.components.CarouselBannerAd
import com.dawitf.akahidegn.ui.components.ShimmerGroupCard
import com.dawitf.akahidegn.ui.components.EmptyStateComponent
// Enhanced UI Components
import com.dawitf.akahidegn.ui.components.EnhancedPullToRefresh
import com.dawitf.akahidegn.ui.components.AnimatedPressableCard
import com.dawitf.akahidegn.ui.components.ShimmerGroupList
import com.dawitf.akahidegn.ui.components.GradientBackground
import com.dawitf.akahidegn.ui.components.AdaptiveText
import com.dawitf.akahidegn.ui.components.FloatingActionButtonAnimated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    groups: List<Group>,
    isLoadingGroups: Boolean,
    recentSearches: List<String>,
    currentSearchText: String,
    onSearchQueryChanged: (String) -> Unit,
    onPerformSearch: (String) -> Unit,
    onRideClicked: (Group) -> Unit,
    onCreateRideClicked: () -> Unit,
    onBackClicked: () -> Unit,
    onNavigateToRideBuddies: () -> Unit = {},
    onNavigateToDebug: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState,
    onRefreshGroups: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    // Simple refresh state for compatibility
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Handle manual refresh
    suspend fun handleRefresh() {
        isRefreshing = true
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        onRefreshGroups()
        isRefreshing = false
    }
    
    // State for bottom sheet
    var bottomSheetHeight by remember { mutableFloatStateOf(0.4f) }
    
    // State for search focus and history dropdown
    var isSearchFocused by remember { mutableStateOf(false) }
    var showSearchHistory by remember { mutableStateOf(false) }
    
    // State for menu dropdown
    var showMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Gradient background for modern look
        GradientBackground()
        
        // Enhanced Pull-to-Refresh wrapper
        EnhancedPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { 
                coroutineScope.launch { 
                    handleRefresh() 
                }
            }
        ) {
            // Top App Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                AdaptiveText(
                    text = stringResource(id = R.string.find_a_ride_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // Ride Buddies Navigation Button
                IconButton(
                    onClick = onNavigateToRideBuddies,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Ride Buddies",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Debug Menu Button (only visible when debug is enabled)
                if (onNavigateToDebug != null) {
                    IconButton(
                        onClick = onNavigateToDebug,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Debug Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // More options menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Send Feedback") },
                            onClick = {
                                showMenu = false
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:dawitfikadu3@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Akahiyaj App Feedback")
                                    putExtra(Intent.EXTRA_TEXT, "Hi, I have feedback about the Akahiyaj app...")
                                }
                                if (emailIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(emailIntent)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Email, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
        
        // Map-like background with search bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp)
        ) {
            // Background with Material 3 surface color for better readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer) // Material 3 surface container
            )
            
            // Search bar at the top with history dropdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                        
                        BasicTextField(
                            value = currentSearchText,
                            onValueChange = onSearchQueryChanged,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .onFocusChanged { focusState ->
                                    isSearchFocused = focusState.isFocused
                                    showSearchHistory = focusState.isFocused && recentSearches.isNotEmpty()
                                },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                onPerformSearch(currentSearchText)
                                showSearchHistory = false
                            }),
                            decorationBox = { innerTextField ->
                                if (currentSearchText.isEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.search_bar_hint_where_to),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
                
                // Search history dropdown
                if (showSearchHistory) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp)
                            .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            items(recentSearches.take(8)) { searchTerm ->
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSearchQueryChanged(searchTerm)
                                                onPerformSearch(searchTerm)
                                                showSearchHistory = false
                                                focusManager.clearFocus()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = searchTerm,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Black
                                        )
                                    }
                                    if (searchTerm != recentSearches.take(8).last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = Color.Gray.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Map controls (zoom and navigation)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 200.dp), // Account for bottom sheet
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom in control
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    onClick = { /* Handle zoom in */ }
                ) {
                    IconButton(
                        onClick = { /* Handle zoom in */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Zoom in",
                            tint = Color.Black
                        )
                    }
                }
                
                // Navigation button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    onClick = { /* Handle navigation */ }
                ) {
                    IconButton(
                        onClick = { /* Handle navigation */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Navigate",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
        
        // Bottom Sheet
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(id = R.string.nearby_rides_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Nearby rides content
                if (isLoadingGroups) {
                    // Enhanced shimmer loading effect
                    ShimmerGroupList(itemCount = 3)
                } else if (groups.isEmpty()) {
                    // Enhanced empty state
                    EmptyStateComponent(
                        title = "No Rides Available",
                        subtitle = "Create a new ride or pull down to refresh and find nearby rides",
                        actionText = "Create Ride",
                        onActionClick = onCreateRideClicked
                    )
                } else {
                    // Enhanced carousel with banner ads every 3rd item
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(160.dp)
                    ) {
                        val itemsWithAds = mutableListOf<Any>()
                        groups.forEachIndexed { index, group ->
                            itemsWithAds.add(group)
                            // Add banner ad every 3rd item (after index 2, 5, 8, etc.)
                            if ((index + 1) % 3 == 0 && index < groups.size - 1) {
                                itemsWithAds.add("ad_${index}")
                            }
                        }
                        
                        items(
                            items = itemsWithAds,
                            key = { item -> 
                                when (item) {
                                    is Group -> item.groupId!!
                                    else -> item.toString()
                                }
                            }
                        ) { item ->
                            when (item) {
                                is Group -> {
                                    AnimatedPressableCard(
                                        onClick = { onRideClicked(item) },
                                        modifier = Modifier.width(140.dp)
                                    ) {
                                        RideGroupCard(
                                            group = item,
                                            onClick = { onRideClicked(item) }
                                        )
                                    }
                                }
                                else -> {
                                    CarouselPlaceholderAd()
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        }
        
        // Floating Action Button with enhanced animations
        FloatingActionButtonAnimated(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onCreateRideClicked()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = Icons.Filled.Add,
            text = stringResource(R.string.create_new_group_fab_description)
        )
        
        // Refresh indicator (simplified)
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        
        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}