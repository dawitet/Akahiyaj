package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Advanced Filtering and Search Components
 * Enhanced search with filters, sorting, and real-time results
 */

data class FilterOption(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val count: Int? = null,
    val isSelected: Boolean = false
)

data class SortOption(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isAscending: Boolean = true
)

@Composable
fun AdvancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search groups...",
    showFilters: Boolean = true,
    onFiltersClick: () -> Unit = {},
    isLoading: Boolean = false,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 8.dp else 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                
                BasicTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        showSuggestions = it.isNotBlank() && suggestions.isNotEmpty()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            showSuggestions = focusState.isFocused && 
                                query.isNotBlank() && suggestions.isNotEmpty()
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearch()
                            showSuggestions = false
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
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            showSuggestions = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                if (showFilters) {
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    
                    IconButton(
                        onClick = onFiltersClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Filters",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // Search suggestions
        if (showSuggestions && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    suggestions.take(5).forEach { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                onSuggestionClick(suggestion)
                                showSuggestions = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.Filled.NorthWest,
            contentDescription = "Use suggestion",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun FilterChipRow(
    filters: List<FilterOption>,
    onFilterToggle: (FilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(filters) { filter ->
            EnhancedFilterChip(
                filter = filter,
                onToggle = { onFilterToggle(filter) }
            )
        }
    }
}

@Composable
fun EnhancedFilterChip(
    filter: FilterOption,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val chipColor by animateColorAsState(
        targetValue = if (filter.isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_color"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (filter.isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_content_color"
    )
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = chipColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (filter.isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filter.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Text(
                text = filter.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (filter.isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
            
            filter.count?.let { count ->
                Box(
                    modifier = Modifier
                        .background(
                            contentColor.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SortingSection(
    sortOptions: List<SortOption>,
    selectedSortId: String,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Sort by",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        sortOptions.forEach { sortOption ->
            SortOptionItem(
                sortOption = sortOption,
                isSelected = sortOption.id == selectedSortId,
                onSelect = { onSortChange(sortOption) }
            )
        }
    }
}

@Composable
private fun SortOptionItem(
    sortOption: SortOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sort_background"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSelect()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = sortOption.icon,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = sortOption.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        
        if (isSelected) {
            Icon(
                imageVector = if (sortOption.isAscending) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = if (sortOption.isAscending) "Ascending" else "Descending",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FilterBottomSheet(
    filters: List<FilterOption>,
    sortOptions: List<SortOption>,
    selectedSortId: String,
    onFilterToggle: (FilterOption) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filters & Sort",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClearFilters()
                }
            ) {
                Text("Clear All")
            }
        }
        
        // Filter section
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filter by",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            filters.chunked(2).forEach { rowFilters ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowFilters.forEach { filter ->
                        EnhancedFilterChip(
                            filter = filter,
                            onToggle = { onFilterToggle(filter) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Fill remaining space if odd number of filters
                    if (rowFilters.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        
        // Sort section
        SortingSection(
            sortOptions = sortOptions,
            selectedSortId = selectedSortId,
            onSortChange = onSortChange
        )
        
        // Apply button
        BouncyButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onApplyFilters()
            },
            text = "Apply Filters",
            icon = Icons.Filled.Check,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SearchResultsHeader(
    totalResults: Int,
    searchQuery: String,
    activeFiltersCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (searchQuery.isNotEmpty()) {
                        "$totalResults results for \"$searchQuery\""
                    } else {
                        "$totalResults groups available"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (activeFiltersCount > 0) {
                    Text(
                        text = "$activeFiltersCount active filter${if (activeFiltersCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (totalResults > 0) {
                PulsingDot(
                    color = MaterialTheme.colorScheme.primary,
                    size = 8.dp
                )
            }
        }
    }
}

@Composable
fun QuickFilters(
    recentSearches: List<String>,
    popularDestinations: List<String>,
    onQuickSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recentSearches.isNotEmpty()) {
            QuickFilterSection(
                title = "Recent Searches",
                items = recentSearches.take(3),
                icon = Icons.Filled.History,
                onClick = onQuickSearchClick
            )
        }
        
        if (popularDestinations.isNotEmpty()) {
            QuickFilterSection(
                title = "Popular Destinations",
                items = popularDestinations.take(4),
                icon = Icons.Filled.TrendingUp,
                onClick = onQuickSearchClick
            )
        }
    }
}

@Composable
private fun QuickFilterSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    onClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(items) { item ->
                QuickFilterChip(
                    text = item,
                    onClick = { onClick(item) }
                )
            }
        }
    }
}

@Composable
private fun QuickFilterChip(
    text: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}
