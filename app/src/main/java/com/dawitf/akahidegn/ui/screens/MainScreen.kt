package com.dawitf.akahidegn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.RideGroupCard
import com.dawitf.akahidegn.ui.components.EmptyStateComponent
import com.dawitf.akahidegn.ui.components.SearchFilters
import com.dawitf.akahidegn.ui.components.EnhancedPullToRefresh
import com.dawitf.akahidegn.ui.components.ShimmerGroupList
import com.dawitf.akahidegn.ui.components.CarouselPlaceholderAd
import com.dawitf.akahidegn.ui.components.CarouselBannerAd
import com.dawitf.akahidegn.ui.components.glassmorphism
import com.dawitf.akahidegn.ui.components.glassCard
import com.dawitf.akahidegn.ui.components.gradientBackground
import com.dawitf.akahidegn.ui.components.SuccessAnimationCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    groups: List<Group>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onGroupClick: (Group) -> Unit,
    isLoading: Boolean,
    onRefreshGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var fabScale by remember { mutableStateOf(1f) }
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    
    // Animated gradient background
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_offset"
    )
    
    suspend fun handleRefresh() {
        isRefreshing = true
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        onRefreshGroups()
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
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .gradientBackground()
    ) {
        Scaffold(
            topBar = {
                // Enhanced TopAppBar with glassmorphism effect
                TopAppBar(
                    title = { 
                        Text(
                            "አካሂያጅ",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    },
                    actions = {
                        // Notification button with badge indicator
                        IconButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToNotifications()
                            }
                        ) {
                            Box {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                // Add notification badge if needed
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                ) {
                                    Text("3", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        
                        // Settings button with micro-interaction
                        IconButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToSettings()
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.glassmorphism(blurRadius = 8.dp, alpha = 0.1f)
                )
            },
            floatingActionButton = {
                // Enhanced FAB with animations and micro-interactions
                FloatingActionButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCreateGroup()
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .scale(fabAnimatedScale)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    fabScale = 0.95f
                                    tryAwaitRelease()
                                    fabScale = 1f
                                }
                            )
                        },
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Create Group",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "ቡድን ፍጠር",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            EnhancedPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { 
                    coroutineScope.launch { 
                        handleRefresh() 
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Amharic tagline above search bar
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(alpha = 0.15f),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "የታክሲ ሰልፍ ረጅም ከሆነ ከሌሎች ሰዎች ጋር በመሆን ራይድ/ፈረስ ጠርተው በአንድ ሰው ሂሳብ በመሄድ ዋጋውን ይካፈሉ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.3.sp,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Enhanced Search Bar with glassmorphism and animations
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(alpha = 0.2f),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { 
                                    Text(
                                        "ቡድን ፈልግ...",
                                        style = MaterialTheme.typography.bodyLarge
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search, 
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { 
                                                onSearchQueryChange("")
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Clear, 
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                
                    // Content with enhanced animations and effects
                    if (isLoading) {
                        ShimmerGroupList()
                    } else if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateComponent(
                                icon = Icons.Default.DirectionsCar,
                                title = "ምንም ቡድን የለም",
                                subtitle = "አዲስ ቡድን ፍጠሩ ወይም ወደ ታች ጎትተው አድስ አድርግ",
                                actionText = "ቡድን ፍጠር",
                                onActionClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCreateGroup()
                                }
                            )
                        }
                    } else {
                        // Groups section with enhanced styling
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "የሚገኙ ቡድኖች",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Add real-time status indicator
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = "${groups.size} ቡድኖች",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        
                        // Enhanced Vertical List with smooth animations
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.animateContentSize()
                        ) {
                            // Create list with groups and ads interspersed
                            val itemsList = mutableListOf<Any>()
                            var index = 0
                            
                            while (index < groups.size) {
                                // Add one group per row
                                if (index < groups.size) {
                                    itemsList.add(groups[index])
                                    index++
                                }
                                
                                // Add ad after every 3 groups (since we have more vertical space)
                                if (index % 3 == 0 && index < groups.size) {
                                    itemsList.add("AD")
                                }
                            }
                            
                            itemsList.forEachIndexed { listIndex, item ->
                                if (item == "AD") {
                                    // Full width banner ad spanning the single column
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(1) }) {
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = slideInVertically(
                                                initialOffsetY = { it },
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) + fadeIn()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .glassCard(alpha = 0.15f)
                                                    .clip(RoundedCornerShape(16.dp))
                                            ) {
                                                CarouselBannerAd()
                                            }
                                        }
                                    }
                                } else {
                                    // Group card
                                    item {
                                        val group = item as Group
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = slideInVertically(
                                                initialOffsetY = { it },
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) + fadeIn()
                                        ) {
                                            var cardScale by remember { mutableStateOf(1f) }
                                            val animatedScale by animateFloatAsState(
                                                targetValue = cardScale,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                                ),
                                                label = "card_scale"
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .scale(animatedScale)
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onPress = {
                                                                cardScale = 0.95f
                                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                tryAwaitRelease()
                                                                cardScale = 1f
                                                            },
                                                            onTap = {
                                                                onGroupClick(group)
                                                            }
                                                        )
                                                    }
                                            ) {
                                                RideGroupCard(
                                                    group = group,
                                                    onClick = { onGroupClick(group) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Bottom spacing for FAB with smooth transition
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(1) }) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn()
                                ) {
                                    Spacer(modifier = Modifier.height(100.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Success Animation Overlay
        SuccessAnimationCard(
            isVisible = showSuccessAnimation,
            title = "በተሳካ ሁኔታ ታደሰ!",
            subtitle = "ቡድኖች ዝርዝሩ ተዘምኗል",
            onDismiss = { showSuccessAnimation = false }
        )
    }
}