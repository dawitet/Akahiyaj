package com.dawitf.akahidegn.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRegistrationDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String) -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var isUserNameError by remember { mutableStateOf(false) }
    var isUserPhoneError by remember { mutableStateOf(false) } 
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Complete Your Profile", style = MaterialTheme.typography.headlineSmall)
                Text("Please provide your name and phone number to continue.", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        isUserNameError = it.isBlank()
                    },
                    label = { Text("Your Name") },
                    isError = isUserNameError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isUserNameError) {
                    Text("Name cannot be empty.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = userPhone,
                    onValueChange = {
                        userPhone = it
                        isUserPhoneError = it.isBlank() // Basic validation, consider more robust phone validation
                    },
                    label = { Text("Your Phone Number") },
                    isError = isUserPhoneError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        if (userName.isNotBlank() && userPhone.isNotBlank()) {
                            onRegister(userName, userPhone)
                        } else {
                            isUserNameError = userName.isBlank()
                            isUserPhoneError = userPhone.isBlank()
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isUserPhoneError) {
                    Text("Phone number cannot be empty.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isUserNameError = userName.isBlank()
                            isUserPhoneError = userPhone.isBlank()
                            if (!isUserNameError && !isUserPhoneError) {
                                onRegister(userName, userPhone)
                            }
                        },
                        enabled = userName.isNotBlank() && userPhone.isNotBlank()
                    ) {
                        Text("Register")
                    }
                }
            }
        }
    }
}
