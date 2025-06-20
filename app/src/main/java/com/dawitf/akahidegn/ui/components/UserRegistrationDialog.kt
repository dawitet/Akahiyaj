package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dawitf.akahidegn.R

@Composable
fun UserRegistrationDialog(
    onComplete: (name: String, phone: String, avatar: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("avatar_1") }
    val focusManager = LocalFocusManager.current
    
    // Available avatars from drawable resources
    val avatars = listOf<Pair<String, Int>>(
        "avatar_1" to R.drawable.user_avatar_1,
        "avatar_2" to R.drawable.user_avatar_2,
        "avatar_3" to R.drawable.user_avatar_3,
        "avatar_4" to R.drawable.user_avatar_4,
        "avatar_5" to R.drawable.user_avatar_5,
        "avatar_6" to R.drawable.default_avatar
    )

    Dialog(
        onDismissRequest = { /* Cannot dismiss - required registration */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "🎉 እንኳን ወደ ኣካሂደኛ መጡ!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "እባክዎ ለመጀመር የእርስዎን መገለጫ ያጠናቅቁ",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Avatar Selection
                Text(
                    text = "👤 የእርስዎን አቫታር ይምረጡ:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(avatars) { (avatarId, drawableRes) ->
                        AvatarOption(
                            drawableRes = drawableRes,
                            isSelected = selectedAvatar == avatarId,
                            onClick = { selectedAvatar = avatarId }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("📝 የእርስዎ ስም") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() })
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Phone Input
                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        // Ethiopian phone number validation - prioritize local format (0911000927)
                        val cleanedValue = newValue.replace(Regex("[^0-9+]"), "")
                        
                        when {
                            // Allow Ethiopian local format (starts with 09)
                            cleanedValue.matches(Regex("^0[0-9]{0,9}$")) -> {
                                phone = cleanedValue
                            }
                            // Convert +251 format to local format
                            cleanedValue.startsWith("+2519") -> {
                                phone = "0" + cleanedValue.substring(5)
                            }
                            cleanedValue.startsWith("2519") -> {
                                phone = "0" + cleanedValue.substring(4)
                            }
                            // Allow typing + at the start temporarily
                            cleanedValue == "+" || cleanedValue == "+2" || cleanedValue == "+25" || cleanedValue == "+251" -> {
                                phone = cleanedValue
                            }
                            // For other inputs, only allow if it's a valid progression
                            cleanedValue.length <= 10 && cleanedValue.all { it.isDigit() || it == '+' } -> {
                                phone = cleanedValue
                            }
                        }
                    },
                    label = { Text("📞 የስልክ ቁጥር") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    placeholder = { Text("0911000927") },
                    supportingText = { 
                        Text(
                            text = "ኢትዮጵያዊ ቁጥር: 0911000927 ወይም +251911000927",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Continue Button
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onComplete(name.trim(), phone.trim(), selectedAvatar)
                        }
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "✨ ቀጥል",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarOption(
    drawableRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Avatar option",
            modifier = Modifier.size(48.dp)
        )
    }
}
