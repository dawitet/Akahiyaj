package com.dawitf.akahidegn // Make sure this matches your package name

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.android.gms.ads.MobileAds

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        MobileAds.initialize(this) {} // Initialize AdMob
    }
}