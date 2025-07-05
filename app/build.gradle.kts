plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Removed kapt plugin - using KSP instead
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.dawitf.akahidegn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dawitf.akahidegn"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.0-compatible"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Production optimization
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("release-key.keystore")
            storePassword = "android123"
            keyAlias = "akahidegn"
            keyPassword = "android123"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            // Removed applicationIdSuffix to avoid Google Services configuration issues
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            manifestPlaceholders["crashlyticsCollectionEnabled"] = false
            
            // Test AdMob IDs for development
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("Boolean", "ADS_ENABLED", "true") // Enable test ads for debugging
            buildConfigField("Boolean", "DEBUG_LOGGING", "true")
        }
        
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
            
            // Disable ads in production until you get real AdMob IDs
            buildConfigField("String", "ADMOB_APP_ID", "\"\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"\"")
            buildConfigField("Boolean", "ADS_ENABLED", "false")
            buildConfigField("Boolean", "DEBUG_LOGGING", "false")
            
            // Production optimizations
            ndk {
                debugSymbolLevel = "FULL"
            }
            
            // Optimize R8 processing to reduce memory usage
            kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=all")
        }
        
        // Staging variant removed to avoid Google Services configuration issues
        // Re-enable if needed with proper Google Services configuration
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xjvm-default=all"
        )
        // JVM arguments should be set separately for kapt
    }
    buildFeatures {
        compose = true
        buildConfig = true
        // viewBinding = true // Not strictly needed for this SplashActivity, but good for other XML layouts
    }
    // If you are using libs.versions.toml, ensure versions for appcompat and constraintlayout are defined there.
    // Otherwise, you can specify versions directly.

    // Add lint configuration to disable problematic checks
    lint {
        disable += listOf("SuspiciousModifierThen")
        // Abort on error
        abortOnError = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Dependencies needed for the custom SplashActivity.kt and activity_splash.xml
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Your existing dependencies
    implementation(libs.androidx.lifecycle.runtime.ktx) // Now using version catalog
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended") // For additional Material Icons
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.hilt.work) // Ensure this is from your libs or specify version
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.code.gson:gson:2.10.1")


    implementation(platform(libs.firebase.bom)) // Updated to latest BOM version
    implementation("com.google.firebase:firebase-analytics-ktx") // Use -ktx version
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-installations-ktx") // Use -ktx version
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.android.gms:play-services-location:21.1.0") // Latest stable version
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.3") // Maps Compose
    implementation("com.google.android.gms:play-services-ads:22.6.0") // Keep for interstitial and rewarded ads
    
    // Hilt for Dependency Injection - simplified for Ethiopian market
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)  // Changed from kapt to ksp
    implementation(libs.androidx.hilt.work)
    implementation(libs.hilt.navigation.compose)
    // Removed complex hilt lifecycle viewmodel - using simpler approach
    ksp("androidx.hilt:hilt-compiler:1.1.0")  // Changed from kapt to ksp

    // Paging for performance
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    
    // Room for local database with better performance
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler) // Changed from kapt to ksp

    // Retrofit for better networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    
    // DataStore for secure preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    // ksp(libs.hilt.compiler) // Use ksp instead of kapt

    // Advanced UI/UX Dependencies
    implementation("androidx.compose.animation:animation:1.5.4")
    implementation("androidx.compose.animation:animation-graphics:1.5.4")
    implementation("com.google.accompanist:accompanist-placeholder-material:0.32.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")

    // Add dependencies for WorkManager testing
    testImplementation(libs.androidx.work.testing)
    testImplementation("androidx.test:core:1.6.1") // Updated
    // Note: Using native Firestore geo-queries instead of GeoFirestore for simplicity
}
