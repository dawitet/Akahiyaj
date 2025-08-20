plugins {
    id("com.android.test")
    kotlin("android")
}

android {
    namespace = "com.dawitf.akahidegn.benchmark"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Use the default debug build for the test APK; app under test can be release/profileable

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.2.4")
    
    // Phase 5: Baseline Profile Generation Dependencies
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("androidx.benchmark:benchmark-macro:1.2.4")
}
