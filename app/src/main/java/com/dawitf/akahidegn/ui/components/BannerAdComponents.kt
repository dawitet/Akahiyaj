package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.dawitf.akahidegn.BuildConfig

@Composable
fun CarouselBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_ID // Use build config
) {
    val context = LocalContext.current
    
    // Don't show ads if disabled or empty ad ID
    if (!BuildConfig.ADS_ENABLED || adUnitId.isEmpty()) {
        Log.d("BannerAd", "Ads disabled - ADS_ENABLED: ${BuildConfig.ADS_ENABLED}, BANNER_ID: '$adUnitId'")
        return
    }
    
    Log.d("BannerAd", "Creating banner ad with ID: $adUnitId")
    
    Card(
        modifier = modifier.size(width = 140.dp, height = 160.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.MEDIUM_RECTANGLE)
                    this.adUnitId = adUnitId
                    Log.d("BannerAd", "Loading banner ad...")
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        )
    }
}

@Composable
fun CarouselPlaceholderAd(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(width = 140.dp, height = 160.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📢",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Advertisement",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FullWidthBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_ID // Use build config
) {
    val context = LocalContext.current
    
    // Don't show ads if disabled or empty ad ID
    if (!BuildConfig.ADS_ENABLED || adUnitId.isEmpty()) {
        Log.d("BannerAd", "Full width ads disabled - ADS_ENABLED: ${BuildConfig.ADS_ENABLED}, BANNER_ID: '$adUnitId'")
        return
    }
    
    Log.d("BannerAd", "Creating full width banner ad with ID: $adUnitId")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp), // Standard banner height
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER) // Standard banner size (320x50)
                    this.adUnitId = adUnitId
                    Log.d("BannerAd", "Loading full width banner ad...")
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}
