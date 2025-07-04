package com.dawitf.akahidegn

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import com.dawitf.akahidegn.ui.theme.FontSize

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // Use the enhanced theme with explicit parameters
            com.dawitf.akahidegn.ui.theme.AkahidegnTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                SplashScreen()
            }
        }
        
        // Navigate to MainActivity after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo at top
            Image(
                painter = painterResource(id = R.drawable.akahidegn_splash_logo),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Car image - much bigger and prominently centered
            Image(
                painter = painterResource(id = R.drawable.car_rideshare),
                contentDescription = "Car with passengers",
                modifier = Modifier
                    .size(300.dp)
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Splash text at bottom
            Text(
                text = stringResource(id = R.string.splash_screen_text),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
