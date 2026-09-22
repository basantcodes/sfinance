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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.Account
import com.example.data.local.entities.InterestFrequency
import com.example.data.local.entities.InterestMode
import com.example.data.local.entities.Loan
import com.example.data.local.entities.LoanType
import com.example.data.nepali.NepaliDateConverter
import com.example.ui.components.NepaliDatePickerDialog

@Composable
fun LoanDialog(
    loanToEdit: Loan? = null,
    accounts: List<Account>,
    currency: String,
    onDismissRequest: () -> Unit,
    onSaveLoan: (
        counterparty: String,
        type: LoanType,
        principal: Double,
        rate: Double,
        startDate: Long,
        accountId: String?,
        mode: InterestMode,
        frequency: InterestFrequency?,
        notes: String?
    ) -> Unit
) {
    var counterparty by remember { mutableStateOf(loanToEdit?.counterparty ?: "") }
    var selectedTypeTab by remember {
        mutableIntStateOf(if (loanToEdit?.type == LoanType.BORROW.name) 1 else 0)
    }
    val currentType = if (selectedTypeTab == 0) LoanType.LEND else LoanType.BORROW

    var principalStr by remember { mutableStateOf(loanToEdit?.principal?.toString() ?: "") }
    var rateStr by remember { mutableStateOf(loanToEdit?.interestRate?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(loanToEdit?.notes ?: "") }

    var selectedAccountId by remember { mutableStateOf(loanToEdit?.accountId ?: accounts.firstOrNull()?.id) }

    var interestMode by remember {
        mutableStateOf(
            loanToEdit?.interestMode?.let { runCatching { InterestMode.valueOf(it) }.getOrNull() } ?: InterestMode.MANUAL
        )
    }

    var interestFrequency by remember {
        mutableStateOf(
            loanToEdit?.interestFrequency?.let { runCatching { InterestFrequency.valueOf(it) }.getOrNull() } ?: InterestFrequency.MONTHLY
        )
    }

    var startDateMillis by remember { mutableLongStateOf(loanToEdit?.startDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var freqExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (loanToEdit == null) "New Loan Entry" else "Edit Loan",
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
                // Type Selector (Lend vs Borrow)
                TabRow(selectedTabIndex = selectedTypeTab) {
                    Tab(
                        selected = selectedTypeTab == 0,
                        onClick = { selectedTypeTab = 0 },
                        text = { Text("I Lent (Receivable)") }
                    )
                    Tab(
                        selected = selectedTypeTab == 1,
                        onClick = { selectedTypeTab = 1 },
                        text = { Text("I Borrowed (Payable)") }
                    )
                }

                // Counterparty
                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it },
                    label = { Text(if (currentType == LoanType.LEND) "Borrower Name (Lent to)*" else "Lender Name (Borrowed from)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Principal
                OutlinedTextField(
                    value = principalStr,
                    onValueChange = { principalStr = it },
                    label = { Text("Principal Amount ($currency)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Interest Rate
                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Annual Interest Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Account Selection
                AccountPicker(
                    label = if (currentType == LoanType.LEND) "Disburse from Account" else "Receive into Account",
                    accounts = accounts,
                    selectedId = selectedAccountId,
                    onSelect = { selectedAccountId = it }
                )

                // Interest Mode & Frequency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().clickable { modeExpanded = true }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(interestMode.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                            InterestMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.name) },
                                    onClick = {
                                        interestMode = mode
                                        modeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().clickable { freqExpanded = true }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Frequency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(interestFrequency.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        DropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                            InterestFrequency.values().forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq.name) },
                                    onClick = {
                                        interestFrequency = freq
                                        freqExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Date Picker Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(NepaliDateConverter.formatDualDate(startDateMillis), fontWeight = FontWeight.SemiBold)
                        }
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
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
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (counterparty.isBlank()) {
                        errorMessage = "Counterparty name is required"
                        return@Button
                    }
                    val principal = principalStr.toDoubleOrNull()
                    if (principal == null || principal <= 0) {
                        errorMessage = "Please enter a valid principal amount"
                        return@Button
                    }
                    val rate = rateStr.toDoubleOrNull() ?: 0.0

                    onSaveLoan(
                        counterparty.trim(),
                        currentType,
                        principal,
                        rate,
                        startDateMillis,
                        selectedAccountId,
                        interestMode,
                        interestFrequency,
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

    if (showDatePicker) {
        NepaliDatePickerDialog(
            initialDateMillis = startDateMillis,
            onDateSelected = { millis, _ ->
                startDateMillis = millis
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}
