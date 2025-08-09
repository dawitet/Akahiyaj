package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.dawitf.akahidegn.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.dawitf.akahidegn.viewmodel.SettingsViewModel
import com.dawitf.akahidegn.ui.components.gradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onSignOut: () -> Unit) { // Added onSignOut callback
    val viewModel: SettingsViewModel = hiltViewModel()
    val notificationsEnabled = viewModel.notificationsEnabled.collectAsState()
    val soundEnabled = viewModel.soundEnabled.collectAsState()
    val vibrationEnabled = viewModel.vibrationEnabled.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .gradientBackground()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween, // Changed to space out content
            horizontalAlignment = Alignment.CenterHorizontally // Center content horizontally
        ) {
            Text(
                text = stringResource(id = R.string.settings_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Toggles
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = stringResource(id = R.string.notification_permission_needed),
                    checked = notificationsEnabled.value,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                Spacer(Modifier.height(12.dp))
                SettingToggleRow(
                    title = stringResource(id = R.string.settings_sound_effects),
                    checked = soundEnabled.value,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )
                Spacer(Modifier.height(12.dp))
                SettingToggleRow(
                    title = stringResource(id = R.string.settings_vibration),
                    checked = vibrationEnabled.value,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f)) // Pushes sign out button to the bottom

            Button(
                onClick = onSignOut, // Call the provided lambda
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color.White)
                Text(
                    text = stringResource(id = R.string.settings_sign_out),
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}