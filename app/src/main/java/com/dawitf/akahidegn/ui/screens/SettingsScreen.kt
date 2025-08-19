package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.dawitf.akahidegn.R
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.dawitf.akahidegn.viewmodel.SettingsViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import com.dawitf.akahidegn.ui.animation.shared.SharedElement
import com.dawitf.akahidegn.ui.animation.shared.SharedElementKeys
import com.dawitf.akahidegn.ui.animation.shared.SharedAnimatedVisibility
import com.dawitf.akahidegn.ui.animation.shared.AnimationType
import com.dawitf.akahidegn.ui.animation.shared.TransformType
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onSignOut: () -> Unit, onOpenProfile: () -> Unit = {}) { // Added onOpenProfile FAB
    val viewModel: SettingsViewModel = hiltViewModel()
    val notificationsEnabled = viewModel.notificationsEnabled.collectAsState()
    val soundEnabled = viewModel.soundEnabled.collectAsState()
    val vibrationEnabled = viewModel.vibrationEnabled.collectAsState()
    val submitting = viewModel.suggestionSubmitting.collectAsState()
    val submitted = viewModel.suggestionSubmitted.collectAsState()
    
    var showSuggestionDialog by remember { mutableStateOf(false) }
    var suggestionText by remember { mutableStateOf("") }

    // Simple column structure matching Main page formatting
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(id = R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Settings content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
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

                // Suggestion button
                Button(
                    onClick = { showSuggestionDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.send_suggestion))
                }

                Spacer(Modifier.height(16.dp))

                // Developer credit image and text
                Image(
                    painter = painterResource(id = R.drawable.dog),
                    contentDescription = "Developer branding",
                    modifier = Modifier
                        .height(80.dp)
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = "የዳዊት ስራ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Button(
                    onClick = onSignOut, // Call the provided lambda
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color.White
                    )
                    Text(
                        text = stringResource(id = R.string.settings_sign_out),
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Profile FAB in bottom-right corner
        ExtendedFloatingActionButton(
            onClick = onOpenProfile,
            icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) },
            text = { Text(text = stringResource(id = R.string.profile)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    if (showSuggestionDialog) {
        AlertDialog(
            onDismissRequest = { showSuggestionDialog = false },
            confirmButton = {
                Button(
                    enabled = !submitting.value && suggestionText.isNotBlank(),
                    onClick = {
                        viewModel.submitSuggestion(suggestionText)
                    }
                ) {
                    if (submitting.value) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(id = R.string.dialog_button_save))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuggestionDialog = false }) {
                    Text(stringResource(id = R.string.dialog_button_cancel))
                }
            },
            title = { Text(stringResource(id = R.string.send_suggestion)) },
            text = {
                OutlinedTextField(
                    value = suggestionText,
                    onValueChange = { suggestionText = it },
                    label = { Text(stringResource(id = R.string.suggestion_input_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    // Close dialog after successful submission
    if (submitted.value && showSuggestionDialog) {
        showSuggestionDialog = false
        suggestionText = ""
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