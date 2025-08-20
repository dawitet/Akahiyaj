package com.dawitf.akahidegn.debug

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dawitf.akahidegn.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Debug activity to test AdMob integration
 * Only available in debug builds
 */
class AdTestActivity : AppCompatActivity() {
    
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple layout programmatically
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Status text
        statusText = TextView(this).apply {
            text = "Initializing AdMob Test...\n"
        }
        layout.addView(statusText)
        
        // Banner ad
        val bannerAd = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER_ID
        }
        layout.addView(bannerAd)
        
        // Load Rewarded Ad button
        val loadRewardedButton = Button(this).apply {
            text = "Load Rewarded Ad"
            setOnClickListener { loadRewardedAd() }
        }
        layout.addView(loadRewardedButton)
        
        // Show Rewarded Ad button
        val showRewardedButton = Button(this).apply {
            text = "Show Rewarded Ad"
            setOnClickListener { showRewardedAd() }
        }
        layout.addView(showRewardedButton)
        
        // Load Interstitial Ad button
        val loadInterstitialButton = Button(this).apply {
            text = "Load Interstitial Ad"
            setOnClickListener { loadInterstitialAd() }
        }
        layout.addView(loadInterstitialButton)
        
        // Show Interstitial Ad button
        val showInterstitialButton = Button(this).apply {
            text = "Show Interstitial Ad"
            setOnClickListener { showInterstitialAd() }
        }
        layout.addView(showInterstitialButton)
        
        setContentView(layout)
        
        // Initialize and test ads
        initializeAds(bannerAd)
    }
    
    private fun initializeAds(bannerAd: AdView) {
        appendStatus("Ads Enabled: ${BuildConfig.ADS_ENABLED}")
        appendStatus("Banner ID: ${BuildConfig.ADMOB_BANNER_ID}")
        appendStatus("Interstitial ID: ${BuildConfig.ADMOB_INTERSTITIAL_ID}")
        appendStatus("Rewarded ID: ${BuildConfig.ADMOB_REWARDED_ID}")
        appendStatus("App ID: ${BuildConfig.ADMOB_APP_ID}")
        
        if (!BuildConfig.ADS_ENABLED) {
            appendStatus("ERROR: Ads are disabled in build configuration!")
            return
        }
        
        // Initialize Mobile Ads
        MobileAds.initialize(this) { initializationStatus ->
            appendStatus("Mobile Ads initialized!")
            
            // Log adapter statuses
            initializationStatus.adapterStatusMap.forEach { (adapter, status) ->
                appendStatus("$adapter: ${status.initializationState}")
            }
            
            // Load banner ad
            loadBannerAd(bannerAd)
        }
    }
    
    private fun loadBannerAd(bannerAd: AdView) {
        appendStatus("Loading banner ad...")
        val adRequest = AdRequest.Builder().build()
        bannerAd.loadAd(adRequest)
        
        bannerAd.adListener = object : com.google.android.gms.ads.AdListener() {
            override fun onAdLoaded() {
                appendStatus("✅ Banner ad loaded successfully!")
            }
            
            override fun onAdFailedToLoad(error: LoadAdError) {
                appendStatus("❌ Banner ad failed to load: ${error.message}")
                appendStatus("Error code: ${error.code}, Domain: ${error.domain}")
            }
        }
    }
    
    private fun loadRewardedAd() {
        appendStatus("Loading rewarded ad...")
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(this, BuildConfig.ADMOB_REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                appendStatus("✅ Rewarded ad loaded successfully!")
                rewardedAd = ad
            }
            
            override fun onAdFailedToLoad(error: LoadAdError) {
                appendStatus("❌ Rewarded ad failed to load: ${error.message}")
                appendStatus("Error code: ${error.code}, Domain: ${error.domain}")
                rewardedAd = null
            }
        })
    }
    
    private fun showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd!!.show(this) { reward ->
                appendStatus("🎉 Rewarded ad completed! Reward: ${reward.amount} ${reward.type}")
            }
        } else {
            appendStatus("❌ No rewarded ad available to show")
        }
    }
    
    private fun loadInterstitialAd() {
        appendStatus("Loading interstitial ad...")
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(this, BuildConfig.ADMOB_INTERSTITIAL_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                appendStatus("✅ Interstitial ad loaded successfully!")
                interstitialAd = ad
            }
            
            override fun onAdFailedToLoad(error: LoadAdError) {
                appendStatus("❌ Interstitial ad failed to load: ${error.message}")
                appendStatus("Error code: ${error.code}, Domain: ${error.domain}")
                interstitialAd = null
            }
        })
    }
    
    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd!!.show(this)
            appendStatus("📱 Interstitial ad shown!")
        } else {
            appendStatus("❌ No interstitial ad available to show")
        }
    }
    
    private fun appendStatus(message: String) {
        Log.d("AdTest", message)
        runOnUiThread {
            statusText.text = statusText.text.toString() + message + "\n"
        }
    }
}
