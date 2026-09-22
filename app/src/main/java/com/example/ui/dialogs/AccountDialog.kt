package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.Account
import com.example.data.local.entities.AccountType
import com.example.ui.components.FormFeedbackMessage

@Composable
fun AccountDialog(
    accountToEdit: Account? = null,
    defaultCurrency: String = "NPR",
    onDismissRequest: () -> Unit,
    onSaveAccount: (name: String, type: AccountType, balance: Double, color: String, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var selectedType by remember {
        mutableStateOf(
            accountToEdit?.type?.let { runCatching { AccountType.valueOf(it) }.getOrNull() } ?: AccountType.BANK
        )
    }
    var balanceStr by remember { mutableStateOf(accountToEdit?.balance?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(accountToEdit?.notes ?: "") }

    val colors = listOf(
        "#6366f1", "#10b981", "#059669", "#3b82f6", "#0ea5e9",
        "#8b5cf6", "#ec4899", "#f59e0b", "#ef4444", "#64748b"
    )
    var selectedColor by remember { mutableStateOf(accountToEdit?.color ?: colors[0]) }

    var typeMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.94f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        title = {
            Text(
                text = if (accountToEdit == null) "New Account" else "Edit Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Account Type Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeMenuExpanded = true }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Account Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(text = selectedType.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Balance
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Balance ($defaultCurrency)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accountToEdit == null // In edit mode, balances are updated via transactions
                )

                // Color Picker Row
                Text(text = "Account Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.take(7).forEach { hex ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        val parsedColor = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
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
                        errorMessage = "Account name is required"
                        return@Button
                    }
                    val balance = balanceStr.toDoubleOrNull()
                    if (balance == null || balance < 0) {
                        errorMessage = "Initial balance cannot be negative"
                        return@Button
                    }

                    onSaveAccount(
                        name.trim(),
                        selectedType,
                        balance,
                        selectedColor,
                        notes.ifBlank { null }
                    )
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
}
