package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Enhanced Shimmer Loading Components
 * Beautiful skeleton loading states to replace basic progress indicators
 */

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val offset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_offset"
    )
    
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(offset * 300, offset * 300),
                    end = Offset((offset + 1) * 300, (offset + 1) * 300)
                )
            )
    )
}

@Composable
fun ShimmerGroupCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title shimmer
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp),
                cornerRadius = 4.dp
            )
            
            // Subtitle shimmer
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp),
                cornerRadius = 4.dp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left info shimmer
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp),
                    cornerRadius = 4.dp
                )
                
                // Right button shimmer
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(32.dp),
                    cornerRadius = 16.dp
                )
            }
        }
    }
}

@Composable
fun ShimmerGroupList(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    // Use a regular Column instead of LazyVerticalGrid to avoid nested scrolling issues
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount.coerceAtMost(6)) {
            ShimmerGroupCard(
                modifier = Modifier.fillMaxWidth()
            )
            
            // Add shimmer banner ad placeholder after the 3rd item
            if (it == 2 && itemCount > 4) {
                ShimmerBannerAd(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Keep the original implementation as an alternative when needed outside scrollable containers
@Composable
fun ShimmerGroupGridList(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        items(itemCount) {
            ShimmerGroupCard()
        }
        
        // Add shimmer banner ad placeholder every 3rd row (after 2 cards)
        if (itemCount > 4) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                ShimmerBannerAd()
            }
        }
    }
}

@Composable
fun ShimmerCarouselItem(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 280.dp,
    height: androidx.compose.ui.unit.Dp = 160.dp
) {
    Card(
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier.size(24.dp),
                    cornerRadius = 12.dp
                )
                
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp),
                    cornerRadius = 4.dp
                )
            }
            
            // Description lines
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                cornerRadius = 4.dp
            )
            
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp),
                cornerRadius = 4.dp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp),
                    cornerRadius = 4.dp
                )
                
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(28.dp),
                    cornerRadius = 14.dp
                )
            }
        }
    }
}

@Composable
fun ShimmerProfileHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile picture shimmer
        ShimmerBox(
            modifier = Modifier.size(60.dp),
            cornerRadius = 30.dp
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Name shimmer
            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(20.dp),
                cornerRadius = 4.dp
            )
            
            // Subtitle shimmer
            ShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp),
                cornerRadius = 4.dp
            )
        }
    }
}

@Composable
fun ShimmerSearchBar(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier.size(24.dp),
                cornerRadius = 12.dp
            )
            
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp),
                cornerRadius = 4.dp
            )
        }
    }
}

@Composable
fun ShimmerStatsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(20.dp),
                cornerRadius = 4.dp
            )
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(40.dp)
                                .height(24.dp),
                            cornerRadius = 4.dp
                        )
                        
                        ShimmerBox(
                            modifier = Modifier
                                .width(60.dp)
                                .height(16.dp),
                            cornerRadius = 4.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulsingShimmer(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_shimmer")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
            )
    )
}

@Composable
fun ShimmerBannerAd(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp),
                    cornerRadius = 4.dp
                )
                
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp),
                    cornerRadius = 4.dp
                )
                
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(20.dp),
                    cornerRadius = 10.dp
                )
            }
            
            // Right icon/image placeholder
            ShimmerBox(
                modifier = Modifier.size(60.dp),
                cornerRadius = 8.dp
            )
        }
    }
}
