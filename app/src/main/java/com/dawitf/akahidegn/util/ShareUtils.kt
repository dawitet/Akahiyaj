package com.dawitf.akahidegn.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility class for sharing app functionality
 */
object ShareUtils {
    
    /**
     * Share the current app APK file
     */
    fun shareApp(context: Context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appName = context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(context.packageName, 0)!!
            ).toString()
            
            // Get the APK file
            val apkFile = File(packageInfo.applicationInfo?.sourceDir ?: return)
            
            if (apkFile.exists()) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, apkUri)
                    putExtra(Intent.EXTRA_SUBJECT, appName)
                    putExtra(Intent.EXTRA_TEXT, 
                        "Check out this amazing app: $appName\n" +
                        "ይህን አስደናቂ መተግበሪያ ይመልከቱ: $appName")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(Intent.createChooser(shareIntent, "Share $appName"))
            } else {
                // Fallback: Share app store link
                shareAppStoreLink(context, appName)
            }
        } catch (e: Exception) {
            // Fallback: Share a general message about the app
            shareAppMessage(context)
        }
    }
    
    /**
     * Share app store link (fallback option)
     */
    private fun shareAppStoreLink(context: Context, appName: String) {
        val packageName = context.packageName
        val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName)
            putExtra(Intent.EXTRA_TEXT, 
                "Check out this amazing app: $appName\n$playStoreUrl\n" +
                "ይህን አስደናቂ መተግበሪያ ይመልከቱ: $appName")
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share $appName"))
    }
    
    /**
     * Share app message (ultimate fallback)
     */
    private fun shareAppMessage(context: Context) {
        val appName = try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(context.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            "Akahidegn"
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName)
            putExtra(Intent.EXTRA_TEXT, 
                "I'm using $appName - a great ride-sharing app!\n" +
                "$appName ን እየተጠቀምኩ ነው - ትላንት የጉዞ አጋሪ መተግበሪያ!")
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share $appName"))
    }
}
