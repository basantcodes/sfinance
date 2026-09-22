package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import com.example.data.local.entities.Category
import com.example.data.local.entities.CategoryType
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TransactionType
import com.example.data.nepali.NepaliDateConverter
import com.example.ui.components.NepaliDatePickerDialog
import com.example.ui.components.FormFeedbackMessage

@Composable
fun TransactionDialog(
    transactionToEdit: TransactionEntity? = null,
    accounts: List<Account>,
    categories: List<Category>,
    currency: String,
    onDismissRequest: () -> Unit,
    onSaveTransaction: (
        type: TransactionType,
        amount: Double,
        date: Long,
        dateBs: String,
        name: String?,
        notes: String?,
        fromAccountId: String?,
        toAccountId: String?,
        categoryId: String?
        ,feeAmount: Double
    ) -> Unit,
    onQuickCreateCategory: (name: String, type: CategoryType) -> Unit
) {
    val initialType = transactionToEdit?.type?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
        ?: TransactionType.EXPENSE

    var selectedTypeTab by remember {
        mutableIntStateOf(
            when (initialType) {
                TransactionType.EXPENSE -> 0
                TransactionType.INCOME -> 1
                TransactionType.TRANSFER -> 2
                else -> 0
            }
        )
    }

    val currentType = when (selectedTypeTab) {
        0 -> TransactionType.EXPENSE
        1 -> TransactionType.INCOME
        else -> TransactionType.TRANSFER
    }

    var amountStr by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var nameStr by remember { mutableStateOf(transactionToEdit?.name ?: "") }
    var notesStr by remember { mutableStateOf(transactionToEdit?.notes ?: "") }
    var feeStr by remember { mutableStateOf(transactionToEdit?.feeAmount?.takeIf { it > 0 }?.toString() ?: "") }

    var selectedDateMillis by remember { mutableLongStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }
    var selectedDateBs by remember {
        mutableStateOf(
            transactionToEdit?.dateBs ?: NepaliDateConverter.adToBs(selectedDateMillis).formatted
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedFromAccountId by remember {
        mutableStateOf(transactionToEdit?.accountFromId ?: accounts.firstOrNull()?.id)
    }
    var selectedToAccountId by remember {
        mutableStateOf(
            transactionToEdit?.accountToId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id
        )
    }

    val filteredCategories = categories.filter {
        if (currentType == TransactionType.INCOME) it.type == CategoryType.INCOME.name
        else it.type == CategoryType.EXPENSE.name
    }

    var selectedCategoryId by remember(currentType) {
        mutableStateOf(
            transactionToEdit?.categoryId ?: filteredCategories.firstOrNull()?.id
        )
    }

    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.94f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        title = {
            Text(
                text = if (transactionToEdit == null) "New Transaction" else "Edit Transaction",
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
                // Type Selector Tabs
                TabRow(selectedTabIndex = selectedTypeTab) {
                    Tab(
                        selected = selectedTypeTab == 0,
                        onClick = { selectedTypeTab = 0 },
                        text = { Text("Expense") }
                    )
                    Tab(
                        selected = selectedTypeTab == 1,
                        onClick = { selectedTypeTab = 1 },
                        text = { Text("Income") }
                    )
                    Tab(
                        selected = selectedTypeTab == 2,
                        onClick = { selectedTypeTab = 2 },
                        text = { Text("Transfer") }
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($currency)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Name / Description
                OutlinedTextField(
                    value = nameStr,
                    onValueChange = { nameStr = it },
                    label = { Text("Description / Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Dual Nepali Date Picker Trigger
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Date (Dual AD / BS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = NepaliDateConverter.formatDualDate(selectedDateMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Dynamic Account Dropdowns based on transaction type
                when (currentType) {
                    TransactionType.EXPENSE -> {
                        AccountPicker(
                            label = "Debit From Account*",
                            accounts = accounts,
                            selectedId = selectedFromAccountId,
                            onSelect = { selectedFromAccountId = it }
                        )
                    }
                    TransactionType.INCOME -> {
                        AccountPicker(
                            label = "Credit To Account*",
                            accounts = accounts,
                            selectedId = selectedToAccountId,
                            onSelect = { selectedToAccountId = it }
                        )
                    }
                    TransactionType.TRANSFER -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Balance transfer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "The destination receives the transfer amount. Any fee is charged separately to the source account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        AccountPicker(
                            label = "Source (From Account)*",
                            accounts = accounts,
                            selectedId = selectedFromAccountId,
                            onSelect = { selectedFromAccountId = it }
                        )
                        AccountPicker(
                            label = "Destination (To Account)*",
                            accounts = accounts,
                            selectedId = selectedToAccountId,
                            onSelect = { selectedToAccountId = it }
                        )
                        OutlinedTextField(
                            value = feeStr,
                            onValueChange = { feeStr = it },
                            label = { Text("Transfer Fee ($currency, optional)") },
                            supportingText = { Text("Paid by the source account") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val previewAmount = amountStr.toDoubleOrNull() ?: 0.0
                        val previewFee = feeStr.toDoubleOrNull() ?: 0.0
                        if (previewAmount > 0 && previewFee >= 0) {
                            Text(
                                "Source account deducted: $currency ${String.format("%,.2f", previewAmount + previewFee)} | Destination receives: $currency ${String.format("%,.2f", previewAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    else -> Unit
                }

                // Category (for Expense & Income)
                if (currentType != TransactionType.TRANSFER) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryPicker(
                                categories = filteredCategories,
                                selectedId = selectedCategoryId,
                                onSelect = { selectedCategoryId = it }
                            )
                        }
                        IconButton(onClick = { showCreateCategoryDialog = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notesStr,
                    onValueChange = { notesStr = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                errorMessage?.let { msg ->
                    FormFeedbackMessage(message = msg, isError = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid positive amount"
                        return@Button
                    }
                    val fee = if (currentType == TransactionType.TRANSFER) feeStr.toDoubleOrNull() ?: 0.0 else 0.0
                    if (!fee.isFinite() || fee < 0) {
                        errorMessage = "Please enter a valid non-negative transfer fee"
                        return@Button
                    }
                    if (currentType == TransactionType.EXPENSE && selectedFromAccountId == null) {
                        errorMessage = "Please select an account to debit from"
                        return@Button
                    }
                    if (currentType == TransactionType.INCOME && selectedToAccountId == null) {
                        errorMessage = "Please select an account to credit to"
                        return@Button
                    }
                    if (currentType == TransactionType.TRANSFER) {
                        if (selectedFromAccountId == null || selectedToAccountId == null) {
                            errorMessage = "Please select both source and destination accounts"
                            return@Button
                        }
                        if (selectedFromAccountId == selectedToAccountId) {
                            errorMessage = "Source and destination cannot be the same"
                            return@Button
                        }
                        if (fee >= amount) {
                            errorMessage = "Transfer fee should be smaller than the transfer amount"
                            return@Button
                        }
                    }

                    onSaveTransaction(
                        currentType,
                        amount,
                        selectedDateMillis,
                        selectedDateBs,
                        nameStr.ifBlank { null },
                        notesStr.ifBlank { null },
                        if (currentType == TransactionType.EXPENSE || currentType == TransactionType.TRANSFER) selectedFromAccountId else null,
                        if (currentType == TransactionType.INCOME || currentType == TransactionType.TRANSFER) selectedToAccountId else null,
                        if (currentType != TransactionType.TRANSFER) selectedCategoryId else null
                        ,fee
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
            initialDateMillis = selectedDateMillis,
            onDateSelected = { millis, bs ->
                selectedDateMillis = millis
                selectedDateBs = bs
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }

    if (showCreateCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            val catType = if (currentType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                            onQuickCreateCategory(newCategoryName, catType)
                            showCreateCategoryDialog = false
                            newCategoryName = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AccountPicker(
    label: String,
    accounts: List<Account>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.firstOrNull { it.id == selectedId }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = selectedAccount?.let { "${it.name} (${it.currency} ${String.format("%.2f", it.balance)})" } ?: "Select Account",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(acc.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Bal: ${acc.currency} ${String.format("%.2f", acc.balance)} • ${acc.type}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(acc.id)
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryPicker(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == selectedId }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = selectedCategory?.name ?: "Select Category",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = {
                        expanded = false
                        onSelect(cat.id)
                    }
                )
            }
        }
    }
}
