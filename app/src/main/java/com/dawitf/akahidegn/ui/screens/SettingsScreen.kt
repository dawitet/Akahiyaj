package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dawitf.akahidegn.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.dawitf.akahidegn.viewmodel.SettingsViewModel
import com.dawitf.akahidegn.ui.components.gradientBackground
import com.dawitf.akahidegn.ui.components.SettingsTabLayout
import com.dawitf.akahidegn.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onSignOut: () -> Unit) { // Added onSignOut callback
    val context = LocalContext.current
    val viewModel: SettingsViewModel = hiltViewModel()
    val notificationsEnabled = viewModel.notificationsEnabled.collectAsState()
    val soundEnabled = viewModel.soundEnabled.collectAsState()
    val vibrationEnabled = viewModel.vibrationEnabled.collectAsState()
    
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }
    
    SettingsTabLayout(
        headerContent = {
            // Header content with title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = com.dawitf.akahidegn.ui.theme.SettingsContentText
                )
            }
        },
        mainContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween, // Changed to space out content
                horizontalAlignment = Alignment.CenterHorizontally // Center content horizontally
            ) {
                Text(
                    text = stringResource(id = R.string.settings_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = com.dawitf.akahidegn.ui.theme.SettingsContentText,
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
                
                Spacer(modifier = Modifier.weight(1f)) // Pushes footer and buttons to the bottom

                // Service/Shuttle Button
                OutlinedButton(
                    onClick = { 
                        showComingSoonDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = com.dawitf.akahidegn.ui.theme.SettingsContentText
                    )
                ) {
                    Text(
                        text = "🚐 ሰርቪስ",
                        color = com.dawitf.akahidegn.ui.theme.SettingsContentText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(በቅርብ ይመጣል)",
                        color = com.dawitf.akahidegn.ui.theme.SettingsContentText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Share App Button
                OutlinedButton(
                    onClick = { 
                        ShareUtils.shareApp(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = com.dawitf.akahidegn.ui.theme.SettingsContentText
                    )
                ) {
                    Text(
                        text = "📤 አፕን አጋራ",
                        color = com.dawitf.akahidegn.ui.theme.SettingsContentText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Feedback Button
                OutlinedButton(
                    onClick = { showFeedbackDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = com.dawitf.akahidegn.ui.theme.SettingsContentText
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = "Feedback",
                        tint = com.dawitf.akahidegn.ui.theme.SettingsContentText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "አስተያየት ስጡ",
                        color = com.dawitf.akahidegn.ui.theme.SettingsContentText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // የዳዊት ስራ Footer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = com.dawitf.akahidegn.ui.theme.SettingsContentText.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🐕", // Dog emoji placeholder for dog.png
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.size(32.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "የዳዊት ስራ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = com.dawitf.akahidegn.ui.theme.SettingsContentText,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Dawit's Work",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.dawitf.akahidegn.ui.theme.SettingsContentText.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSignOut, // Call the provided lambda
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.dawitf.akahidegn.ui.theme.SettingsContentText.copy(alpha = 0.8f),
                        contentColor = com.dawitf.akahidegn.ui.theme.SettingsContentBackground
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
                    Text(
                        text = stringResource(id = R.string.settings_sign_out),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            // Feedback Dialog
            if (showFeedbackDialog) {
                FeedbackDialog(
                    onDismiss = { showFeedbackDialog = false },
                    onSubmit = { feedback ->
                        // Handle feedback submission
                        // For now, just dismiss the dialog
                        // In a real app, you would send this to your server
                        showFeedbackDialog = false
                    }
                )
            }
            
            // Coming Soon Dialog
            if (showComingSoonDialog) {
                AlertDialog(
                    onDismissRequest = { showComingSoonDialog = false },
                    title = {
                        Text(
                            text = "🚐 ሰርቪስ", 
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    text = {
                        Text(
                            text = "የሰርቪስ ባቡር አገልግሎት በቅርብ ጊዜ ይመጣል! እባክዎ ትንሽ ይጠብቁ።",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showComingSoonDialog = false }
                        ) {
                            Text(
                                text = "እሺ",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    )
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
        Text(
            text = title, 
            style = MaterialTheme.typography.bodyLarge,
            color = com.dawitf.akahidegn.ui.theme.SettingsContentText
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = com.dawitf.akahidegn.ui.theme.SettingsContentText,
                checkedTrackColor = com.dawitf.akahidegn.ui.theme.SettingsContentText.copy(alpha = 0.5f),
                uncheckedThumbColor = com.dawitf.akahidegn.ui.theme.SettingsContentText.copy(alpha = 0.5f),
                uncheckedTrackColor = com.dawitf.akahidegn.ui.theme.SettingsContentText.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "አስተያየት ስጡ",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    text = "የመተግበሪያችንን እገዛ ለማሻሻል አስተያየትዎን ያካፍሉን። ምን ያሻሻላል? ምን ችግር አጋጥሞዎታል?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    label = { Text("አስተያየትዎን እዚህ ይጻፉ...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    singleLine = false
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (feedbackText.isNotBlank()) {
                        onSubmit(feedbackText)
                    }
                },
                enabled = feedbackText.isNotBlank()
            ) {
                Text("ላክ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ተወው")
            }
        }
    )
}