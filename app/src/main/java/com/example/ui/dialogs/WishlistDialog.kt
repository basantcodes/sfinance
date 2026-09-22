package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.local.entities.WishlistItem
import com.example.data.nepali.NepaliDateConverter
import com.example.ui.components.FormFeedbackMessage
import com.example.ui.components.NepaliDatePickerDialog

@Composable
fun WishlistDialog(
    itemToEdit: WishlistItem? = null,
    currency: String,
    onDismissRequest: () -> Unit,
    onSaveItem: (
        name: String,
        cost: Double,
        priority: String,
        preferredDate: Long?,
        category: String?,
        notes: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var costStr by remember { mutableStateOf(itemToEdit?.estimatedCost?.toString() ?: "") }
    var priority by remember { mutableStateOf(itemToEdit?.priority ?: "MEDIUM") }
    var preferredDate by remember { mutableStateOf(itemToEdit?.preferredDate) }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }

    var priorityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.94f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        title = {
            Text(
                text = if (itemToEdit == null) "New Wishlist Item" else "Edit Wishlist Item",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Estimated Cost ($currency)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { priorityExpanded = true }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Priority", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(priority, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    DropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    priority = p
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }

                // Target Date Card
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Target Date (Dual AD/BS)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = preferredDate?.let { NepaliDateConverter.formatDualDate(it) } ?: "No target date set",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Gadgets, Travel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Specifications") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    FormFeedbackMessage(message = it, isError = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Item name is required"
                        return@Button
                    }
                    val cost = costStr.toDoubleOrNull()
                    if (cost == null || cost <= 0) {
                        errorMessage = "Please enter a valid estimated cost"
                        return@Button
                    }
                    onSaveItem(name.trim(), cost, priority, preferredDate, category.ifBlank { null }, notes.ifBlank { null })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        NepaliDatePickerDialog(
            initialDateMillis = preferredDate ?: System.currentTimeMillis(),
            onDateSelected = { millis, _ ->
                preferredDate = millis
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}
