package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Optimized Image Loading Components
 * Provides efficient image loading with placeholders, error handling, and caching
 */

/**
 * LazyImageLoader - Optimized component for loading images with proper memory management
 */
@Composable
fun LazyImageLoader(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isCircular: Boolean = false,
    cornerRadius: Int = 8,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    errorColor: Color = MaterialTheme.colorScheme.errorContainer
) {
    val context = LocalContext.current
    
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    
    val imageShape = if (isCircular) {
        CircleShape
    } else {
        RoundedCornerShape(cornerRadius.dp)
    }
    
    Box(
        modifier = modifier.clip(imageShape),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder while loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(placeholderColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Error state
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(errorColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (contentDescription?.contains("profile", ignoreCase = true) == true) {
                        Icons.Default.Person
                    } else {
                        Icons.Default.BrokenImage
                    },
                    contentDescription = "Image failed to load",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Actual image with Coil
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            onLoading = { isLoading = true },
            onSuccess = { 
                isLoading = false
                isError = false
            },
            onError = {
                isLoading = false
                isError = true
            }
        )
    }
}

/**
 * OptimizedProfileImage - Specialized version for profile images
 */
@Composable
fun OptimizedProfileImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "Profile Image"
) {
    LazyImageLoader(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        isCircular = true,
        placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
