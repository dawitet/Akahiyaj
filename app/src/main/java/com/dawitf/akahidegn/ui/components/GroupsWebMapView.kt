package com.dawitf.akahidegn.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.dawitf.akahidegn.Group
import com.google.gson.Gson

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GroupsWebMapView(
    context: Context,
    groups: List<Group>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                // Enable hardware acceleration - this is critical for map rendering
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                
                // WebView settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                
                // Additional settings for map rendering
                settings.allowUniversalAccessFromFileURLs = true
                settings.allowFileAccessFromFileURLs = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportZoom(true)
                
                // Force WebView to render content
                setInitialScale(1)
                
                // Remove background color now that we confirmed WebView is visible
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                
                Log.d("GroupsWebMapView", "WebView created with enhanced settings for map rendering")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("GroupsWebMapView", "Page loaded, WebView size: ${view?.width}x${view?.height}")
                    }
                    
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d("GroupsWebMapView", "Page started loading: $url")
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Log.d("WebView-Console", "${consoleMessage.messageLevel()}: ${consoleMessage.message()}")
                        return true
                    }
                }
                loadUrl("file:///android_asset/leaflet_map.html")
            }
        },
        update = { webView ->
            if (groups.isNotEmpty()) {
                val validGroups = groups.filter { it.pickupLat != null && it.pickupLng != null }
                Log.d("GroupsWebMapView", "Updating map with ${validGroups.size} valid groups")
                val groupsJson = Gson().toJson(validGroups).replace("'", "\\'")
                webView.evaluateJavascript("window.addGroups('$groupsJson')", null)
            }
        }
    )
}
