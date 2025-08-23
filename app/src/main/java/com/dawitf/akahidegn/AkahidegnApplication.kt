package com.dawitf.akahidegn

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dawitf.akahidegn.service.GroupCleanupScheduler
import com.dawitf.akahidegn.util.TestDeviceHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.osmdroid.config.Configuration // Added for OSMdroid
import androidx.preference.PreferenceManager // Added for OSMdroid

@HiltAndroidApp
class AkahidegnApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var groupCleanupScheduler: GroupCleanupScheduler
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()

        // OSMdroid configuration
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID // Using BuildConfig for safety

        try {
            // Initialize Firebase App first (important for other Firebase services)
            FirebaseApp.initializeApp(this)

            // Initialize Firebase App Check
            initializeFirebaseAppCheck()

            // Log device information for debugging
            TestDeviceHelper.logDeviceInfo()

            // Enable Samsung test device configuration
            TestDeviceHelper.initializeTestDevices(this)
            
            // Initialize Mobile Ads SDK
            initializeMobileAds()

            // Initialize Firebase Database persistence first
            initializeFirebaseDatabase()
            
            // Initialize Firebase Authentication
            initializeFirebaseAuth()
            
            // Schedule group cleanup task
            initializeGroupCleanup()
            
            Log.d("APP_INIT", "Application initialized successfully")
        } catch (e: Exception) {
            Log.e("AkahidegnApp", "Error during application initialization", e)
            // Don't crash the app, continue with basic functionality
        }
    }

    private fun initializeFirebaseAppCheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                Log.d("APP_INIT", "Initializing App Check with DEBUG provider")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                Log.d("APP_INIT", "Initializing App Check with PLAY INTEGRITY provider")
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
            Log.d("APP_INIT", "Firebase App Check initialization attempted")
        } catch (e: Exception) {
            Log.e("APP_INIT", "Firebase App Check initialization failed: ${e.message}")
        }
    }
    
    private fun initializeMobileAds() {
        try {
            MobileAds.initialize(this) { initializationStatus ->
                Log.d("APP_INIT", "Mobile Ads SDK initialized: $initializationStatus")
                
                // Log initialization status for each ad network
                initializationStatus.adapterStatusMap.forEach { (adapterClass, status) ->
                    Log.d("APP_INIT", "Adapter $adapterClass: ${status.initializationState} - ${status.description}")
                }
            }
            Log.d("APP_INIT", "Mobile Ads SDK initialization started")
        } catch (e: Exception) {
            Log.e("APP_INIT", "Mobile Ads SDK initialization failed: ${e.message}")
        }
    }
    
    private fun initializeFirebaseDatabase() {
        try {
            // Correct project URL (previously had a typo 'akahiyaj' vs 'akahidegn')
            val correctDbUrl = "https://akahidegn-79376-default-rtdb.europe-west1.firebasedatabase.app"
            // FirebaseDatabase.getInstance(correctDbUrl).setPersistenceEnabled(true) // Line removed
            FirebaseDatabase.getInstance(correctDbUrl).setLogLevel(com.google.firebase.database.Logger.Level.DEBUG)
            Log.d("APP_INIT", "Firebase Database initialized with URL=$correctDbUrl (DEBUG logging)") // Log message updated
        } catch (e: Exception) {
            Log.w("APP_INIT", "Firebase database setup failed: ${e.message}") // Log message updated
        }
    }
    
    private fun initializeFirebaseAuth() {
        try {
            Firebase.auth
            Log.d("APP_INIT", "Firebase Authentication initialized")
        } catch (e: Exception) {
            Log.e("APP_INIT", "Firebase Authentication initialization failed: ${e.message}")
        }
    }
    
    private fun initializeGroupCleanup() {
        try {
            groupCleanupScheduler.scheduleGroupCleanup()
            Log.d("APP_INIT", "Group cleanup scheduler initialized")
        } catch (e: Exception) {
            Log.e("APP_INIT", "Group cleanup scheduler initialization failed: ${e.message}")
        }
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // Add GIF decoder for animated WebP support
                add(GifDecoder.Factory())
                // Add ImageDecoder for API 28+ (Android 9+) devices for better WebP support
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                }
                // Add SVG support if needed
                add(SvgDecoder.Factory())
            }
            .respectCacheHeaders(false)
            .logger(DebugLogger()) // Enable debug logging
            .build()
    }
}
