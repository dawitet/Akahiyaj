package com.dawitf.akahidegn.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.ui.animation.shared.SharedBounds
import com.dawitf.akahidegn.ui.animation.shared.SharedBoundsTransforms
import com.dawitf.akahidegn.ui.animation.shared.TransformType
import com.dawitf.akahidegn.ui.components.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group?,
    onBack: () -> Unit,
    onJoin: ((Group) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.destinationName ?: "Group Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (group == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                return@Column
            }

            val cardKey = remember(group.groupId) { "groupCard-${group.groupId ?: group.destinationName}" }
            SharedBounds(
                key = cardKey,
                transform = TransformType.ELEGANT_ARC
            ) { sharedMod ->
                Surface(
                    modifier = sharedMod.fillMaxWidth().glassCard(),
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = group.destinationName ?: "Unknown", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(text = "Members: ${group.memberCount}/${group.maxMembers}")
                        group.from?.let { Text(text = "From: $it") }
                        group.to?.let { Text(text = "To: $it") }
                        group.timestamp?.let { Text(text = "Created: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}") }
                    }
                }
            }

            if (onJoin != null) {
                Button(
                    onClick = { onJoin(group) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Join Group") }
            }
        }
    }
}
