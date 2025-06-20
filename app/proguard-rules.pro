# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Production ProGuard Rules for Akahiyaj

# Keep line numbers for production debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# R8 optimization configuration - more conservative to avoid XML parsing issues
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 3
-allowaccessmodification
-dontpreverify

# Android support library
-keep class androidx.** { *; }
-dontwarn androidx.**

# XML parsing and reflection safety
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Firebase rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Gson rules for JSON serialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes used with Firebase
-keep class com.dawitf.akahidegn.data.** { *; }
-keep class com.dawitf.akahidegn.ChatMessage { *; }
-keep class com.dawitf.akahidegn.Group { *; }

# Hilt rules
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-dontwarn dagger.hilt.**

# Compose rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Retrofit and OkHttp rules
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# AdMob rules
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Room database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep manifest components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-dontwarn okhttp3.**

# Room database rules
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Memory optimization for R8
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Additional rule to prevent R8 from running out of memory
-dontpreverify

# Keep crash reporting information
-keepattributes LineNumberTable,SourceFile
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Location services
-keep class com.google.android.gms.location.** { *; }

# Ads
-keep class com.google.android.gms.ads.** { *; }

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile