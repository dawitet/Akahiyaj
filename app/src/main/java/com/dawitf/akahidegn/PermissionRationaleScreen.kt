package com.dawitf.akahidegn

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionRationaleScreen(
	onRequestPermission: () -> Unit,
	onExitApp: () -> Unit
) {
	Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Icon(
				imageVector = Icons.Filled.LocationOff,
				contentDescription = "Location Permission Required",
				modifier = Modifier.size(72.dp),
				tint = MaterialTheme.colorScheme.error
			)
			Spacer(modifier = Modifier.height(24.dp))
			Text(
				text = "Location Permission Essential",
				style = MaterialTheme.typography.headlineSmall,
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(16.dp))
			Text(
				text = "This app requires access to your location to function. Please grant the location permission to find and create groups based on your current whereabouts.",
				style = MaterialTheme.typography.bodyMedium,
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(32.dp))
			Button(onClick = onRequestPermission) {
				Text("Grant Permission")
			}
			Spacer(modifier = Modifier.height(16.dp))
			OutlinedButton(onClick = onExitApp) {
				Text("Exit App")
			}
		}
	}
}