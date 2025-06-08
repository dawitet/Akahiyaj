plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Assuming this is for Jetpack Compose
    id("com.google.gms.google-services")
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.dawitf.akahidegn"
    compileSdk = 35 // Or your preferred SDK, 34 is latest stable, 35 is beta

    defaultConfig {
        applicationId = "com.dawitf.akahidegn"
        minSdk = 26
        targetSdk = 35 // Match compileSdk if using 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { // Important for vector drawables used in splash
            useSupportLibrary = true
        }
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
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // Or JavaVersion.VERSION_1_8 if you have older libraries
        targetCompatibility = JavaVersion.VERSION_11 // Or JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "11" // Or "1.8"
    }
    buildFeatures {
        compose = true
        // viewBinding = true // Not strictly needed for this SplashActivity, but good for other XML layouts
    }
    // If you are using libs.versions.toml, ensure versions for appcompat and constraintlayout are defined there.
    // Otherwise, you can specify versions directly.
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Dependencies needed for the custom SplashActivity.kt and activity_splash.xml
    implementation("androidx.appcompat:appcompat:1.7.0") // Updated for security
    implementation("androidx.constraintlayout:constraintlayout:2.2.0") // Updated

    // Your existing dependencies
    implementation(libs.androidx.lifecycle.runtime.ktx) // Now using version catalog
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7") // Updated
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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0") // Updated
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.14.2") // Updated for security
    testImplementation("io.mockk:mockk:1.13.14") // Latest version
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("io.coil-kt:coil-compose:2.7.0") // Updated to available version
    implementation("com.google.code.gson:gson:2.11.0")


    implementation(platform(libs.firebase.bom)) // Updated to latest BOM version
    implementation("com.google.firebase:firebase-analytics-ktx") // Use -ktx version
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-installations-ktx") // Use -ktx version
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.android.gms:play-services-location:21.3.0") // Latest stable version
    implementation("com.google.android.gms:play-services-ads:23.6.0") // Updated
    
    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // Paging for performance
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    
    // Room for local database with better performance
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    kapt(libs.androidx.room.compiler)
    
    // Retrofit for better networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    
    // DataStore for secure preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1") // Updated
    
    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    kapt(libs.hilt.compiler)

    // Advanced UI/UX Dependencies
    implementation("androidx.compose.animation:animation:1.7.5") // Updated
    implementation("androidx.compose.animation:animation-graphics:1.7.5") // Updated
    implementation("com.google.accompanist:accompanist-placeholder-material:0.34.0") // Updated
    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0") // Updated
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0") // Updated
    implementation("androidx.compose.material:material-icons-core:1.7.5") // Updated
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.5") // Updated
    implementation("androidx.activity:activity-compose:1.9.3") // Updated
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1") // Updated

    // Add dependencies for WorkManager testing
    testImplementation(libs.androidx.work.testing)
    testImplementation("androidx.test:core:1.6.1") // Updated
}