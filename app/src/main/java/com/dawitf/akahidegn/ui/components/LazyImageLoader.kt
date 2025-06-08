package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.dawitf.akahidegn.util.AvatarUtil

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
    errorColor: Color = MaterialTheme.colorScheme.errorContainer,
    fallbackAvatarId: String? = null, // For consistent avatar selection
    isUserAvatar: Boolean = false, // To differentiate between user and group avatars
    isGroupAvatar: Boolean = false
) {
    val context = LocalContext.current
    
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    
    val imageShape = if (isCircular) {
        CircleShape
    } else {
        RoundedCornerShape(cornerRadius.dp)
    }
    
    // Get appropriate fallback avatar
    val fallbackAvatar = remember(fallbackAvatarId, isUserAvatar, isGroupAvatar) {
        when {
            isUserAvatar -> AvatarUtil.getUserAvatar(fallbackAvatarId)
            isGroupAvatar -> AvatarUtil.getGroupAvatar(fallbackAvatarId)
            contentDescription?.contains("profile", ignoreCase = true) == true -> AvatarUtil.getUserAvatar(fallbackAvatarId)
            contentDescription?.contains("group", ignoreCase = true) == true -> AvatarUtil.getGroupAvatar(fallbackAvatarId)
            else -> AvatarUtil.getDefaultUserAvatar()
        }
    }
    
    Box(
        modifier = modifier.clip(imageShape),
        contentAlignment = Alignment.Center
    ) {
        // Show fallback avatar when URL is null/empty or when there's an error
        if (imageUrl.isNullOrBlank() || isError) {
            Image(
                painter = painterResource(id = fallbackAvatar),
                contentDescription = contentDescription ?: "Avatar",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder while loading
            if (isLoading) {
                Image(
                    painter = painterResource(id = fallbackAvatar),
                    contentDescription = contentDescription ?: "Avatar",
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Loading indicator overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
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
}

/**
 * OptimizedProfileImage - Specialized version for profile images
 */
@Composable
fun OptimizedProfileImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "Profile Image",
    userId: String? = null
) {
    LazyImageLoader(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        isCircular = true,
        placeholderColor = MaterialTheme.colorScheme.surfaceVariant,
        fallbackAvatarId = userId,
        isUserAvatar = true
    )
}
