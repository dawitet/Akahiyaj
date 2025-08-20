package com.dawitf.akahidegn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.dawitf.akahidegn.ui.theme.AkahidegnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EnhancedSplashActivity : ComponentActivity() {
    companion object {
        const val EXTRA_GOOGLE_ID_TOKEN = "google_id_token"
        const val EXTRA_IS_FIRST_TIME_USER = "is_first_time_user"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AkahidegnTheme {
                EnhancedSplashScreen()
            }
        }
        
        // Navigate to MainActivity after 3 seconds
        lifecycleScope.launch {
            delay(3000)
            startActivity(Intent(this@EnhancedSplashActivity, MainActivity::class.java))
            finish()
        }
    }
}

@Composable
fun EnhancedSplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Akahidegn",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enhanced",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
