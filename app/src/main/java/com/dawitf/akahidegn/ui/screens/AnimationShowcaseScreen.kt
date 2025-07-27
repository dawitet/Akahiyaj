package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.ui.components.*

/**
 * Animation Showcase Screen
 * Demonstrates all available animation components with interactive controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationShowcaseScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Success", "Error", "Warning", "Loading", "Settings")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Animation Showcase") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> SuccessAnimationDemo()
            1 -> ErrorAnimationDemo()
            2 -> WarningAnimationDemo()
            3 -> LoadingAnimationDemo()
            4 -> AnimationSettingsDemo()
        }
    }
}

@Composable
fun SuccessAnimationDemo() {
    var showAnimation by remember { mutableStateOf(false) }
    var animationSpeed by remember { mutableStateOf(AnimationSpeed.Normal) }
    var slideDirection by remember { mutableStateOf(SlideDirection.TOP) }
    var animationSize by remember { mutableStateOf(AnimationSize.Medium) }
    var enableSwipeToDismiss by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Success Animation Demo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animation Speed Control
                    Text("Animation Speed:", fontWeight = FontWeight.Medium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val speedOptions = listOf(
                            AnimationSpeed.Slow to "Slow",
                            AnimationSpeed.Normal to "Normal",
                            AnimationSpeed.Fast to "Fast"
                        )
                        speedOptions.forEach { (speed, label) ->
                            FilterChip(
                                selected = animationSpeed == speed,
                                onClick = { animationSpeed = speed },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slide Direction Control
                    Text("Slide Direction:", fontWeight = FontWeight.Medium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(SlideDirection.TOP, SlideDirection.BOTTOM, SlideDirection.LEFT, SlideDirection.RIGHT).forEach { direction ->
                            FilterChip(
                                selected = slideDirection == direction,
                                onClick = { slideDirection = direction },
                                label = { Text(direction.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Size Control
                    Text("Animation Size:", fontWeight = FontWeight.Medium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(AnimationSize.Small, AnimationSize.Medium, AnimationSize.Large).forEach { size ->
                            FilterChip(
                                selected = animationSize == size,
                                onClick = { animationSize = size },
                                label = { Text(size.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Options
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enableSwipeToDismiss,
                            onCheckedChange = { enableSwipeToDismiss = it }
                        )
                        Text("Enable Swipe to Dismiss")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trigger Button
                    Button(
                        onClick = { showAnimation = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Success Animation")
                    }
                }
            }
        }

        item {
            // Demo with different configurations
            DemoConfigurationCard(
                title = "Custom Icon Success",
                description = "Success animation with custom icon"
            ) {
                SuccessAnimationCard(
                    isVisible = showAnimation,
                    title = "Custom Success!",
                    subtitle = "Using custom icon instead of checkmark",
                    onDismiss = { showAnimation = false },
                    customIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star icon",
                            tint = Color(0xFFFFD700), // Gold color
                            modifier = Modifier.size(64.dp)
                        )
                    }
                )
            }
        }
    }

    // Main Success Animation
    SuccessAnimationCard(
        isVisible = showAnimation,
        title = "በጣም ጥሩ!",
        subtitle = "የእርስዎ ተግባር በተሳካ ሁኔታ ተጠናቋል።",
        onDismiss = { showAnimation = false },
        animationSpeed = animationSpeed,
        slideDirection = slideDirection,
        size = animationSize,
        enableSwipeToDismiss = enableSwipeToDismiss
    )
}

@Composable
fun ErrorAnimationDemo() {
    var showAnimation by remember { mutableStateOf(false) }
    var showRetryButton by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error Animation Demo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showRetryButton,
                            onCheckedChange = { showRetryButton = it }
                        )
                        Text("Show Retry Button")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAnimation = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Show Error Animation")
                    }
                }
            }
        }
    }

    ErrorAnimationCard(
        isVisible = showAnimation,
        title = "ስህተት!",
        subtitle = "የሆነ ችግር ተፈጥሯል። እባክዎ እንደገና ይሞክሩ።",
        onDismiss = { showAnimation = false },
        onRetry = if (showRetryButton) {{ showAnimation = false }} else null
    )
}

@Composable
fun WarningAnimationDemo() {
    var showAnimation by remember { mutableStateOf(false) }
    var showActionButton by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Warning Animation Demo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showActionButton,
                            onCheckedChange = { showActionButton = it }
                        )
                        Text("Show Action Button")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAnimation = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("Show Warning Animation")
                    }
                }
            }
        }
    }

    WarningAnimationCard(
        isVisible = showAnimation,
        title = "ማስታወሻ!",
        subtitle = "ከመቀጠልዎ በፊት እባክዎ ይህንን ይመልከቱ።",
        onDismiss = { showAnimation = false },
        onAction = if (showActionButton) {{ showAnimation = false }} else null,
        actionText = "ተግባር"
    )
}

@Composable
fun LoadingAnimationDemo() {
    var showAnimation by remember { mutableStateOf(false) }
    var loadingTitle by remember { mutableStateOf("እየጠብቅ...") }
    var loadingSubtitle by remember { mutableStateOf("እባክዎ ይጠብቁ") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Loading Animation Demo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = loadingTitle,
                        onValueChange = { loadingTitle = it },
                        label = { Text("Loading Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = loadingSubtitle,
                        onValueChange = { loadingSubtitle = it },
                        label = { Text("Loading Subtitle") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAnimation = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Show Loading")
                        }

                        Button(
                            onClick = { showAnimation = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Stop Loading")
                        }
                    }
                }
            }
        }
    }

    LoadingAnimationCard(
        isVisible = showAnimation,
        title = loadingTitle,
        subtitle = loadingSubtitle
    )
}

@Composable
fun AnimationSettingsDemo() {
    val animationController = rememberAnimationController()
    var enableSoundEffects by remember { mutableStateOf(false) }
    var enableHapticFeedback by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Animation Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animation Controller
                    Text("Animation Control:", fontWeight = FontWeight.Medium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { animationController.pause() },
                            enabled = !animationController.isPaused
                        ) {
                            Text("Pause")
                        }

                        Button(
                            onClick = { animationController.resume() },
                            enabled = animationController.isPaused
                        ) {
                            Text("Resume")
                        }

                        Button(
                            onClick = { animationController.toggle() }
                        ) {
                            Text("Toggle")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Status: ${if (animationController.isPaused) "Paused" else "Running"}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sound Effects
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enableSoundEffects,
                            onCheckedChange = { enableSoundEffects = it }
                        )
                        Text("Enable Sound Effects")
                    }

                    // Haptic Feedback
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enableHapticFeedback,
                            onCheckedChange = { enableHapticFeedback = it }
                        )
                        Text("Enable Haptic Feedback")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test Sound Effects
                    if (enableSoundEffects) {
                        Text("Test Sound Effects:", fontWeight = FontWeight.Medium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { SoundEffectsManager.playSuccessSound() }
                            ) {
                                Text("Success")
                            }

                            Button(
                                onClick = { SoundEffectsManager.playErrorSound() }
                            ) {
                                Text("Error")
                            }

                            Button(
                                onClick = { SoundEffectsManager.playWarningSound() }
                            ) {
                                Text("Warning")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Performance Information",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val performanceItems = listOf(
                        "✓ Path objects are cached for better performance",
                        "✓ derivedStateOf used for expensive calculations",
                        "✓ remember used to prevent unnecessary recomposition",
                        "✓ Error handling implemented for animation failures",
                        "✓ Accessibility support with reduced motion detection",
                        "✓ Semantic properties for screen readers"
                    )

                    performanceItems.forEach { item ->
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DemoConfigurationCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnimationShowcaseScreenPreview() {
    MaterialTheme {
        AnimationShowcaseScreen()
    }
}
