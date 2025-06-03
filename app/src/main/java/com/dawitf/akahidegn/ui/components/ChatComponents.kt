package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.ChatMessage
import com.dawitf.akahidegn.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageItem(message: ChatMessage, isCurrentUser: Boolean) {
    // Cache the formatted time to prevent recreation on every recomposition
    val formattedTime = remember(message.timestamp) {
        message.timestamp?.let { 
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
        } ?: ""
    }
    
    // Cache the sender name to prevent unnecessary operations
    val senderDisplayName = remember(message.senderName) {
        message.senderName ?: "Unknown Sender"
    }
    
    // Cache the corner radius values to prevent recreation
    val cornerRadius = remember(isCurrentUser) {
        RoundedCornerShape( // Different corners for sender/receiver
            topStart = if (isCurrentUser) 16.dp else 4.dp,
            topEnd = if (isCurrentUser) 4.dp else 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(cornerRadius)
                .background(
                    if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (!isCurrentUser) { // Show sender name if not current user
                    Text(
                        text = senderDisplayName,
                        style = MaterialTheme.typography.labelMedium, // একটু বড়ো করলাম
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp)) // অল্প ফাঁক
                }
                Text(
                    text = message.text ?: "",
                    style = MaterialTheme.typography.bodyLarge, // একটু বড়ো করলাম
                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}