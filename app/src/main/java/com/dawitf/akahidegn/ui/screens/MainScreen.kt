package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.dawitf.akahidegn.Group // Import your Group data class
import com.dawitf.akahidegn.R     // Import your R file
import com.dawitf.akahidegn.ui.components.RideGroupCard // Import from your components package

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMainScreen(
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
    snackbarHostState: SnackbarHostState
) {
    val focusManager = LocalFocusManager.current
    
    // State for bottom sheet
    var bottomSheetHeight by remember { mutableFloatStateOf(0.4f) }
    
    // State for search focus and history dropdown
    var isSearchFocused by remember { mutableStateOf(false) }
    var showSearchHistory by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(10f),
            color = Color.White,
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
                        tint = Color.Black
                    )
                }
                
                Text(
                    text = stringResource(id = R.string.find_a_ride_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
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
                        tint = Color.Black
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
                            tint = Color.Black
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
            // Background map image - Recent satellite view of Addis Ababa
            AsyncImage(
                model = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/12/1520/2065",
                contentDescription = "Addis Ababa satellite map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
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
                    color = Color.White
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
                            tint = Color.Gray,
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
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
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
                                        color = Color.Gray,
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
            color = Color.White,
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
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(id = R.string.nearby_rides_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Nearby rides content
                if (isLoadingGroups) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (groups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_groups_found_placeholder),
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(160.dp)
                    ) {
                        items(
                            items = groups,
                            key = { group -> group.groupId!! }
                        ) { group ->
                            RideGroupCard(
                                group = group,
                                onClick = { onRideClicked(group) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = onCreateRideClicked,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF3D98F4),
            contentColor = Color.White
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.create_new_group_fab_description)
            )
        }
        
        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}