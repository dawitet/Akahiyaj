package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActiveGroupsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("የእርስዎ ንቁ ቡድኖች") }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ይህ ገጽ የእርስዎን ንቁ ቡድኖች ያሳያል. በቅርቡ ይመጣል!",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
