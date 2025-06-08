package com.dawitf.akahidegn.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dawitf.akahidegn.dataStore
import com.dawitf.akahidegn.MainActivity
import com.dawitf.akahidegn.ui.components.ThemeToggleCard
import com.dawitf.akahidegn.ui.components.BilingualText
import com.dawitf.akahidegn.ui.components.AnimatedPressableCard
import com.dawitf.akahidegn.ui.components.GlassmorphismCard
import com.dawitf.akahidegn.ui.components.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var textScaleFactor by remember { mutableFloatStateOf(1.0f) }
    var enableHighContrastMode by remember { mutableStateOf(false) }
    var enableHapticFeedback by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoPlaceNearbyGroups by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme Settings Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BilingualText(
                    englishText = "Appearance",
                    amharicText = "የገፅታ ቅንብሮች",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                
                ThemeToggleCard(
                    currentTheme = currentThemeMode,
                    onThemeChanged = { newThemeMode ->
                        coroutineScope.launch {
                            context.dataStore.edit { preferences ->
                                preferences[MainActivity.THEME_MODE_KEY] = newThemeMode.name
                            }
                            onThemeChanged(newThemeMode)
                        }
                    }
                )
                
                // Text Size Setting
                GlassmorphismCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BilingualText(
                            englishText = "Text Size",
                            amharicText = "የፅሁፍ መጠን",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Slider(
                            value = textScaleFactor,
                            onValueChange = { textScaleFactor = it },
                            valueRange = 0.8f..1.4f,
                            steps = 3,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                }
                
                // High Contrast Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BilingualText(
                            englishText = "High Contrast Mode",
                            amharicText = "የከፍተኛ ንፅፅር ሁነታ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Better visibility in bright light",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enableHighContrastMode,
                        onCheckedChange = { enableHighContrastMode = it }
                    )
                }
            }
            
            // Notification Settings
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BilingualText(
                    englishText = "Notifications",
                    amharicText = "ማሳወቂያዎች",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BilingualText(
                            englishText = "Enable Notifications",
                            amharicText = "ማሳወቂያዎችን አንቃ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Get notified about your rides",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            }
            
            // Location & Maps Settings
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BilingualText(
                    englishText = "Location & Maps",
                    amharicText = "አካባቢ እና ካርታዎች",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BilingualText(
                            englishText = "Auto-place nearby groups",
                            amharicText = "በአካባቢዎ ያሉ ቡድኖችን በራስ-ሰር አሳይ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Automatically show groups near your location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoPlaceNearbyGroups,
                        onCheckedChange = { autoPlaceNearbyGroups = it }
                    )
                }
            }
            
            // Accessibility Settings
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BilingualText(
                    englishText = "Accessibility",
                    amharicText = "ተደራሽነት",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BilingualText(
                            englishText = "Haptic Feedback",
                            amharicText = "የንክኪ ምላሽ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Vibration when touching interactive elements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enableHapticFeedback,
                        onCheckedChange = { enableHapticFeedback = it }
                    )
                }
            }
            
            // About Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BilingualText(
                    englishText = "About",
                    amharicText = "ስለ አፕሊኬሽኑ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                
                AnimatedPressableCard(
                    onClick = { /* Open app version info */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "Akahiyaj (አካሂያጅ)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
