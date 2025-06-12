package com.dawitf.akahidegn.ui.bookmarks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.features.bookmark.BookmarkManager
import com.dawitf.akahidegn.ui.components.RideGroupCard
import com.dawitf.akahidegn.ui.components.EmptyStateComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onGroupClick: (Group) -> Unit = {}
) {
    val context = LocalContext.current
    val bookmarkedGroups by BookmarkManager.getBookmarkedGroups(context).collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Groups") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (bookmarkedGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateComponent(
                    title = "No Saved Groups",
                    subtitle = "Your bookmarked ride groups will appear here.\nBookmark groups to save them for later!",
                    icon = Icons.Default.BookmarkBorder
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookmarkedGroups, key = { it.group.groupId ?: "" }) { bookmarkedGroup ->
                    RideGroupCard(
                        group = bookmarkedGroup.group,
                        onClick = { onGroupClick(bookmarkedGroup.group) }
                    )
                }
            }
        }
    }
}