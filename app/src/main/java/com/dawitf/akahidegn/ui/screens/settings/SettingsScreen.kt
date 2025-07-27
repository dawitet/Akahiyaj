package com.dawitf.akahidegn.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.viewmodel.SettingsViewModel

/**
 * Settings screen for notification preferences including sound and vibration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = "ቅንብሮች", // Settings in Amharic
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔔 ማሳወቂያዎች", // Notifications in Amharic
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enable/Disable Notifications
                    SettingsToggleItem(
                        title = "ማሳወቂያዎችን ማንቃት", // Enable Notifications
                        description = "ስለ ቡድን ክስተቶች ማሳወቂያ ይቀበሉ", // Receive notifications about group events
                        icon = Icons.Default.Notifications,
                        checked = notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Sound Settings
                    SettingsToggleItem(
                        title = "ድምፅ", // Sound
                        description = "ለማሳወቂያዎች ድምፅ አንጻት", // Play sound for notifications
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        checked = soundEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = viewModel::setSoundEnabled
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Vibration Settings
                    SettingsToggleItem(
                        title = "ንዝረት", // Vibration
                        description = "ለማሳወቂያዎች እና ክስተቶች ንዝረት", // Vibrate for notifications and events
                        icon = Icons.Default.Vibration,
                        checked = vibrationEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = viewModel::setVibrationEnabled
                    )
                }
            }

            // Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ የማሳወቂያ አይነቶች", // Notification Types
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotificationTypeItem(
                            emoji = "👋",
                            title = "አባል ተቀላቀለ", // Member Joined
                            description = "ወደ ቡድንዎ አንድ ሰው ሲቀላቀል" // When someone joins your group
                        )

                        NotificationTypeItem(
                            emoji = "🚪",
                            title = "አባል ወጣ", // Member Left
                            description = "ከቡድንዎ አንድ ሰው ሲወጣ" // When someone leaves your group
                        )

                        NotificationTypeItem(
                            emoji = "🚗",
                            title = "ቡድን ሞላ", // Group Full
                            description = "ቡድንዎ ከፍተኛ አቅሙን ሲደርስ" // When your group reaches maximum capacity
                        )

                        NotificationTypeItem(
                            emoji = "⚠️",
                            title = "ቡድን ተበተነ", // Group Disbanded
                            description = "ቡድን ሲበተን" // When a group is disbanded
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun NotificationTypeItem(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
