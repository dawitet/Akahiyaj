package com.dawitf.akahidegn.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.ui.theme.ThemeMode
import com.dawitf.akahidegn.domain.model.LanguageOption
import com.dawitf.akahidegn.domain.model.FontSizeOption
import com.dawitf.akahidegn.viewmodel.SettingsViewModel

/**
 * Settings screen for app configuration including theme, notifications, and accessibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Settings
            item {
                SettingsSection(
                    title = "ገጽታ እና ቅርጸት", // Appearance & Format
                    icon = Icons.Default.Palette
                ) {
                    // Theme Mode
                    SettingsRow(
                        title = "ገጽታ ሁነታ", // Theme Mode
                        subtitle = when (uiState.themeMode) {
                            ThemeMode.LIGHT -> "ብሩህ"
                            ThemeMode.DARK -> "ጨለማ"
                            ThemeMode.SYSTEM -> "ስርዓት"
                        }
                    ) {
                        ThemeModeDialog(
                            currentMode = uiState.themeMode,
                            onModeSelected = viewModel::setThemeMode
                        )
                    }

                    // Language
                    SettingsRow(
                        title = "ቋንቋ", // Language
                        subtitle = when (uiState.language) {
                            LanguageOption.ENGLISH -> "English"
                            LanguageOption.AMHARIC -> "አማርኛ"
                            LanguageOption.OROMO -> "Afaan Oromoo"
                            LanguageOption.SYSTEM -> "ስርዓት ቋንቋ"
                        }
                    ) {
                        LanguageDialog(
                            currentLanguage = uiState.language,
                            onLanguageSelected = viewModel::setLanguage
                        )
                    }

                    // Dynamic Colors
                    SwitchSettingsRow(
                        title = "ተለዋዋጭ ቀለሞች", // Dynamic Colors
                        subtitle = "Material You ዲዛይን",
                        checked = uiState.useDynamicColors,
                        onCheckedChange = viewModel::setDynamicColors
                    )
                }
            }

            // Accessibility Settings
            item {
                SettingsSection(
                    title = "ተደራሽነት", // Accessibility
                    icon = Icons.Default.Accessibility
                ) {
                    // High Contrast
                    SwitchSettingsRow(
                        title = "ከፍተኛ ንፅፅር", // High Contrast
                        subtitle = "በዓይነ ስውር ተጠቃሚዎች ላይ የተመሰረተ",
                        checked = uiState.useHighContrast,
                        onCheckedChange = viewModel::setHighContrast
                    )

                    // Font Size
                    SettingsRow(
                        title = "የቅርጸ-ቁምፊ መጠን", // Font Size
                        subtitle = when (uiState.fontSize) {
                            FontSizeOption.SMALL -> "ትንሽ"
                            FontSizeOption.MEDIUM -> "መደበኛ"
                            FontSizeOption.LARGE -> "ትልቅ"
                            FontSizeOption.EXTRA_LARGE -> "በጣም ትልቅ"
                        }
                    ) {
                        FontSizeDialog(
                            currentSize = uiState.fontSize,
                            onSizeSelected = viewModel::setFontSize
                        )
                    }
                }
            }

            // Notification Settings
            item {
                SettingsSection(
                    title = "ማሳወቂያዎች", // Notifications
                    icon = Icons.Default.Notifications
                ) {
                    // Enable Notifications
                    SwitchSettingsRow(
                        title = "ማሳወቂያዎችን አንቃ", // Enable Notifications
                        subtitle = "አጠቃላይ ማሳወቂያዎች",
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled
                    )

                    if (uiState.notificationsEnabled) {
                        // Chat Notifications
                        SwitchSettingsRow(
                            title = "የውይይት ማሳወቂያዎች", // Chat Notifications
                            subtitle = "አዲስ መልዕክቶች",
                            checked = uiState.chatNotificationsEnabled,
                            onCheckedChange = viewModel::setChatNotificationsEnabled,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        // Trip Notifications
                        SwitchSettingsRow(
                            title = "የጉዞ ማሳወቂያዎች", // Trip Notifications
                            subtitle = "ጉዞ ዝመናዎች",
                            checked = uiState.tripNotificationsEnabled,
                            onCheckedChange = viewModel::setTripNotificationsEnabled,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        // System Notifications
                        SwitchSettingsRow(
                            title = "የስርዓት ማሳወቂያዎች", // System Notifications
                            subtitle = "አፕ ዝመናዎች",
                            checked = uiState.systemNotificationsEnabled,
                            onCheckedChange = viewModel::setSystemNotificationsEnabled,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        // Quiet Hours
                        SwitchSettingsRow(
                            title = "ጸጥታ ሰዓቶች", // Quiet Hours
                            subtitle = "ከ10:00 PM እስከ 7:00 AM",
                            checked = uiState.quietHoursEnabled,
                            onCheckedChange = viewModel::setQuietHoursEnabled,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            // Privacy & Data
            item {
                SettingsSection(
                    title = "ግላዊነት እና መረጃ", // Privacy & Data
                    icon = Icons.Default.Security
                ) {
                    // Analytics
                    SwitchSettingsRow(
                        title = "ትንታኔ ማጋራት", // Share Analytics
                        subtitle = "አፕን ለማሻሻል ይረዳል",
                        checked = uiState.analyticsEnabled,
                        onCheckedChange = viewModel::setAnalyticsEnabled
                    )

                    // Location Data
                    SwitchSettingsRow(
                        title = "የቦታ መረጃ", // Location Data
                        subtitle = "ለተሻለ ሰርች ውጤት",
                        checked = uiState.locationDataEnabled,
                        onCheckedChange = viewModel::setLocationDataEnabled
                    )
                }
            }

            // About Section
            item {
                SettingsSection(
                    title = "ስለ አፕ", // About App
                    icon = Icons.Default.Info
                ) {
                    SettingsRow(
                        title = "አካሂያጅ",
                        subtitle = "እትም 1.0.0"
                    ) {
                        // Could open about dialog
                    }

                    ClickableSettingsRow(
                        title = "የግላዊነት ፖሊሲ", // Privacy Policy
                        subtitle = "ያንብቡ"
                    ) {
                        // Open privacy policy
                    }

                    ClickableSettingsRow(
                        title = "የአገልግሎት ውሎች", // Terms of Service
                        subtitle = "ያንብቡ"
                    ) {
                        // Open terms of service
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { showDialog = true }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    if (showDialog) {
        content()
        LaunchedEffect(showDialog) {
            if (showDialog) {
                showDialog = false
            }
        }
    }
}

@Composable
private fun SwitchSettingsRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ClickableSettingsRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Dialog components for settings
@Composable
private fun ThemeModeDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("ገጽታ ሁነታ ይምረጡ") },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onModeSelected(mode)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentMode == mode,
                                onClick = {
                                    onModeSelected(mode)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.LIGHT -> "ብሩህ"
                                    ThemeMode.DARK -> "ጨለማ"
                                    ThemeMode.SYSTEM -> "ስርዓት"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("ሰርዝ")
                }
            }
        )
    }
}

@Composable
private fun LanguageDialog(
    currentLanguage: LanguageOption,
    onLanguageSelected: (LanguageOption) -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("ቋንቋ ይምረጡ") },
            text = {
                Column {
                    LanguageOption.values().forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageSelected(language)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == language,
                                onClick = {
                                    onLanguageSelected(language)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (language) {
                                    LanguageOption.ENGLISH -> "English"
                                    LanguageOption.AMHARIC -> "አማርኛ"
                                    LanguageOption.OROMO -> "Afaan Oromoo"
                                    LanguageOption.SYSTEM -> "ስርዓት ቋንቋ"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("ሰርዝ")
                }
            }
        )
    }
}

@Composable
private fun FontSizeDialog(
    currentSize: FontSizeOption,
    onSizeSelected: (FontSizeOption) -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("የቅርጸ-ቁምፊ መጠን ይምረጡ") },
            text = {
                Column {
                    FontSizeOption.values().forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSizeSelected(size)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSize == size,
                                onClick = {
                                    onSizeSelected(size)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (size) {
                                    FontSizeOption.SMALL -> "ትንሽ"
                                    FontSizeOption.MEDIUM -> "መደበኛ"
                                    FontSizeOption.LARGE -> "ትልቅ"
                                    FontSizeOption.EXTRA_LARGE -> "በጣም ትልቅ"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("ሰርዝ")
                }
            }
        )
    }
}
