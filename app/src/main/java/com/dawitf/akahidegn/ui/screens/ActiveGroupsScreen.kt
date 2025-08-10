package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.ui.components.ActiveGroupsTabLayout

@Composable
fun ActiveGroupsScreen() {
    ActiveGroupsTabLayout(
        headerContent = {
            Text(
                text = "Active Groups",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        },
        mainContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ይህ ገጽ የእርስዎን ንቁ ቡድኖች ያሳያል. በቅርቡ ይመጣል!",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    )
}
