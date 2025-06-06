package com.dawitf.akahidegn.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.ui.components.*

/**
 * Enhanced MainScreen with comprehensive UI/UX improvements
 * Integrates all the new components for a modern user experience
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedMainScreen(
    groups: List<Group>,
    bookmarkedGroups: List<BookmarkedGroup>,
    recentActivities: List<RecentActivity>,
    isLoadingGroups: Boolean,
    recentSearches: List<String>,
    currentSearchText: String,
    searchFilters: SearchFilters,
    onSearchQueryChanged: (String) -> Unit,
    onPerformSearch: (String) -> Unit,
    onFiltersChanged: (SearchFilters) -> Unit,
    onRideClicked: (Group) -> Unit,
    onCreateRideClicked: () -> Unit,
    onBackClicked: () -> Unit,
    onNavigateToRideBuddies: () -> Unit = {},
    onNavigateToDebug: (() -> Unit)? = null,
    onToggleBookmark: (Group) -> Unit,
    onRefreshGroups: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    
    // States for UI interactions
    var isRefreshing by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showErrorSheet by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf<ErrorInfo?>(null) }
    
    // Filter and sort groups
    val filteredGroups = remember(groups, searchFilters) {
        filterGroups(groups, searchFilters)
    }
    
    // Handle pull-to-refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onRefreshGroups()
            isRefreshing = false
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Enhanced Top App Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ተመለስ",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Theme toggle button
                        IconButton(onClick = { /* Handle theme change */ }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Theme Settings"
                            )
                        }
                        
                        // Debug menu if available
                        onNavigateToDebug?.let { debugAction ->
                            IconButton(onClick = debugAction) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Debug Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // More options menu
                        EnhancedMenuButton(
                            showMenu = showMenu,
                            onMenuToggle = { showMenu = it },
                            onFeedbackClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:dawitfikadu3@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Akahiyaj App Feedback")
                                    putExtra(Intent.EXTRA_TEXT, "Hi, I have feedback about the Akahiyaj app...")
                                }
                                if (emailIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(emailIntent)
                                }
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
            
            // Tab Row for different sections
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("ቡድኖች") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("የተቀመጡ") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("ቅርብ እንቅስቃሴ") }
                )
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> {
                    // Groups Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Enhanced Search Bar
                        EnhancedSearchBar(
                            query = currentSearchText,
                            onQueryChange = onSearchQueryChanged,
                            onSearchSubmit = onPerformSearch,
                            isActive = isSearchFocused,
                            onActiveChange = { isSearchFocused = it }
                        )
                        
                        // Filter Chips
                        FilterChips(
                            selectedFilter = searchFilters.filterType,
                            onFilterChange = { filterType ->
                                onFiltersChanged(searchFilters.copy(filterType = filterType))
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Search Results Header
                        if (currentSearchText.isNotEmpty() || searchFilters.filterType != GroupFilterType.ALL) {
                            SearchResultsHeader(
                                resultsCount = filteredGroups.size,
                                query = currentSearchText,
                                sortOption = searchFilters.sortOption,
                                onSortClick = { showSortSheet = true }
                            )
                        }
                        
                        // Groups Content
                        Box(modifier = Modifier.fillMaxSize()) {
                            when {
                                isLoadingGroups -> {
                                    // Shimmer loading effect
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(16.dp)
                                    ) {
                                        items(3) {
                                            ShimmerGroupCard()
                                        }
                                    }
                                }
                                
                                filteredGroups.isEmpty() && currentSearchText.isNotEmpty() -> {
                                    // No search results
                                    NoSearchResults(
                                        query = currentSearchText,
                                        onClearSearch = { onSearchQueryChanged("") }
                                    )
                                }
                                
                                filteredGroups.isEmpty() -> {
                                    // Empty state
                                    EmptyStateComponent(
                                        title = "ምንም ቡድን አልተገኘም",
                                        subtitle = "አዲስ ቡድን ይፍጠሩ ወይም ቆይተው ይሞክሩ",
                                        actionText = "ቡድን ፍጠር",
                                        onActionClick = onCreateRideClicked
                                    )
                                }
                                
                                else -> {
                                    // Groups list with enhanced cards
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(16.dp)
                                    ) {
                                        itemsIndexed(
                                            items = filteredGroups,
                                            key = { _, group -> group.id }
                                        ) { index, group ->
                                            // Show banner ad every 3rd item
                                            if (index % 3 == 2 && index > 0) {
                                                CarouselBannerAd(
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                            
                                            EnhancedRideGroupCard(
                                                group = group,
                                                isBookmarked = bookmarkedGroups.any { it.group.id == group.id },
                                                onGroupClick = { onRideClicked(group) },
                                                onBookmarkToggle = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onToggleBookmark(group)
                                                    showSuccessMessage = true
                                                    successMessage = if (bookmarkedGroups.any { it.group.id == group.id }) {
                                                        "ከመዝገብ ተወግዷል"
                                                    } else {
                                                        "በመዝገብ ተቀመጠ"
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                1 -> {
                    // Bookmarks Tab
                    BookmarkedGroupsList(
                        bookmarkedGroups = bookmarkedGroups,
                        onGroupClick = onRideClicked,
                        onRemoveBookmark = { bookmarkedGroup ->
                            onToggleBookmark(bookmarkedGroup.group)
                            showSuccessMessage = true
                            successMessage = "ከመዝገብ ተወግዷል"
                        }
                    )
                }
                
                2 -> {
                    // Recent Activity Tab
                    RecentActivityList(
                        activities = recentActivities,
                        onActivityClick = { activity ->
                            // Handle activity click - maybe navigate to related group
                        }
                    )
                }
            }
        }
        
        // Floating Action Button with haptic feedback
        FloatingActionButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onCreateRideClicked()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "አዲስ ቡድን ፍጠር"
            )
        }
        
        // Success message overlay
        FloatingSuccessMessage(
            message = successMessage,
            isVisible = showSuccessMessage,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Auto-dismiss success message
        LaunchedEffect(showSuccessMessage) {
            if (showSuccessMessage) {
                kotlinx.coroutines.delay(2000)
                showSuccessMessage = false
            }
        }
    }
    
    // Bottom sheets and dialogs
    SortingBottomSheet(
        isVisible = showSortSheet,
        currentSort = searchFilters.sortOption,
        onSortChange = { sortOption ->
            onFiltersChanged(searchFilters.copy(sortOption = sortOption))
        },
        onDismiss = { showSortSheet = false }
    )
    
    // Error handling
    currentError?.let { error ->
        ErrorBottomSheet(
            errorInfo = error,
            isVisible = showErrorSheet,
            onDismiss = { showErrorSheet = false },
            onRetry = {
                onRefreshGroups()
                showErrorSheet = false
                currentError = null
            }
        )
    }
    
    // Snackbar host
    SnackbarHost(
        hostState = snackbarHostState
    )
}

@Composable
private fun EnhancedRideGroupCard(
    group: Group,
    isBookmarked: Boolean,
    onGroupClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onGroupClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "ወደ ${group.destination}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "መነሻ: ${group.departureTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                BookmarkButton(
                    isBookmarked = isBookmarked,
                    onToggle = onBookmarkToggle
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress indicator for group capacity
            UserCapacityIndicator(
                currentMembers = group.currentMembers,
                maxMembers = group.maxMembers
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${group.pricePerPerson} ብር",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                group.rating?.let { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB000),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedMenuButton(
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onFeedbackClick: () -> Unit
) {
    Box {
        IconButton(
            onClick = { onMenuToggle(true) }
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "ተጨማሪ አማራጮች",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onMenuToggle(false) }
        ) {
            DropdownMenuItem(
                text = { Text("ግብረመልስ ላክ") },
                onClick = {
                    onMenuToggle(false)
                    onFeedbackClick()
                },
                leadingIcon = {
                    Icon(Icons.Filled.Email, contentDescription = null)
                }
            )
        }
    }
}
