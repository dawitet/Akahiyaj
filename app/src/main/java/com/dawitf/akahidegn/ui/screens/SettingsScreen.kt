package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.viewmodel.SettingsViewModel
import com.dawitf.akahidegn.ui.components.BilingualText
import com.dawitf.akahidegn.ui.components.ThemeToggleCard
import com.dawitf.akahidegn.ui.components.ThemeMode
import com.dawitf.akahidegn.MainActivity
import com.dawitf.akahidegn.data.datastore.dataStore
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit // Added missing import

// NOTE: This file consolidates the previously duplicated SettingsScreen implementations found in:
//  - ui/settings/SettingsScreen.kt (theme + appearance + about)
//  - ui/screens/SettingsScreen.kt (sign-out, suggestion dialog, toggles)
//  - ui/screens/settings/SettingsScreen.kt (empty placeholder)
// Only this implementation should be used going forward.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onOpenProfile: () -> Unit = {},
    // Optional theme controls (retained for backward compatibility)
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeChanged: (ThemeMode) -> Unit = {},
    onNavigateBack: () -> Unit = {} // Allows embedding in its own NavHost if needed
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val notificationsEnabledState = viewModel.notificationsEnabled.collectAsState()
    val soundEnabledState = viewModel.soundEnabled.collectAsState()
    val vibrationEnabledState = viewModel.vibrationEnabled.collectAsState()
    val submitting = viewModel.suggestionSubmitting.collectAsState()
    val submitted = viewModel.suggestionSubmitted.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Local UI states (currently ephemeral; can be hoisted or persisted later)
    var autoPlaceNearbyGroups by remember { mutableStateOf(true) }
    var showSuggestionDialog by remember { mutableStateOf(false) }
    var suggestionText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenProfile,
                icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) },
                text = { Text(text = stringResource(id = R.string.profile)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Appearance / Theme Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BilingualText(
                    englishText = "Appearance",
                    amharicText = "የገፅታ ቅንብሮች",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                ThemeToggleCard(
                    currentTheme = currentThemeMode,
                    onThemeChanged = { newTheme ->
                        coroutineScope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[MainActivity.THEME_MODE_KEY] = newTheme.name
                            }
                            onThemeChanged(newTheme)
                        }
                    }
                )
            }

            // Core Settings Toggles
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_description),
                    style = MaterialTheme.typography.bodyLarge
                )
                SettingToggleRow(
                    title = stringResource(id = R.string.notification_permission_needed),
                    checked = notificationsEnabledState.value,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                SettingToggleRow(
                    title = stringResource(id = R.string.settings_sound_effects),
                    checked = soundEnabledState.value,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )
                SettingToggleRow(
                    title = stringResource(id = R.string.settings_vibration),
                    checked = vibrationEnabledState.value,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
            }

            // Location & Maps Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BilingualText(
                    englishText = "Location & Maps",
                    amharicText = "አካባቢ እና ካርታዎች",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                SettingToggleRow(
                    title = "Auto-place nearby groups",
                    checked = autoPlaceNearbyGroups,
                    onCheckedChange = { autoPlaceNearbyGroups = it }
                )
            }

            // Suggestion Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.send_suggestion),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Button(
                    onClick = { showSuggestionDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(text = stringResource(id = R.string.send_suggestion)) }
            }

            // About Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BilingualText(
                    englishText = "About",
                    amharicText = "ስለ አፕሊኬሽኑ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(text = "Akahiyaj (አካሂያጅ)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Developer credit
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.dog),
                        contentDescription = "Developer branding",
                        modifier = Modifier.height(80.dp)
                    )
                    Text(
                        text = "የዳዊት ስራ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Sign Out
            Button(
                onClick = onSignOut,
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
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSuggestionDialog) {
        AlertDialog(
            onDismissRequest = { showSuggestionDialog = false },
            confirmButton = {
                Button(
                    enabled = !submitting.value && suggestionText.isNotBlank(),
                    onClick = { viewModel.submitSuggestion(suggestionText) }
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
    Row(
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