package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.dawitf.akahidegn.location.LocationProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.ui.components.*

/**
 * Advanced Search and Filter Components
 * Provides intelligent search with filters and sorting options
 */

enum class GroupSortOption(val label: String) {
    NEAREST("ቅርብ"),
    DEPARTURE_TIME("የመነሻ ሰዓት"),
    AVAILABLE_SEATS("ተለዋዋጭ ስፍራ"),
    
    RATING("ደረጃ")
}

enum class GroupFilterType(val label: String, val icon: ImageVector) {
    ALL("ሁሉም", Icons.AutoMirrored.Filled.List),
    DEPARTING_SOON("በቅርቡ የሚነሳ", Icons.Default.AccessTime),
    AVAILABLE_SEATS("ተለዋዋጭ ስፍራ", Icons.Default.AirlineSeatReclineNormal),
    PREMIUM("ፕሬሚየም", Icons.Default.Star),
    WOMEN_ONLY("ለሴቶች ብቻ", Icons.Default.Person)
}

data class SearchFilters(
    val query: String = "",
    val sortOption: GroupSortOption = GroupSortOption.NEAREST,
    val filterType: GroupFilterType = GroupFilterType.ALL,
    
    val minimumSeats: Int = 1
)

/**
 * Filters a list of groups based on search filters
 */
fun filterGroups(groups: List<Group>, filters: SearchFilters): List<Group> {
    var filteredGroups = groups
    
    // Apply text query filter
    if (filters.query.isNotBlank()) {
        filteredGroups = filteredGroups.filter { group ->
            group.destinationName?.contains(filters.query, ignoreCase = true) == true ||
            group.creatorId?.contains(filters.query, ignoreCase = true) == true
        }
    }
    
    // Apply type filter
    filteredGroups = when (filters.filterType) {
        GroupFilterType.ALL -> filteredGroups
        GroupFilterType.DEPARTING_SOON -> {
            val currentTime = System.currentTimeMillis()
            filteredGroups.filter { group ->
                val departureTime = group.timestamp?.plus(30 * 60 * 1000) ?: 0L // Default 30 mins from creation
                departureTime > currentTime && departureTime < (currentTime + 15 * 60 * 1000) // Within next 15 mins
            }
        }
        GroupFilterType.AVAILABLE_SEATS -> filteredGroups.filter { group ->
            group.maxMembers > group.memberCount + 1 // At least 1 seat available
        }
        GroupFilterType.PREMIUM -> filteredGroups.filter { group ->
            group.isPremium == true
        }
        GroupFilterType.WOMEN_ONLY -> filteredGroups.filter { group ->
            group.isWomenOnly == true
        }
    }
    
    
    
    // Apply minimum seats filter
    filteredGroups = filteredGroups.filter { group ->
        val availableSeats = group.maxMembers - group.memberCount
        availableSeats >= filters.minimumSeats
    }
    
    // Sort based on selected option
    filteredGroups = when (filters.sortOption) {
        GroupSortOption.NEAREST -> {
            // Get the current location from the context - it will be provided externally by the caller
            // For filtering, null userLocation will keep original order
            val userLocation = LocationProvider.getUserLocation()
            
            if (userLocation != null) {
                filteredGroups.sortedBy { group ->
                    // Calculate distance if group has location data
                    if (group.pickupLat != null && group.pickupLng != null) {
                        com.dawitf.akahidegn.util.LocationUtils.calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            group.pickupLat!!, group.pickupLng!!
                        )
                    } else {
                        // Groups without location data go to the end
                        Double.MAX_VALUE
                    }
                }
            } else {
                // If user location not available, don't change order
                filteredGroups
            }
        }
        GroupSortOption.DEPARTURE_TIME -> {
            val currentTime = System.currentTimeMillis()
            filteredGroups.sortedBy { group ->
                group.timestamp?.plus(30 * 60 * 1000) ?: currentTime // Default 30 mins from creation
            }
        }
        GroupSortOption.AVAILABLE_SEATS -> filteredGroups.sortedByDescending { group ->
            group.maxMembers - group.memberCount
        }
        
        GroupSortOption.RATING -> filteredGroups.sortedByDescending { it.rating }
    }
    
    return filteredGroups
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "ቡድን ፈልግ...",
    isActive: Boolean = false,
    onActiveChange: (Boolean) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        onActiveChange(focusState.isFocused)
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSearchSubmit(query)
                        focusManager.clearFocus()
                    }
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
            
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueryChange("")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "ሰርዝ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChips(
    selectedFilter: GroupFilterType,
    onFilterChange: (GroupFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(GroupFilterType.values()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChange(filter)
                },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortingBottomSheet(
    isVisible: Boolean,
    currentSort: GroupSortOption,
    onSortChange: (GroupSortOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "ከዚህ መሰረት ደርድር",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
                
                GroupSortOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSortChange(option)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == option,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSortChange(option)
                                onDismiss()
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (currentSort == option) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SearchResultsHeader(
    resultsCount: Int,
    query: String,
    sortOption: GroupSortOption,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            if (query.isNotEmpty()) {
                Text(
                    text = "\"$query\" ውጤቶች",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$resultsCount ${if (resultsCount == 1) "ቡድን" else "ቡድኖች"} ተገኝተዋል",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSortClick()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = sortOption.label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun NoSearchResults(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Empty search illustration
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "የፈለጉት ቡድን አልተገኘም",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "\"$query\" ለሚለው ፍለጋ ውጤት አልተገኘም። \nሌላ ቃል ይሞክሩ ወይም ፍለጋዎን ያጽዱ።",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClearSearch()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("ፍለጋ አጽዳ")
        }
    }
}

/**
 * Smart search functionality for filtering groups
 */

