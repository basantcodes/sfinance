package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.Loan
import com.example.data.local.entities.LoanStatus
import com.example.data.local.entities.LoanType
import com.example.data.nepali.NepaliDateConverter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FormFeedbackMessage
import com.example.ui.components.TransactionTypeBadge
import com.example.ui.components.formatAmount
import com.example.ui.dialogs.AccountPicker
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun LoansScreen(
    viewModel: FinanceViewModel,
    onOpenAddLoan: () -> Unit
) {
    val loans by viewModel.loans.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val currency by viewModel.currencyState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Repaid
    val currentStatus = if (selectedTab == 0) LoanStatus.ACTIVE.name else LoanStatus.REPAID.name

    val filteredLoans = loans.filter { it.status == currentStatus }

    val activeLoans = loans.filter { it.status == LoanStatus.ACTIVE.name }
    val totalLent = activeLoans.filter { it.type == LoanType.LEND.name }.sumOf { it.principal }
    val totalBorrowed = activeLoans.filter { it.type == LoanType.BORROW.name }.sumOf { it.principal }
    val netReceivable = totalLent - totalBorrowed

    var loanToRepay by remember { mutableStateOf<Loan?>(null) }
    var repayAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var repaymentAmountStr by remember { mutableStateOf("") }
    var repaymentError by remember { mutableStateOf<String?>(null) }
    var loanToDelete by remember { mutableStateOf<Loan?>(null) }
    var loanForInterest by remember { mutableStateOf<Loan?>(null) }
    var interestAmountStr by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Track money lent & borrowed with interest",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Lent", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                                Text(formatAmount(totalLent, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Total Borrowed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9333EA))
                                Text(formatAmount(totalBorrowed, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Position", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    formatAmount(netReceivable, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netReceivable >= 0) EmeraldPrimary else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }

            // Tabs
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Active Loans (${activeLoans.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Repaid (${loans.size - activeLoans.size})") }
                    )
                }
            }

            if (filteredLoans.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No ${if (selectedTab == 0) "active" else "repaid"} loans",
                        subtitle = if (selectedTab == 0) {
                            "Record money you lend or borrow so repayment dates and balances stay visible."
                        } else {
                            "Repaid loans will appear here as your history grows."
                        },
                        actionLabel = if (selectedTab == 0) "+ Record New Loan" else null,
                        onAction = if (selectedTab == 0) onOpenAddLoan else null,
                        icon = "🤝",
                        accentColor = EmeraldPrimary
                    )
                }
            } else {
                items(filteredLoans, key = { it.id }) { loan ->
                    LoanCardItem(
                        loan = loan,
                        currency = currency,
                        onMarkRepaid = {
                            loanToRepay = loan
                            repayAccountId = accounts.firstOrNull()?.id
                            repaymentAmountStr = loan.remainingAmount.toString()
                            repaymentError = null
                        },
                        onAddInterest = {
                            loanForInterest = loan
                            interestAmountStr = ""
                        },
                        onDelete = { loanToDelete = loan }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

    // Repay Confirmation Dialog
    loanToRepay?.let { loan ->
        AlertDialog(
            onDismissRequest = { loanToRepay = null },
            title = { Text("Record Loan Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Remaining: ${formatAmount(loan.remainingAmount, currency)}\nCounterparty: ${loan.counterparty}\nType: ${loan.type}"
                    )
                    OutlinedTextField(
                        value = repaymentAmountStr,
                        onValueChange = {
                            repaymentAmountStr = it
                            repaymentError = null
                        },
                        label = { Text("Payment Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "You can pay or receive part of the balance. The loan stays active until the remaining amount reaches zero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (loan.type == LoanType.LEND.name)
                            "Select account to receive repayment into (creates Income transaction):"
                        else
                            "Select account to pay debt from (creates Expense transaction):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    AccountPicker(
                        label = "Settlement Account",
                        accounts = accounts,
                        selectedId = repayAccountId,
                        onSelect = { repayAccountId = it }
                    )
                    repaymentError?.let { FormFeedbackMessage(message = it, isError = true) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = repaymentAmountStr.toDoubleOrNull()
                        when {
                            amount == null || !amount.isFinite() || amount <= 0 -> {
                                repaymentError = "Enter a positive payment amount"
                            }
                            amount > loan.remainingAmount + 0.0001 -> {
                                repaymentError = "Payment cannot exceed the remaining balance"
                            }
                            repayAccountId == null -> {
                                repaymentError = "Select a settlement account"
                            }
                            else -> {
                                viewModel.markLoanRepaid(loan, repayAccountId, amount)
                                loanToRepay = null
                            }
                        }
                    }
                ) {
                    Text("Record Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToRepay = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Manual Interest Dialog
    loanForInterest?.let { loan ->
        AlertDialog(
            onDismissRequest = { loanForInterest = null },
            title = { Text("Add Accrued Interest") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Loan with ${loan.counterparty} (${loan.interestRate}% interest)")
                    OutlinedTextField(
                        value = interestAmountStr,
                        onValueChange = { interestAmountStr = it },
                        label = { Text("Interest Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = interestAmountStr.toDoubleOrNull()
                        if (amt != null && amt > 0) {
                            coroutineScope.launch {
                                viewModel.repository.addLoanInterest(loan.id, amt)
                                viewModel.refreshAllData()
                            }
                            loanForInterest = null
                        }
                    }
                ) {
                    Text("Add Interest")
                }
            },
            dismissButton = {
                TextButton(onClick = { loanForInterest = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Loan Dialog
    loanToDelete?.let { loan ->
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = { Text("Delete Loan") },
            text = { Text("Are you sure you want to delete this loan record for ${loan.counterparty}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLoan(loan)
                        loanToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LoanCardItem(
    loan: Loan,
    currency: String,
    onMarkRepaid: () -> Unit,
    onAddInterest: () -> Unit,
    onDelete: () -> Unit
) {
    val isLend = loan.type == LoanType.LEND.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransactionTypeBadge(type = loan.type)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = loan.counterparty,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formatAmount(loan.principal, currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLend) Color(0xFFD97706) else Color(0xFF9333EA)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Started: ${NepaliDateConverter.formatDualDate(loan.startDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (loan.interestRate > 0) {
                Text(
                    text = "Rate: ${loan.interestRate}% • Mode: ${loan.interestMode} (${loan.interestFrequency ?: "MONTHLY"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            loan.notes?.let { n ->
                if (n.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = n, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loan.status == LoanStatus.ACTIVE.name) {
                    OutlinedButton(
                        onClick = onAddInterest,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Interest", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onMarkRepaid,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Repaid", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
