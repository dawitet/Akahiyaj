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

# Compose rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Retrofit and OkHttp rules
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Room database rules
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

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