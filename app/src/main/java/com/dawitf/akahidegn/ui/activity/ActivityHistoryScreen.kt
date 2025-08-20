package com.dawitf.akahidegn.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.viewmodel.ActivityHistoryViewModel
import com.dawitf.akahidegn.ui.animation.shared.SharedElement
import com.dawitf.akahidegn.ui.animation.shared.SharedElementKeys
import androidx.compose.animation.ExperimentalSharedTransitionApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ActivityHistoryScreen(
    onBack: () -> Unit,
    viewModel: ActivityHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    SharedElement(key = SharedElementKeys.HISTORY_SCREEN) { sharedModifier ->
        Scaffold(
            modifier = sharedModifier,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.activity_history_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_button))
                        }
                    },
                    actions = {
                        if (!state.isEmpty) {
                            TextButton(onClick = { viewModel.clear() }) { Text(stringResource(id = R.string.clear_button)) }
                        }
                    }
                )
            }
        ) { padding ->
        if (state.isEmpty) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(id = R.string.no_trip_history), fontWeight = FontWeight.Medium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.history) { trip ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(trip.destinationName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("${trip.memberCount} ${stringResource(id = R.string.group_members_count, trip.memberCount, trip.memberCount)} • ${trip.status}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
}
