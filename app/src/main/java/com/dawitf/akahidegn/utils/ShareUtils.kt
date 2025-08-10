package com.dawitf.akahidegn.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {
    
    /**
     * Shares the app with others using multiple fallback methods
     */
    fun shareApp(context: Context) {
        // Try to share APK file first
        if (!shareApkFile(context)) {
            // Fall back to app store link
            if (!shareAppStoreLink(context)) {
                // Final fallback to simple message
                shareAppMessage(context)
            }
        }
    }
    
    /**
     * Attempts to share the APK file directly
     */
    private fun shareApkFile(context: Context): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val apkFile = File(packageInfo.applicationInfo?.sourceDir ?: return false)
            
            if (apkFile.exists()) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    apkFile
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, apkUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Check out Akahidegn App!")
                    putExtra(Intent.EXTRA_TEXT, "Download and install this amazing app!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Shares link to app store (Google Play Store)
     */
    private fun shareAppStoreLink(context: Context): Boolean {
        return try {
            val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out Akahidegn App!")
                putExtra(Intent.EXTRA_TEXT, "Download Akahidegn app from Google Play Store: $playStoreUrl")
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share App"))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Shares a simple message about the app (final fallback)
     */
    private fun shareAppMessage(context: Context) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Akahidegn App")
                putExtra(Intent.EXTRA_TEXT, "Check out the Akahidegn app - it's amazing!")
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share App"))
        } catch (e: Exception) {
            // If even this fails, there's not much we can do
        }
    }
}
