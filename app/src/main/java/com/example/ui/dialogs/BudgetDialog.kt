package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Category
import com.example.data.local.entities.CategoryType
import com.example.ui.components.FormFeedbackMessage

@Composable
fun BudgetDialog(
    budgetToEdit: Budget? = null,
    categories: List<Category>,
    currency: String,
    onDismissRequest: () -> Unit,
    onSaveBudget: (categoryId: String, limit: Double, rollover: Boolean) -> Unit
) {
    val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE.name }
    var selectedCategoryId by remember {
        mutableStateOf(budgetToEdit?.categoryId ?: expenseCategories.firstOrNull()?.id ?: "")
    }
    var limitStr by remember { mutableStateOf(budgetToEdit?.monthlyLimit?.toString() ?: "") }
    var rollover by remember { mutableStateOf(budgetToEdit?.rollover ?: false) }

    var catMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val selectCategoryErrorMessage = stringResource(R.string.select_category_error)
    val validMonthlyLimitMessage = stringResource(R.string.valid_monthly_limit)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.94f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        title = {
            Text(
                text = stringResource(if (budgetToEdit == null) R.string.new_budget else R.string.edit_budget),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { catMenuExpanded = true }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.expense_category), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(selectedCategory?.name ?: stringResource(R.string.select_category), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    DropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false }
                    ) {
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Monthly Limit
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text(stringResource(R.string.monthly_limit_required, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rollover Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.rollover_balance), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.rollover_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = rollover, onCheckedChange = { rollover = it })
                }

                errorMessage?.let {
                    FormFeedbackMessage(message = it, isError = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCategoryId.isBlank()) {
                        errorMessage = selectCategoryErrorMessage
                        return@Button
                    }
                    val limit = limitStr.toDoubleOrNull()
                    if (limit == null || limit <= 0) {
                        errorMessage = validMonthlyLimitMessage
                        return@Button
                    }
                    onSaveBudget(selectedCategoryId, limit, rollover)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
