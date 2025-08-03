package com.dawitf.akahidegn.ui.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dawitf.akahidegn.domain.model.SearchFilters

import com.dawitf.akahidegn.domain.model.SortOption
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    var currentFilters by remember { mutableStateOf(filters) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ፍተሻ ማጣሪያዎች",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Filter content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Distance filter
                    FilterSection(title = "ከእኔ የሚሻለው ርቀት") {
                        DistanceFilter(
                            maxDistance = currentFilters.maxDistance,
                            onDistanceChange = { distance ->
                                currentFilters = currentFilters.copy(maxDistance = distance)
                            }
                        )
                    }
                    
                    
                    
                    // Members filter
                    FilterSection(title = "የቡድን ወሰን") {
                        MembersFilter(
                            maxMembers = currentFilters.maxMembers,
                            onMaxMembersChange = { maxMembers ->
                                currentFilters = currentFilters.copy(maxMembers = maxMembers)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Available seats only
                    FilterSection(title = "ተጨማሪ አማራጮች") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = currentFilters.availableSeatsOnly,
                                onCheckedChange = { checked ->
                                    currentFilters = currentFilters.copy(availableSeatsOnly = checked)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ነፃ መቀመጫ ያላቸው ብቻ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sort options
                    FilterSection(title = "ደርደር በ") {
                        SortOptionsFilter(
                            sortBy = currentFilters.sortBy,
                            onSortByChange = { sortBy ->
                                currentFilters = currentFilters.copy(sortBy = sortBy)
                            }
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentFilters = SearchFilters()
                            onClear()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("አጽዳ")
                    }
                    
                    Button(
                        onClick = {
                            onFiltersChange(currentFilters)
                            onApply()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ተግብር")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun DistanceFilter(
    maxDistance: Double,
    onDistanceChange: (Double) -> Unit
) {
    Column {
        Text(
            text = "${maxDistance.toInt()} ኪሎ ሚተር",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Slider(
            value = maxDistance.toFloat(),
            onValueChange = { value -> onDistanceChange(value.toDouble()) },
            valueRange = 1f..50f,
            steps = 49,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1 ኪሜ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "50 ኪሜ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}





@Composable
private fun MembersFilter(
    maxMembers: Int?,
    onMaxMembersChange: (Int?) -> Unit
) {
    var memberCount by remember { mutableStateOf(maxMembers?.toString() ?: "") }
    
    OutlinedTextField(
        value = memberCount,
        onValueChange = { value ->
            memberCount = value
            val members = value.toIntOrNull()
            onMaxMembersChange(members)
        },
        label = { Text("ከፍተኛ የአባላት ቁጥር") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun SortOptionsFilter(
    sortBy: SortOption,
    onSortByChange: (SortOption) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        SortOption.values().forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (option == sortBy),
                        onClick = { onSortByChange(option) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == sortBy),
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
