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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dawitf.akahidegn.R

@Composable
fun UserRegistrationDialog(
    initialName: String = "",
    initialPhotoUrl: String? = null, // Added initialPhotoUrl parameter
    onComplete: (name: String, phone: String) -> Unit, // Avatar no longer passed from here
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
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
                Text(
                    text = "🎉 እንኳን ወደ አካሂያጅ በደህና መጡ!",
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

                // Display Google Photo URL or Fallback
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(initialPhotoUrl)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.default_avatar), // Your default avatar
                    error = painterResource(R.drawable.default_avatar), // Fallback if error or no URL
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        val cleanedValue = newValue.replace(Regex("[^0-9+]"), "")
                        when {
                            cleanedValue.matches(Regex("^0[0-9]{0,9}$")) -> phone = cleanedValue
                            cleanedValue.startsWith("+2519") -> phone = "0" + cleanedValue.substring(5)
                            cleanedValue.startsWith("2519") -> phone = "0" + cleanedValue.substring(4)
                            cleanedValue == "+" || cleanedValue == "+2" || cleanedValue == "+25" || cleanedValue == "+251" -> phone = cleanedValue
                            cleanedValue.length <= 10 && cleanedValue.all { it.isDigit() || it == '+' } -> phone = cleanedValue
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
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onComplete(name.trim(), phone.trim())
                            }
                        }
                    ),
                    placeholder = { Text("0912345678") },
                    supportingText = { 
                        Text(
                            text = "ኢትዮጵያዊ ቁጥር: 0912345678 ወይም +251912345678",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onComplete(name.trim(), phone.trim())
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

// AvatarOption composable is no longer needed and can be removed if not used elsewhere.
// @Composable
// private fun AvatarOption(...) { ... }
